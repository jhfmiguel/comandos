package com.comandos.consumption.controller;

import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:ammunition-consumption-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "logging.level.root=WARN"
})
class AmmunitionConsumptionApiTests {
    @LocalServerPort int port; @Autowired JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient(); private final JsonMapper json = JsonMapper.builder().build();
    record Result(int status, JsonNode body, String raw) {}
    record Setup(long organization, long unit, long responsible, long authorizer, long location, long lot, long balance) {}
    private Result request(String method, String path, Object body) throws Exception { var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/erp/" + path))
        .header("Content-Type", "application/json").method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString()); return new Result(response.statusCode(), response.body().isBlank() ? null : json.readTree(response.body()), response.body()); }
    private long create(String path, Map<String, Object> body) throws Exception { var result = request("POST", path, body); assertEquals(201, result.status(), result.raw()); return result.body().get("id").asLong(); }
    private String unique() { return UUID.randomUUID().toString(); }
    private Setup setup() throws Exception {
        long org = create("core/organizations", Map.of("name", unique(), "nature", "Public safety", "publicOrganization", true, "active", true));
        long unit = create("core/units", Map.of("organizationId", org, "code", unique(), "name", "Range", "type", "Unit"));
        long responsible = create("core/people", Map.of("personType", "INDIVIDUAL", "fullName", "Responsible", "active", true));
        long authorizer = create("core/people", Map.of("personType", "INDIVIDUAL", "fullName", "Authorizer", "active", true));
        long category = create("inventory/categories", Map.of("name", unique(), "family", "AMMUNITION", "serialized", false, "lotControlled", true, "consumable", true));
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Manufacturer"));
        long model = create("inventory/models", Map.of("categoryId", category, "brandId", brand, "name", "9 mm cartridge", "unitOfMeasure", "EA", "sku", unique(), "listPrice", "2"));
        long location = create("inventory/locations", Map.of("organizationId", org, "unitId", unit, "name", "Ammunition store", "type", "Controlled", "controlled", true));
        long lot = create("inventory/lots", Map.of("modelId", model, "openingLocationId", location, "lotNumber", unique(), "initialQuantity", "10.5000"));
        long balance = jdbc.queryForObject("select id from erp_stock_balance where lot_id = ?", Long.class, lot);
        return new Setup(org, unit, responsible, authorizer, location, lot, balance);
    }
    private Map<String, Object> payload(Setup s, String quantity) { return Map.of("requestId", unique(), "organizationId", s.organization(), "unitId", s.unit(),
        "responsibleId", s.responsible(), "authorizerId", s.authorizer(), "purpose", "Qualification training",
        "items", List.of(Map.of("balanceId", s.balance(), "quantity", quantity, "result", "Consumed during training"))); }

    @Test void finalizationDeductsExactStockAndIsIdempotent() throws Exception {
        var s = setup(); var data = payload(s, "2.2500"); var first = request("POST", "ammunition-consumptions", data);
        assertEquals(200, first.status(), first.raw()); assertEquals("FINALIZED", first.body().get("status").asText());
        com.comandos.audit.controller.AuditTraceAssertions.trace(port,"ammunition-consumptions",first.body().get("id").asLong(),"lotId="+s.lot()+"&unitId="+s.unit(),"FINALIZE");
        assertEquals("8.2500", jdbc.queryForObject("select cast(available as varchar) from erp_stock_balance where id = ?", String.class, s.balance()));
        assertEquals("8.2500", jdbc.queryForObject("select cast(available_quantity as varchar) from erp_stock_lot where id = ?", String.class, s.lot()));
        assertEquals(first.body().get("id").asLong(), request("POST", "ammunition-consumptions", data).body().get("id").asLong());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_stock_movement where nature = 'CONSUMPTION_DEFLAGRATION' and lot_id = ?", Long.class, s.lot()));
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'ammunition-consumptions' and record_id = ?", Long.class, first.body().get("id").asLong()));
    }
    @Test void insufficientSecondLineRollsBackEveryDeduction() throws Exception {
        var s = setup(); var data = new HashMap<String, Object>(payload(s, "1"));
        long model = jdbc.queryForObject("select model_id from erp_stock_lot where id = ?", Long.class, s.lot());
        long otherLot = create("inventory/lots", Map.of("modelId", model, "openingLocationId", s.location(), "lotNumber", unique(), "initialQuantity", "10"));
        long otherBalance = jdbc.queryForObject("select id from erp_stock_balance where lot_id = ?", Long.class, otherLot);
        data.put("items", List.of(Map.of("balanceId", s.balance(), "quantity", "1", "result", "Used"), Map.of("balanceId", otherBalance, "quantity", "99", "result", "Used")));
        assertEquals(409, request("POST", "ammunition-consumptions", data).status());
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_ammunition_consumption where request_id = ?", Long.class, data.get("requestId")));
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_stock_movement where nature = 'CONSUMPTION_DEFLAGRATION' and lot_id = ?", Long.class, s.lot()));
        assertEquals("10.5000", jdbc.queryForObject("select cast(available as varchar) from erp_stock_balance where id = ?", String.class, s.balance()));
        assertEquals("10.5000", jdbc.queryForObject("select cast(available_quantity as varchar) from erp_stock_lot where id = ?", String.class, s.lot()));
    }
    @Test void changedRetryConflictsAndExpiredAmmunitionIsExcluded() throws Exception {
        var s = setup(); var data = new HashMap<String, Object>(payload(s, "1"));
        var first = request("POST", "ammunition-consumptions", data); assertEquals(200, first.status(), first.raw());
        data.put("purpose", "Different purpose");
        assertEquals(409, request("POST", "ammunition-consumptions", data).status());
        jdbc.update("update erp_stock_lot set valid_until = ? where id = ?", java.time.LocalDate.now().minusDays(1), s.lot());
        assertEquals(0, request("GET", "ammunition-consumptions/stock?organizationId=" + s.organization() + "&unitId=" + s.unit(), null).body().get("totalElements").asInt());
        assertEquals(400, request("POST", "ammunition-consumptions", payload(s, "1")).status());
        assertEquals("9.5000", jdbc.queryForObject("select cast(available as varchar) from erp_stock_balance where id = ?", String.class, s.balance()));
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_stock_movement where nature = 'CONSUMPTION_DEFLAGRATION' and lot_id = ?", Long.class, s.lot()));
    }
    @Test void concurrentRetriesProduceOnlyOneConsumption() throws Exception {
        var s = setup(); var data = payload(s, "2");
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Result> consume = () -> { start.await(); return request("POST", "ammunition-consumptions", data); };
            var first = executor.submit(consume); var second = executor.submit(consume); start.countDown();
            var a = first.get(30, java.util.concurrent.TimeUnit.SECONDS); var b = second.get(30, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(200, a.status(), a.raw()); assertEquals(200, b.status(), b.raw());
            assertEquals(a.body().get("id").asLong(), b.body().get("id").asLong());
        }
        assertEquals("8.5000", jdbc.queryForObject("select cast(available as varchar) from erp_stock_balance where id = ?", String.class, s.balance()));
        assertEquals("8.5000", jdbc.queryForObject("select cast(available_quantity as varchar) from erp_stock_lot where id = ?", String.class, s.lot()));
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_stock_movement where nature = 'CONSUMPTION_DEFLAGRATION' and lot_id = ?", Long.class, s.lot()));
    }
    @Test void stockAndHistoryAreScopedAndValidationRejectsInvalidLines() throws Exception {
        var s = setup(); assertEquals(1, request("GET", "ammunition-consumptions/stock?organizationId=" + s.organization() + "&unitId=" + s.unit(), null).body().get("totalElements").asInt());
        var invalid = payload(s, "0"); assertEquals(400, request("POST", "ammunition-consumptions", invalid).status());
        var created = request("POST", "ammunition-consumptions", payload(s, "1")); assertEquals(200, created.status(), created.raw());
        assertEquals(1, request("GET", "ammunition-consumptions?organizationId=" + s.organization() + "&unitId=" + s.unit(), null).body().get("totalElements").asInt());
        assertEquals(created.body().get("id").asLong(), request("GET", "ammunition-consumptions/" + created.body().get("id").asLong(), null).body().get("id").asLong());
    }
}
