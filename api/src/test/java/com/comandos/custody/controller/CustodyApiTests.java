package com.comandos.custody.controller;

import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:custody-tests;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "logging.level.root=WARN", "debug=false"
})
class CustodyApiTests {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    record Result(int status, JsonNode body, String raw) {}
    record Setup(long organization, long unit, long recipient, long authorizer, long location, long first, long second) {}

    private Result request(String method, String path, Object body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/erp/" + path))
            .header("Content-Type", "application/json").method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new Result(response.statusCode(), response.body().isBlank() ? null : json.readTree(response.body()), response.body());
    }
    private long create(String path, Map<String, Object> body) throws Exception {
        var result = request("POST", path, body); assertEquals(201, result.status(), result.raw()); return result.body().get("id").asLong();
    }
    private String unique() { return UUID.randomUUID().toString(); }
    private Setup setup() throws Exception {
        long org = create("core/organizations", Map.of("name", unique(), "nature", "Public safety", "publicOrganization", true, "active", true));
        long unit = create("core/units", Map.of("organizationId", org, "code", unique(), "name", "Operational unit", "type", "Unit"));
        long recipient = create("core/people", Map.of("personType", "INDIVIDUAL", "fullName", "Recipient", "active", true));
        long authorizer = create("core/people", Map.of("personType", "INDIVIDUAL", "fullName", "Authorizer", "active", true));
        long category = create("inventory/categories", Map.of("name", unique(), "family", "OPTICAL", "serialized", true,
            "lotControlled", false, "consumable", false));
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Manufacturer"));
        long model = create("inventory/models", Map.of("categoryId", category, "brandId", brand, "name", "Service pistol",
            "unitOfMeasure", "EA", "sku", unique(), "listPrice", "1000"));
        long location = create("inventory/locations", Map.of("organizationId", org, "unitId", unit, "name", "Armory",
            "type", "Controlled", "controlled", true));
        long first = asset(model, location); long second = asset(model, location);
        return new Setup(org, unit, recipient, authorizer, location, first, second);
    }
    private long asset(long model, long location) throws Exception {
        return create("inventory/assets", Map.of("modelId", model, "locationId", location, "assetCode", unique(),
            "serialNumber", unique(), "condition", "GOOD", "status", "AVAILABLE", "currentValue", "1000"));
    }
    private Map<String, Object> issue(Setup s, List<Long> assets) {
        return new HashMap<>(Map.of("requestId", unique(), "organizationId", s.organization(), "unitId", s.unit(),
            "recipientId", s.recipient(), "authorizerId", s.authorizer(), "purpose", "Service duty", "assetIds", assets));
    }

