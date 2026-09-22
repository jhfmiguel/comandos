package com.weaponsregistration.lifecycle.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import jakarta.persistence.EntityManager;
import com.weaponsregistration.core.model.Person;
import com.weaponsregistration.core.model.SystemUser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:lifecycle-workflow-report-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "logging.level.root=WARN", "debug=false"
})
class LifecycleWorkflowReportApiTests {
    @LocalServerPort int port;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    private final JsonMapper json = JsonMapper.builder().build();
    private String login;

    @BeforeEach
    void authenticateResponsibleOperator() throws Exception {
        login = unique();
        tx.executeWithoutResult(status -> {
            var person = new Person(); person.fullName = "Lifecycle operator"; person.personType = "INDIVIDUAL"; em.persist(person);
            var user = new SystemUser(); user.person = person; user.login = login;
            user.passwordHash = new BCryptPasswordEncoder().encode("Lifecycle-test-password"); em.persist(user);
        });
        var tokenResponse = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, tokenResponse.statusCode());
        var token = json.readTree(tokenResponse.body());
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
            .header(token.get("headerName").asText(), token.get("token").asText())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("username=" + login + "&password=Lifecycle-test-password")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, response.statusCode(), response.body());
    }
    record Scope(long organization, long unit, long asset, long lot) {
        String query() { return "organizationId=" + organization + "&unitId=" + unit; }
    }

    @Test
    void statusCatalogsValidateDuplicatesVersionsAndProtectDefaults() throws Exception {
        for (String resource : List.of("reservation-status-types", "inventory-count-status-types")) {
            String path = "inventory/" + resource;
            var defaults = ok("GET", path, null).get("content");
            assertFalse(defaults.isEmpty());
            for (var entry : defaults) {
                if (entry.get("systemProtected").asBoolean())
                    assertStatus(400, "DELETE", path + "/" + entry.get("id").asLong() + "?version=" + entry.get("version").asLong(), null);
            }
            var data = new HashMap<String, Object>(Map.of("code", "CUSTOM_" + unique().replace("-", ""),
                "name", "Custom status", "active", true, "terminal", false, "displayOrder", 50));
            var created = create(path, data);
            assertFalse(created.get("systemProtected").asBoolean());
            assertStatus(409, "POST", path, data);
            data.put("code", created.get("code").asText());
            String record = path + "/" + created.get("id").asLong();
            data.put("version", created.get("version").asLong()); data.put("name", "Updated status");
            var updated = ok("PUT", record, data);
            assertEquals("Updated status", updated.get("name").asText());
            assertStatus(409, "PUT", record, data);
            assertStatus(409, "DELETE", record + "?version=" + created.get("version").asLong(), null);
            data.put("version", updated.get("version").asLong()); data.put("name", " ");
            assertStatus(400, "PUT", record, data);
            assertEquals("Updated status", ok("GET", record, null).get("name").asText());
            assertStatus(204, "DELETE", record + "?version=" + updated.get("version").asLong(), null);
            assertStatus(404, "GET", record, null);
        }
    }

    @Test
    void workflowRequiresOrderedTransitionsAndKeepsAnImmutableEventHistory() throws Exception {
        var s = setup();
        assertStatus(400, "POST", "workflows", Map.of());
        var created = create("workflows", Map.of("organizationId", s.organization(), "unitId", s.unit(),
            "operationType", " transfer ", "resource", "inventory/assets", "recordId", s.asset(), "justification", " Transfer request "));
        String path = "workflows/" + created.get("id").asLong();
        assertEquals("REQUESTED", created.get("status").asText());
        assertEquals("TRANSFER", created.get("operationType").asText());
        assertStatus(400, "POST", path + "/execute", null);
        String previous = "REQUESTED";
        int events = 1;
        for (String action : List.of("analyze", "authorize", "execute", "conclude")) {
            String target = Map.of("analyze", "ANALYZED", "authorize", "AUTHORIZED", "execute", "EXECUTED", "conclude", "CONCLUDED").get(action);
            var moved = ok("POST", path + "/" + action, Map.of("justification", action));
            assertEquals(target, moved.get("status").asText());
            assertEquals(++events, moved.get("events").size());
            var event = moved.get("events").get(events - 1);
            assertEquals(previous, event.get("fromStatus").asText());
            assertEquals(target, event.get("toStatus").asText());
            assertStatus(400, "POST", path + "/" + action, null);
            assertEquals(events, ok("GET", path, null).get("events").size());
            previous = target;
        }
        var finished = ok("GET", path, null);
        var audit = ok("GET", "audit?resource=approval-workflows&assetId=" + s.asset() + "&unitId=" + s.unit(), null);
        assertEquals(5, audit.get("totalElements").asInt());
        var conclusion = ok("GET", "audit/" + audit.get("content").get(0).get("id").asLong(), null);
        assertEquals("EXECUTED", conclusion.get("before").get("status").asText());
        assertEquals("CONCLUDED", conclusion.get("after").get("status").asText());
        assertEquals(login, conclusion.get("event").get("actorLogin").asText());
        for (String field : List.of("authorizedAt", "executedAt", "concludedAt")) assertFalse(finished.get(field).isNull());
        assertStatus(400, "POST", path + "/cancel", null);
        assertEquals(1, ok("GET", "workflows?" + s.query() + "&status=concluded", null).get("totalElements").asInt());
        assertEquals(0, ok("GET", "workflows?" + s.query() + "&status=requested", null).get("totalElements").asInt());
        assertEquals(5, ok("GET", "armament-reports/history?resource=approval-workflows&recordId=" + created.get("id").asLong(), null).size());
        assertStatus(404, "GET", "workflows/9223372036854775807", null);
    }

    @Test
    void cancellationIsTerminalAndMismatchedUnitIsRejected() throws Exception {
        var s = setup(); var other = setup();
        var data = new HashMap<String, Object>(Map.of("organizationId", s.organization(), "unitId", other.unit(),
            "operationType", "DISPOSAL", "resource", "inventory/assets", "justification", "Review"));
        assertStatus(400, "POST", "workflows", data);
        assertEquals(0, ok("GET", "workflows?" + s.query(), null).get("totalElements").asInt());
        data.put("unitId", s.unit());
        String path = "workflows/" + create("workflows", data).get("id").asLong();
        var cancelled = ok("POST", path + "/cancel", null);
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertFalse(cancelled.get("cancelledAt").isNull());
        assertStatus(400, "POST", path + "/cancel", null);
        assertStatus(400, "POST", path + "/analyze", null);
        assertEquals(2, ok("GET", path, null).get("events").size());
    }

    @Test
    void failedInspectionBlocksAssetAndApprovalIsIdempotent() throws Exception {
        var s = setup(); var other = setup();
        var data = new HashMap<String, Object>(Map.of("organizationId", s.organization(), "unitId", s.unit(),
            "assetId", other.asset(), "checklist", "Safety checks", "result", "FAILED", "damages", "Broken component"));
        assertStatus(400, "POST", "lifecycle/inspections", data);
        assertEquals("AVAILABLE", assetStatus(other));
        data.put("assetId", s.asset());
        data.put("checklist", " ");
        assertStatus(400, "POST", "lifecycle/inspections", data);
        data.put("checklist", "Safety checks");
        var inspection = create("lifecycle/inspections", data);
        assertEquals(login, inspection.get("responsibleLogin").asText());
        assertEquals("BLOCKED", assetStatus(s));
        String path = "lifecycle/inspections/" + inspection.get("id").asLong() + "/approve";
        var approved = ok("POST", path, null);
        assertEquals(login, approved.get("approvedByLogin").asText());
        assertEquals(approved, ok("POST", path, null));
        assertEquals(1, ok("GET", "lifecycle/inspections?" + s.query(), null).get("totalElements").asInt());
        assertEquals(2, ok("GET", "armament-reports/history?resource=periodic-inspections&recordId=" + inspection.get("id").asLong(), null).size());
        assertStatus(404, "POST", "lifecycle/inspections/9223372036854775807/approve", null);
    }

    @Test
    void maintenanceCannotBeBypassedByLifecycleOperations() throws Exception {
        var s=setup();
        var recovery=create("lifecycle/occurrences",Map.of("organizationId",s.organization(),"unitId",s.unit(),"assetId",s.asset(),"type","RECOVERY","description","Located"));
        ok("POST","maintenance/orders",Map.of("requestId",unique(),"organizationId",s.organization(),"unitId",s.unit(),"assetId",s.asset(),"reason","Repair"));
        for(String result:List.of("APPROVED","FAILED","MAINTENANCE_REQUIRED"))assertStatus(409,"POST","lifecycle/inspections",Map.of("organizationId",s.organization(),"unitId",s.unit(),"assetId",s.asset(),"checklist","Inspect","result",result,"generateMaintenance",true));
        assertStatus(409,"POST","lifecycle/occurrences",Map.of("organizationId",s.organization(),"unitId",s.unit(),"assetId",s.asset(),"type","DAMAGE","description","Damage"));
        assertStatus(409,"POST","lifecycle/occurrences/"+recovery.get("id").asLong()+"/resolve",Map.of());
        assertEquals("IN_MAINTENANCE",assetStatus(s));
    }

    @Test
    void inspectionCanGenerateCorrectiveMaintenance() throws Exception {
        var s = setup();
        var inspection = create("lifecycle/inspections", Map.of("organizationId", s.organization(), "unitId", s.unit(),
            "assetId", s.asset(), "checklist", "Function check", "result", "MAINTENANCE_REQUIRED", "generateMaintenance", true));
        assertFalse(inspection.get("generatedWorkOrderId").isNull());
        assertEquals("IN_MAINTENANCE", assetStatus(s));
        assertEquals(1, ok("GET", "armament-reports/dashboard?" + s.query(), null).get("maintenanceAssets").asInt());
    }

    @Test
    void occurrencesUpdateStatusAndResolutionDoesNotDuplicateAuditOrOverwriteEvidence() throws Exception {
        var s = setup();
        assertStatus(400, "POST", "lifecycle/occurrences", Map.of("organizationId", s.organization(), "type", "LOSS", "description", "Missing"));
        var occurrence = create("lifecycle/occurrences", Map.of("organizationId", s.organization(), "unitId", s.unit(),
            "assetId", s.asset(), "type", "LOSS", "description", "Missing during count"));
        assertEquals("MISSING", assetStatus(s));
        String path = "lifecycle/occurrences/" + occurrence.get("id").asLong() + "/resolve";
        var resolved = ok("POST", path, Map.of("investigation", "Investigation completed", "documentReference", "DOC-1"));
        assertEquals("RESOLVED", resolved.get("status").asText());
        assertFalse(resolved.get("resolvedAt").isNull());
        var stored = ok("GET", "lifecycle/occurrences?" + s.query(), null).get("content").get(0);
        assertEquals("Investigation completed", stored.get("investigation").asText());
        assertEquals("DOC-1", stored.get("documentReference").asText());
        assertEquals(stored, ok("POST", path, Map.of("investigation", "Must not overwrite")));
        assertEquals("MISSING", assetStatus(s));
        assertEquals(2, ok("GET", "armament-reports/history?resource=exception-occurrences&recordId=" + occurrence.get("id").asLong(), null).size());
        var recovery = create("lifecycle/occurrences", Map.of("organizationId", s.organization(), "unitId", s.unit(),
            "assetId", s.asset(), "type", "RECOVERY", "description", "Located"));
        ok("POST", "lifecycle/occurrences/" + recovery.get("id").asLong() + "/resolve", null);
        assertEquals("AVAILABLE", assetStatus(s));
        assertEquals(2, ok("GET", "lifecycle/occurrences?" + s.query(), null).get("totalElements").asInt());
    }

    @Test
    void attachmentsRoundTripBytesAndRejectMalformedContent() throws Exception {
        var s = setup();
        var data = new HashMap<String, Object>(Map.of("resource", "inventory/assets", "recordId", s.asset(),
            "fileName", "evidence.txt", "contentType", "text/plain", "base64", "%%%"));
        assertStatus(400, "POST", "lifecycle/attachments", data);
        byte[] bytes = "Evidence: inspeção".getBytes(StandardCharsets.UTF_8);
        data.put("base64", Base64.getEncoder().encodeToString(bytes));
        var attachment = create("lifecycle/attachments", data);
        var response = client.send(HttpRequest.newBuilder(uri("lifecycle/attachments/" + attachment.get("id").asLong() + "/content")).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        assertArrayEquals(bytes, response.body());
        assertTrue(response.headers().firstValue("Content-Disposition").orElseThrow().contains("evidence.txt"));
        assertEquals(1, ok("GET", "lifecycle/attachments?resource=inventory/assets&recordId=" + s.asset(), null).size());
    }

    @Test
    void custodyReportFollowsIssueAndReturnStatus() throws Exception {
        var s = setup();
        long person = create("core/people", Map.of("personType", "INDIVIDUAL", "fullName", "Custody recipient", "active", true)).get("id").asLong();
        var custody = ok("POST", "custodies", Map.of("requestId", unique(), "organizationId", s.organization(),
            "unitId", s.unit(), "recipientId", person, "authorizerId", person, "purpose", "Service", "assetIds", List.of(s.asset())));
        assertEquals("CUSTODIED", assetStatus(s));
        String report = "armament-reports/dashboard?" + s.query();
        assertEquals(1, ok("GET", report, null).get("custodyAssets").asInt());
        assertEquals(0, ok("GET", report, null).get("availableAssets").asInt());
        ok("POST", "custodies/" + custody.get("id").asLong() + "/returns", Map.of("requestId", unique(),
            "itemIds", List.of(custody.get("items").get(0).get("id").asLong())));
        assertEquals(0, ok("GET", report, null).get("custodyAssets").asInt());
        assertEquals(1, ok("GET", report, null).get("availableAssets").asInt());
    }

    @Test
    void divergenceReportUsesApprovedCountsAndRegisteredResultCodes() throws Exception {
        for (boolean divergent : List.of(false, true)) {
            var s = setup();
            long location = ok("GET", "inventory/assets/" + s.asset(), null).get("locationId").asLong();
            var opened = ok("POST", "inventory-counts", Map.of("requestId", unique(), "organizationId", s.organization(),
                "unitId", s.unit(), "locationId", location, "purpose", "Report regression"));
            String report = "armament-reports/dashboard?" + s.query();
            assertEquals(0, ok("GET", report, null).get("inventoryDivergences").asInt());
            var lines = new ArrayList<Map<String, Object>>();
            for (var item : opened.get("items")) lines.add(Map.of("itemId", item.get("id").asLong(),
                "countedQuantity", divergent ? "0" : item.get("systemQuantity").asText()));
            String path = "inventory-counts/" + opened.get("id").asLong();
            ok("PUT", path + "/count", Map.of("items", lines));
            assertEquals(0, ok("GET", report, null).get("inventoryDivergences").asInt());
            ok("POST", path + "/approve", null);
            assertEquals(divergent ? 1 : 0, ok("GET", report, null).get("inventoryDivergences").asInt());
            boolean alert = false;
            for (var entry : ok("GET", "armament-reports/alerts?" + s.query(), null))
                if ("INVENTORY_DIVERGENCE".equals(entry.get("type").asText())) alert = true;
            assertEquals(divergent, alert);
        }
    }

    @Test
    void reportsReflectOperationsAndFilterOrganizationUnitAndSearch() throws Exception {
        var s = setup(); var other = setup();
        create("lifecycle/occurrences", Map.of("organizationId", s.organization(), "unitId", s.unit(),
            "assetId", s.asset(), "type", "DAMAGE", "description", "Damage report"));
        create("workflows", Map.of("organizationId", s.organization(), "unitId", s.unit(),
            "operationType", "MAINTENANCE", "resource", "inventory/assets", "justification", "Repair"));
        var dashboard = ok("GET", "armament-reports/dashboard?" + s.query(), null);
        assertEquals(1, dashboard.get("totalAssets").asInt());
        assertEquals(0, dashboard.get("availableAssets").asInt());
        assertEquals(1, dashboard.get("blockedAssets").asInt());
        assertEquals(1, dashboard.get("openOccurrences").asInt());
        assertEquals(1, dashboard.get("pendingWorkflows").asInt());
        assertEquals(10, dashboard.get("availableStock").asInt());
        var alerts = ok("GET", "armament-reports/alerts?" + s.query(), null);
        Set<String> types = new HashSet<>(); alerts.forEach(a -> types.add(a.get("type").asText()));
        assertEquals(Set.of("OPEN_OCCURRENCE", "PENDING_APPROVAL"), types);
        var assets = ok("GET", "armament-reports/assets?" + s.query(), null);
        assertEquals(1, assets.size()); assertEquals(s.asset(), assets.get(0).get("assetId").asLong());
        assertEquals(assets, ok("GET", "armament-reports/assets?" + s.query() + "&q=" + assets.get(0).get("assetCode").asText(), null));
        assertEquals(0, ok("GET", "armament-reports/assets?" + s.query() + "&q=no-match", null).size());
        var lots = ok("GET", "armament-reports/lots?" + s.query(), null);
        assertEquals(1, lots.size()); assertEquals(s.lot(), lots.get(0).get("lotId").asLong());
        assertEquals(10, lots.get(0).get("available").asInt());
        var bundle = ok("GET", "armament-reports/bundle?" + s.query(), null);
        assertEquals(dashboard, bundle.get("dashboard")); assertEquals(assets, bundle.get("assets")); assertEquals(lots, bundle.get("lots"));
        Set<Long> scopedEvents = new HashSet<>();
        ok("GET", "audit?" + s.query(), null).get("content").forEach(event -> scopedEvents.add(event.get("id").asLong()));
        assertFalse(bundle.get("recentHistory").isEmpty());
        for (var event : bundle.get("recentHistory")) assertTrue(scopedEvents.contains(event.get("id").asLong()));
        assertEquals(0, ok("GET", "armament-reports/bundle?organizationId=" + s.organization()
            + "&unitId=" + other.unit(), null).get("recentHistory").size());
        assertEquals(1, ok("GET", "armament-reports/dashboard?" + other.query(), null).get("availableAssets").asInt());
        assertEquals(0, ok("GET", "armament-reports/dashboard?organizationId=" + s.organization() + "&unitId=" + other.unit(), null).get("totalAssets").asInt());
    }

    private String assetStatus(Scope s) throws Exception { return ok("GET", "inventory/assets/" + s.asset(), null).get("status").asText(); }
    private Scope setup() throws Exception {
        long org = create("core/organizations", Map.of("name", unique(), "nature", "Public safety", "publicOrganization", true, "active", true)).get("id").asLong();
        long unit = create("core/units", Map.of("organizationId", org, "code", unique(), "name", "Unit", "type", "Unit")).get("id").asLong();
        long location = create("inventory/locations", Map.of("organizationId", org, "unitId", unit, "name", "Vault", "type", "Controlled", "controlled", true)).get("id").asLong();
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Maker")).get("id").asLong();
        long asset = 0, lot = 0;
        for (boolean serialized : List.of(true, false)) {
            long category = create("inventory/categories", Map.of("name", unique(), "family", serialized ? "OPTICAL" : "AMMUNITION", "serialized", serialized, "lotControlled", !serialized, "consumable", !serialized)).get("id").asLong();
            long model = create("inventory/models", Map.of("categoryId", category, "brandId", brand, "name", "Model", "unitOfMeasure", "EA", "sku", unique(), "listPrice", "1")).get("id").asLong();
            if (serialized) asset = create("inventory/assets", Map.of("modelId", model, "locationId", location, "assetCode", unique(), "serialNumber", unique(), "condition", "GOOD", "status", "AVAILABLE", "currentValue", "1")).get("id").asLong();
            else lot = create("inventory/lots", Map.of("modelId", model, "openingLocationId", location, "lotNumber", unique(), "initialQuantity", "10")).get("id").asLong();
        }
        return new Scope(org, unit, asset, lot);
    }
    private String unique() { return UUID.randomUUID().toString(); }
    private URI uri(String path) { return URI.create("http://localhost:" + port + "/api/erp/" + path); }
    private JsonNode create(String path, Object data) throws Exception { return assertStatus(201, "POST", path, data); }
    private JsonNode ok(String method, String path, Object data) throws Exception { return assertStatus(200, method, path, data); }
    private JsonNode assertStatus(int expected, String method, String path, Object data) throws Exception {
        var request = HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
            .method(method, data == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data))).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(expected, response.statusCode(), method + " " + path + ": " + response.body());
        return response.body().isBlank() ? null : json.readTree(response.body());
    }
}
