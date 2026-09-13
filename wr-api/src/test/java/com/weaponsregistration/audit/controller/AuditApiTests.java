package com.weaponsregistration.audit.controller;

import com.weaponsregistration.core.service.CoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:audit-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "logging.level.root=WARN", "debug=false"
})
class AuditApiTests {
    @LocalServerPort int port;
    @Autowired CoreService core;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    record Result(int status, JsonNode body, String raw) {}

    private Result request(String method, String path, Object data) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (!method.equals("GET")) {
            var token = request("GET", "/api/auth/csrf", null).body();
            builder.header(token.get("headerName").asText(), token.get("token").asText());
        }
        boolean form = data instanceof String;
        builder.header("Content-Type", form ? "application/x-www-form-urlencoded" : "application/json")
            .method(method, data == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(form ? (String) data : json.writeValueAsString(data)));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Result(response.statusCode(), response.body().isBlank() ? null : json.readTree(response.body()), response.body());
    }
    private Map<String, Object> person(String name) { return Map.of("personType", "INDIVIDUAL", "fullName", name, "active", true); }
    private JsonNode history(String resource, long id) throws Exception {
        var result = request("GET", "/api/erp/audit?resource=" + resource + "&recordId=" + id, null);
        assertEquals(200, result.status(), result.raw()); return result.body();
    }
    private JsonNode detail(JsonNode event) throws Exception { return request("GET", "/api/erp/audit/" + event.get("id").asLong(), null).body(); }

    @Test
    void committedCreateUpdateDeletePreserveSnapshotsAndFailedWritesLeaveNoEvents() throws Exception {
        var data = new HashMap<String, Object>(person("Original name"));
        var created = request("POST", "/api/erp/core/people", data); assertEquals(201, created.status(), created.raw());
        long id = created.body().get("id").asLong();
        data.put("version", 99); data.put("fullName", "Rejected name");
        assertEquals(409, request("PUT", "/api/erp/core/people/" + id, data).status());
        assertEquals(1, history("core/people", id).get("totalElements").asInt());
        data.put("version", 0); data.put("fullName", "Updated name");
        var updated = request("PUT", "/api/erp/core/people/" + id, data); assertEquals(200, updated.status(), updated.raw());
        assertEquals(204, request("DELETE", "/api/erp/core/people/" + id + "?version=" + updated.body().get("version").asLong(), null).status());
        var events = history("core/people", id).get("content"); assertEquals(3, events.size());
        assertEquals("DELETE", events.get(0).get("action").asText());
        assertEquals("Updated name", detail(events.get(0)).get("before").get("fullName").asText());
        assertTrue(detail(events.get(0)).get("after").isNull());
        assertEquals("Original name", detail(events.get(1)).get("before").get("fullName").asText());
        assertEquals("Updated name", detail(events.get(1)).get("after").get("fullName").asText());
        assertEquals("UNAUTHENTICATED", events.get(2).get("actorType").asText());
        assertTrue(events.get(2).get("actorId").isNull());
    }

    @Test
    void authenticatedActorIsServerDerivedAndPasswordsNeverEnterSnapshots() throws Exception {
        long person = (Long) core.save("people", null, person("Account person")).get("id");
        String login = UUID.randomUUID().toString(); String password = "Private-password-12345";
        var user = core.save("users", null, Map.of("personId", person, "login", login, "password", password, "blocked", false));
        long userId = (Long) user.get("id");
        var accountEvent = detail(history("core/users", userId).get("content").get(0));
        assertFalse(accountEvent.toString().contains(password));
        assertFalse(accountEvent.toString().contains("passwordHash"));
        assertTrue(accountEvent.get("after").get("passwordChanged").asBoolean());
        assertEquals(204, request("POST", "/api/auth/login", "username=" + login + "&password=" + password).status());
        var created = request("POST", "/api/erp/core/people", person("Audited person"));
        assertEquals(201, created.status(), created.raw());
        long id = created.body().get("id").asLong();
        var event = history("core/people", id).get("content").get(0);
        assertEquals(userId, event.get("actorId").asLong());
        assertEquals(login, event.get("actorLogin").asText());
        assertEquals("ACCOUNT", event.get("actorType").asText());
        jdbc.update("update erp_system_user set login = ? where id = ?", "changed-" + login, userId);
        jdbc.update("delete from erp_system_user where id = ?", userId);
        assertEquals(login, jdbc.queryForObject("select actor_login from erp_audit_record where id = ?", String.class, event.get("id").asLong()));
    }

    @Test
    void auditAndBusinessDataRollbackTogether() {
        var id = new AtomicLong();
        assertThrows(IllegalStateException.class, () -> tx.execute(status -> {
            id.set((Long) core.save("people", null, person("Rolled back")).get("id"));
            throw new IllegalStateException("Simulated failure after the business operation.");
        }));
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_person where id = ?", Long.class, id.get()));
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'core/people' and record_id = ?", Long.class, id.get()));
    }

    @Test
    void auditApiIsReadOnlyAndFiltersValidateTimeRanges() throws Exception {
        long id = (Long) core.save("people", null, person("Filter example")).get("id");
        var events = history("core/people", id);
        long eventId = events.get("content").get(0).get("id").asLong();
        assertEquals(405, request("POST", "/api/erp/audit", Map.of()).status());
        assertEquals(405, request("PUT", "/api/erp/audit/" + eventId, Map.of()).status());
        assertEquals(405, request("DELETE", "/api/erp/audit/" + eventId, null).status());
        assertEquals(400, request("GET", "/api/erp/audit?page=-1", null).status());
        assertEquals(400, request("GET", "/api/erp/audit?from=2030-01-01T00:00:00Z&until=2020-01-01T00:00:00Z", null).status());
        assertEquals(0, request("GET", "/api/erp/audit?resource=core/people&recordId=" + id + "&action=DELETE", null).body().get("totalElements").asInt());
        assertEquals(1, history("core/people", id).get("totalElements").asInt());
    }

    @Test
    void inventoryRegistrationsCaptureTheirChanges() throws Exception {
        var data = new HashMap<String, Object>(Map.of("name", UUID.randomUUID().toString(), "manufacturer", "Original factory"));
        var created = request("POST", "/api/erp/inventory/brands", data); assertEquals(201, created.status(), created.raw());
        long id = created.body().get("id").asLong();
        data.put("version", 0); data.put("manufacturer", "Updated factory");
        var updated = request("PUT", "/api/erp/inventory/brands/" + id, data); assertEquals(200, updated.status(), updated.raw());
        assertEquals(204, request("DELETE", "/api/erp/inventory/brands/" + id + "?version=" + updated.body().get("version").asLong(), null).status());
        var events = history("inventory/brands", id).get("content");
        assertEquals(3, events.size());
        var update = detail(events.get(1));
        assertEquals("Original factory", update.get("before").get("manufacturer").asText());
        assertEquals("Updated factory", update.get("after").get("manufacturer").asText());
    }
}