    @Test
    void armamentQueryCombinesFiltersPagesAndLoadsOnlySelectedAssetHistory() throws Exception {
        var s = setup();
        var first = request("GET", "inventory/assets/" + s.first(), null).body();
        String filters = "&filter.modelId=Service&filter.status=AVAILABLE&filter.unit=Operational&filter.locationId=Armory";
        String path = "inventory/assets?organizationId=" + s.organization() + filters;
        var page = request("GET", path + "&size=1&page=1", null);
        assertEquals(200, page.status(), page.raw());
        assertEquals(2, page.body().get("totalElements").asInt());
        assertEquals(1, page.body().get("content").size());
        assertEquals(s.second(), page.body().get("content").get(0).get("id").asLong());
        assertEquals(s.unit(), first.get("unitId").asLong());
        assertEquals(s.organization(), first.get("organizationId").asLong());
        for (String field : List.of("assetCode", "serialNumber")) {
            var found = request("GET", path + "&filter." + field + "=" + first.get(field).asText(), null);
            assertEquals(1, found.body().get("totalElements").asInt());
            assertEquals(s.first(), found.body().get("content").get(0).get("id").asLong());
        }
        assertEquals(0, request("GET", path + "&filter.assetCode=nonexistent", null).body().get("totalElements").asInt());
        assertEquals(0, request("GET", "inventory/assets?filter.unit=" + unique(), null).body().get("totalElements").asInt());
        assertEquals(400, request("GET", path + "&page=-1", null).status());
        var issued = request("POST", "custodies", issue(s, List.of(s.first())));
        assertEquals(200, issued.status(), issued.raw());
        var custodies = request("GET", "custodies/by-asset/" + s.first(), null);
        assertEquals(200, custodies.status(), custodies.raw());
        assertEquals(1, custodies.body().get("totalElements").asInt());
        assertEquals(issued.body().get("id").asLong(), custodies.body().get("content").get(0).get("id").asLong());
        assertEquals(0, request("GET", "custodies/by-asset/" + s.second(), null).body().get("totalElements").asInt());
        assertEquals(0, request("GET", "custodies/by-asset/" + s.first() + "?page=1", null).body().get("content").size());
        assertEquals(400, request("GET", "custodies/by-asset/" + s.first() + "?page=-1", null).status());
        var movements = request("GET", "inventory/movements?assetId=" + s.first() + "&size=1", null);
        assertEquals(200, movements.status(), movements.raw());
        assertEquals(2, movements.body().get("totalElements").asInt());
        assertEquals(s.first(), movements.body().get("content").get(0).get("assetId").asLong());
    }

    @Test
    void organizationalUnitReceivesEquipmentAndReturnsItWithRecipientSnapshot() throws Exception {
        var s = setup();
        long receivingUnit = create("core/units", Map.of("organizationId", s.organization(), "code", unique(),
            "name", "Receiving battalion", "type", "Unit"));
        var data = issue(s, List.of(s.first(), s.second()));
        data.remove("recipientId"); data.put("recipientUnitId", receivingUnit);
        var issued = request("POST", "custodies", data);
        assertEquals(200, issued.status(), issued.raw());
        long id = issued.body().get("id").asLong();
        assertTrue(issued.body().get("recipientId").isNull());
        assertEquals("UNIT", issued.body().get("recipientType").asText());
        assertEquals(receivingUnit, issued.body().get("recipientUnitId").asLong());
        assertEquals(s.unit(), issued.body().get("unitId").asLong());
        assertEquals(id, request("POST", "custodies", data).body().get("id").asLong());
        data.put("recipientUnitId", s.unit());
        assertEquals(409, request("POST", "custodies", data).status());
        jdbc.update("update erp_organizational_unit set name = 'Renamed battalion' where id = ?", receivingUnit);
        var detail = request("GET", "custodies/" + id, null);
        assertEquals("Receiving battalion", detail.body().get("recipientName").asText());
        assertEquals("UNIT", request("GET", "custodies?organizationId=" + s.organization(), null)
            .body().get("content").get(0).get("recipientType").asText());
        long first = issued.body().get("items").get(0).get("id").asLong();
        long second = issued.body().get("items").get(1).get("id").asLong();
        var partial = request("POST", "custodies/" + id + "/returns", Map.of("requestId", unique(), "itemIds", List.of(first)));
        assertEquals(200, partial.status(), partial.raw());
        assertEquals("PARTIALLY_RETURNED", partial.body().get("status").asText());
        var returned = request("POST", "custodies/" + id + "/returns", Map.of("requestId", unique(), "itemIds", List.of(second)));
        assertEquals(200, returned.status(), returned.raw());
        assertEquals("RETURNED", returned.body().get("status").asText());
        assertEquals(receivingUnit, returned.body().get("recipientUnitId").asLong());
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.first()));
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.second()));
        var other = setup();
        var move = request("PUT", "core/units/" + receivingUnit, Map.of("organizationId", other.organization(),
            "code", unique(), "name", "Moved battalion", "type", "Unit", "version", 0));
        assertEquals(400, move.status(), move.raw());
    }

    @Test
    void recipientMustBeExactlyOnePersonOrUnitInTheOrganization() throws Exception {
        var s = setup(); var other = setup();
        var data = issue(s, List.of(s.first()));
        data.put("recipientUnitId", s.unit());
        assertEquals(400, request("POST", "custodies", data).status());
        data.remove("recipientId"); data.remove("recipientUnitId");
        assertEquals(400, request("POST", "custodies", data).status());
        for (long invalid : List.of(other.unit(), -1L, Long.MAX_VALUE)) {
            data.put("recipientUnitId", invalid);
            assertEquals(400, request("POST", "custodies", data).status());
        }
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_custody where request_id = ?", Long.class, data.get("requestId")));
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.first()));
    }

    @Test
    void issueAndPartialReturnsMoveFirearmsAndRemainIdempotent() throws Exception {
        var s = setup(); var data = issue(s, List.of(s.first(), s.second()));
        var issued = request("POST", "custodies", data); assertEquals(200, issued.status(), issued.raw());
        long custody = issued.body().get("id").asLong();
        assertEquals("ACTIVE", issued.body().get("status").asText());
        assertEquals("PERSON", issued.body().get("recipientType").asText());
        assertEquals(s.recipient(), issued.body().get("recipientId").asLong());
        assertTrue(issued.body().get("recipientUnitId").isNull());
        assertEquals(2, issued.body().get("items").size());
        assertEquals(2L, jdbc.queryForObject("select count(*) from erp_stock_movement where nature = 'CUSTODY_ISSUE' and location_id = ?", Long.class, s.location()));
        assertEquals("CUSTODIED", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.first()));
        var retry = request("POST", "custodies", data); assertEquals(custody, retry.body().get("id").asLong());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource_name_name = 'custodies' and record_id = ? and action = 'ISSUE'", Long.class, custody));
        long firstItem = issued.body().get("items").get(0).get("id").asLong();
        long secondItem = issued.body().get("items").get(1).get("id").asLong();
        var firstReturn = Map.of("requestId", unique(), "itemIds", List.of(firstItem));
        var partial = request("POST", "custodies/" + custody + "/returns", firstReturn);
        assertEquals(200, partial.status(), partial.raw()); assertEquals("PARTIALLY_RETURNED", partial.body().get("status").asText());
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.first()));
        assertEquals("CUSTODIED", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.second()));
        assertEquals(200, request("POST", "custodies/" + custody + "/returns", firstReturn).status());
        var completed = request("POST", "custodies/" + custody + "/returns", Map.of("requestId", unique(), "itemIds", List.of(secondItem)));
        assertEquals("RETURNED", completed.body().get("status").asText()); assertFalse(completed.body().get("completedAt").isNull());
        assertEquals(2, completed.body().get("returns").size());
        assertEquals(2L, jdbc.queryForObject("select count(*) from erp_stock_movement where nature = 'CUSTODY_RETURN' and location_id = ?", Long.class, s.location()));
        assertEquals(2L, jdbc.queryForObject("select count(*) from erp_audit_record where resource_name_name = 'custodies' and record_id = ? and action = 'RETURN'", Long.class, custody));
        com.comandos.audit.controller.AuditTraceAssertions.trace(port,"custodies",custody,"assetId="+s.first()+"&unitId="+s.unit(),"ISSUE","RETURN","RETURN");
    }

    @Test
    void invalidScopeOrUnavailableSecondAssetRollsBackTheWholeIssue() throws Exception {
        var s = setup(); var other = setup();
        var data = issue(s, List.of(s.first(), other.first()));
        assertEquals(400, request("POST", "custodies", data).status());
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.first()));
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_custody where request_id = ?", Long.class, data.get("requestId")));
        jdbc.update("update erp_asset_item set status = 'BLOCKED' where id = ?", s.second());
        data = issue(s, List.of(s.first(), s.second()));
        assertEquals(409, request("POST", "custodies", data).status());
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.first()));
    }

    @Test
    void returnValidatesMembershipAndCannotReturnAnItemTwice() throws Exception {
        var s = setup(); var other = setup();
        var first = request("POST", "custodies", issue(s, List.of(s.first()))).body();
        var second = request("POST", "custodies", issue(other, List.of(other.first()))).body();
        long firstId = first.get("id").asLong(); long firstItem = first.get("items").get(0).get("id").asLong();
        long otherItem = second.get("items").get(0).get("id").asLong();
        assertEquals(400, request("POST", "custodies/" + firstId + "/returns", Map.of("requestId", unique(), "itemIds", List.of(otherItem))).status());
        assertEquals(200, request("POST", "custodies/" + firstId + "/returns", Map.of("requestId", unique(), "itemIds", List.of(firstItem))).status());
        assertEquals(409, request("POST", "custodies/" + firstId + "/returns", Map.of("requestId", unique(), "itemIds", List.of(firstItem))).status());
    }

    @Test
    void stockAndHistoryHonorUnitAndOnlyExposeAvailableFirearms() throws Exception {
        var s = setup();
        assertEquals(2, request("GET", "custodies/stock?organizationId=" + s.organization() + "&unitId=" + s.unit(), null).body().get("totalElements").asInt());
        var issued = request("POST", "custodies", issue(s, List.of(s.first())));
        assertEquals(1, request("GET", "custodies/stock?organizationId=" + s.organization() + "&unitId=" + s.unit(), null).body().get("totalElements").asInt());
        assertEquals(1, request("GET", "custodies?organizationId=" + s.organization() + "&unitId=" + s.unit(), null).body().get("totalElements").asInt());
        assertEquals(issued.body().get("id").asLong(), request("GET", "custodies/" + issued.body().get("id").asLong(), null).body().get("id").asLong());
    }

    @Test
    void competingIssuesCannotIssueTheSameFirearmTwice() throws Exception {
        var s = setup(); var a = issue(s, List.of(s.first())); var b = issue(s, List.of(s.first()));
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Result>> futures = new ArrayList<>();
            for (var payload : List.of(a, b)) futures.add(executor.submit(() -> { ready.countDown(); start.await(); return request("POST", "custodies", payload); }));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            var statuses = List.of(futures.get(0).get(30, TimeUnit.SECONDS).status(), futures.get(1).get(30, TimeUnit.SECONDS).status()).stream().sorted().toList();
            assertEquals(List.of(200, 409), statuses);
        }
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_custody_item where asset_id = ?", Long.class, s.first()));
    }

    @Test
    void issuesAndReturnsCompleteEquipmentSetWithAssetAndLotQuantity() throws Exception {
        var s = setup();
        long category = create("inventory/categories", Map.of("name", unique(), "family", "AMMUNITION", "serialized", false,
            "lotControlled", true, "consumable", true));
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Manufacturer"));
        long model = create("inventory/models", Map.of("categoryId", category, "brandId", brand, "name", "Cartridge",
            "unitOfMeasure", "EA", "sku", unique(), "listPrice", "1"));
        long lot = create("inventory/lots", Map.of("modelId", model, "openingLocationId", s.location(),
            "lotNumber", unique(), "initialQuantity", "20"));
        long balance = jdbc.queryForObject("select id from erp_stock_balance where lot_id = ?", Long.class, lot);
        String code = unique();
        long set = create("inventory/equipment-sets", Map.of("organizationId", s.organization(), "unitId", s.unit(),
            "code", code, "name", "Patrol kit", "active", false));
        create("inventory/equipment-set-components", Map.of("equipmentSetId", set, "assetId", s.first(),
            "role", "Primary equipment", "quantity", "1"));
        create("inventory/equipment-set-components", Map.of("equipmentSetId", set, "balanceId", balance,
            "role", "Ammunition", "quantity", "5"));
        var activation = request("PUT", "inventory/equipment-sets/" + set, Map.of("organizationId", s.organization(),
            "unitId", s.unit(), "code", code, "name", "Patrol kit", "active", true, "version", 0));
        assertEquals(200, activation.status(), activation.raw());
        assertEquals(1, request("GET", "custodies/stock?organizationId=" + s.organization() + "&unitId=" + s.unit(), null)
            .body().get("totalElements").asInt());
        assertEquals(1, request("GET", "custodies/equipment-sets?organizationId=" + s.organization() + "&unitId=" + s.unit(), null)
            .body().get("totalElements").asInt());

        var issued = request("POST", "custodies", Map.of("requestId", unique(), "organizationId", s.organization(),
            "unitId", s.unit(), "recipientId", s.recipient(), "authorizerId", s.authorizer(), "purpose", "Patrol",
            "assetIds", List.of(), "equipmentSetIds", List.of(set)));
        assertEquals(200, issued.status(), issued.raw()); assertEquals(2, issued.body().get("items").size());
        assertEquals("CUSTODIED", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.first()));
        assertEquals(0, jdbc.queryForObject("select available from erp_stock_balance where id = ?", java.math.BigDecimal.class, balance)
            .compareTo(new java.math.BigDecimal("15")));
        long custody = issued.body().get("id").asLong();
        long firstItem = issued.body().get("items").get(0).get("id").asLong();
        var itemIds = List.of(firstItem, issued.body().get("items").get(1).get("id").asLong());
        assertEquals(400, request("POST", "custodies/" + custody + "/returns",
            Map.of("requestId", unique(), "itemIds", List.of(firstItem))).status());
        long damaged = jdbc.queryForObject("select id from erp_custody_return_condition_type where code = 'DAMAGED'", Long.class);
        var returned = request("POST", "custodies/" + custody + "/returns", Map.of("requestId", unique(), "itemIds", itemIds,
            "conditionTypeId", damaged, "inspectionNotes", "Damage found during return inspection"));
        assertEquals(200, returned.status(), returned.raw()); assertEquals("RETURNED", returned.body().get("status").asText());
        assertEquals("BLOCKED", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.first()));
        assertEquals(0, jdbc.queryForObject("select available from erp_stock_balance where id = ?", java.math.BigDecimal.class, balance)
            .compareTo(new java.math.BigDecimal("15")));
        assertEquals(0, jdbc.queryForObject("select blocked from erp_stock_balance where id = ?", java.math.BigDecimal.class, balance)
            .compareTo(new java.math.BigDecimal("5")));
        assertEquals("Damaged", returned.body().get("items").get(0).get("returnConditionName").asText());
        assertEquals("Damage found during return inspection", returned.body().get("items").get(0).get("inspectionNotes").asText());
        long inspection = returned.body().get("items").get(0).get("returnInspectionId").asLong();
        var maintenance = request("POST", "maintenance/orders", Map.of("requestId", unique(),
            "organizationId", s.organization(), "unitId", s.unit(), "assetId", s.first(),
            "reason", "Damage found during return inspection", "custodyReturnItemId", inspection));
        assertEquals(200, maintenance.status(), maintenance.raw());
        assertEquals(inspection, maintenance.body().get("custodyReturnItemId").asLong());
        assertEquals("IN_MAINTENANCE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.first()));
        assertEquals(409, request("POST", "maintenance/orders", Map.of("requestId", unique(),
            "organizationId", s.organization(), "unitId", s.unit(), "assetId", s.first(),
            "reason", "Duplicate order", "custodyReturnItemId", inspection)).status());
        var history = request("GET", "custodies?organizationId=" + s.organization() + "&unitId=" + s.unit(), null);
        assertEquals(maintenance.body().get("id").asLong(), history.body().get("content").get(0).get("items").get(0)
            .get("maintenanceWorkOrderId").asLong());
    }
}
