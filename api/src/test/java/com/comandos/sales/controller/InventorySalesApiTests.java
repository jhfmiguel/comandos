package com.comandos.sales.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fariamiguel.app.core.service.CoreService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.net.CookieManager;
import java.net.CookiePolicy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:sales-tests;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "logging.level.root=WARN", "debug=false"
})
class InventorySalesApiTests {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired CoreService core;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
    private final JsonMapper json = JsonMapper.builder().build();
    private static final String PASSWORD = "Test-password-12345";
    record Result(int status, JsonNode body, String raw) {}
    record Setup(long organization, long buyer, long location, long asset, long lot, long balance, long assetModel) {}

    private Result request(String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/erp/" + path)).header("Content-Type", "application/json");
        if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("OPTIONS")) {
            JsonNode csrf = json.readTree(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/csrf"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString()).body());
            builder.header(csrf.get("headerName").asText(), csrf.get("token").asText());
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Result(response.statusCode(), response.body().isBlank() ? null : json.readTree(response.body()), response.body());
    }
    private void login() throws Exception {
        long person = (Long) core.save("people", null, Map.of("personType", "INDIVIDUAL", "fullName", "Sales integration actor", "active", true)).get("id");
        String username = unique();
        core.save("users", null, Map.of("personId", person, "login", username, "password", PASSWORD, "blocked", false));
        JsonNode csrf = json.readTree(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/csrf"))
                .GET().build(), HttpResponse.BodyHandlers.ofString()).body());
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .POST(HttpRequest.BodyPublishers.ofString("username=" + username + "&password=" + PASSWORD)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(204, response.statusCode(), response.body());
    }
    private void logout() throws Exception {
        JsonNode csrf = json.readTree(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/csrf"))
                .GET().build(), HttpResponse.BodyHandlers.ofString()).body());
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/logout"))
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, response.statusCode(), response.body());
    }
    private long create(String path, Map<String, Object> data) throws Exception {
        var response = request("POST", path, data);
        assertEquals(201, response.status(), response.raw());
        return response.body().get("id").asLong();
    }
    private String unique() { return UUID.randomUUID().toString(); }
    @Test
    void browserCanAccessSalesFromConfiguredOriginOnly() throws Exception {
        for (String path : List.of("sales", "sales/stock?organizationId=1&kind=ASSET")) {
            String method = path.equals("sales") ? "POST" : "GET";
            var preflight = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/erp/" + path))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", method)
                .header("Access-Control-Request-Headers", "content-type").build();
            var response = client.send(preflight, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            assertEquals("http://localhost:3000", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
            assertTrue(response.headers().firstValue("Access-Control-Allow-Methods").orElseThrow().contains(method));
        }
        var allowed = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/erp/sales?organizationId=1"))
            .header("Origin", "http://localhost:3000").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, allowed.statusCode(), allowed.body());
        assertEquals("http://localhost:3000", allowed.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        var denied = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/erp/sales?organizationId=1"))
            .header("Origin", "https://untrusted.example").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, denied.statusCode());
        assertTrue(denied.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
    }

    private Setup setup() throws Exception {
        long org = create("core/organizations", Map.of("name", unique(), "nature", "Company", "publicOrganization", false, "active", true));
        long buyer = create("core/people", Map.of("personType", "INDIVIDUAL", "fullName", "Test buyer", "active", true));
        long location = create("inventory/locations", Map.of("organizationId", org, "name", "Warehouse", "type", "Warehouse", "controlled", true));
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Test manufacturer"));
        long assetModel = model(false, brand);
        long lotModel = model(true, brand);
        long asset = create("inventory/assets", Map.of("modelId", assetModel, "locationId", location, "assetCode", unique(),
            "serialNumber", unique(), "condition", "NEW", "status", "AVAILABLE", "currentValue", "10"));
        long lot = create("inventory/lots", Map.of("modelId", lotModel, "openingLocationId", location, "lotNumber", unique(), "initialQuantity", "10.125"));
        long balance = jdbc.queryForObject("select id from erp_stock_balance where lot_id = ?", Long.class, lot);
        return new Setup(org, buyer, location, asset, lot, balance, assetModel);
    }
    private long model(boolean lot, long brand) throws Exception {
        long category = create("inventory/categories", Map.of("name", unique(), "family", "GENERAL", "serialized", !lot, "lotControlled", lot, "consumable", lot));
        return create("inventory/models", Map.of("name", "Test model", "categoryId", category, "brandId", brand,
            "unitOfMeasure", "EA", "sku", unique(), "listPrice", "12.3456"));
    }
    private Map<String, Object> line(String field, long id, String quantity) {
        return Map.of(field, id, "quantity", quantity, "expectedUnitPrice", "12.3456");
    }
    private Map<String, Object> sale(Setup s, List<Map<String, Object>> items) {
        return new HashMap<>(Map.of(
            "requestId", unique(),
            "organizationId", s.organization(),
            "buyerId", s.buyer(),
            "paymentMethod", "PIX",
            "processNumber", "SALE-" + unique(),
            "legalBasis", "Integration test sale",
            "documentReference", "DOC-" + unique(),
            "items", items));
    }
    private Map<String, Object> mixed(Setup s) {
        return sale(s, List.of(line("assetId", s.asset(), "1"), line("balanceId", s.balance(), "2.125")));
    }
    private void stockUnchanged(Setup s) {
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.asset()));
        assertEquals("10.1250", jdbc.queryForObject("select available from erp_stock_balance where id = ?", BigDecimal.class, s.balance()).toPlainString());
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_sale where organization_id = ?", Long.class, s.organization()));
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_stock_movement where location_id = ? and nature = 'SALE'", Long.class, s.location()));
    }

    @Test
    void mixedSaleSnapshotsPricesAndBuyerAndDeductsStockAtomically() throws Exception {
        var s = setup();
        var response = request("POST", "sales", mixed(s));
        assertEquals(200, response.status(), response.raw());
        assertEquals("38.5800", response.body().get("total").asText());
        var auditJson = jdbc.queryForObject("select after_json from erp_audit_record where resource_name = 'sales' and record_id = ?", String.class, response.body().get("id").asLong());
        var audit = json.readTree(auditJson);
        assertEquals(3, audit.get("stockChanges").size());
        assertEquals("SOLD", audit.get("stockChanges").get(0).get("after").get("status").asText());
        assertEquals("38.5800", audit.get("sale").get("total").asText());
        assertEquals(2, response.body().get("items").size());
        assertEquals("SOLD", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.asset()));
        assertEquals("8.0000", jdbc.queryForObject("select available from erp_stock_balance where id = ?", BigDecimal.class, s.balance()).toPlainString());
        assertEquals("8.0000", jdbc.queryForObject("select available_quantity from erp_stock_lot where id = ?", BigDecimal.class, s.lot()).setScale(4).toPlainString());
        assertEquals("-3.1250", jdbc.queryForObject("select sum(quantity) from erp_stock_movement where location_id = ? and nature = 'SALE'", BigDecimal.class, s.location()).setScale(4).toPlainString());
        jdbc.update("update erp_person set full_name = 'Changed buyer' where id = ?", s.buyer());
        jdbc.update("update erp_item_model set name = 'Changed model', list_price = 99 where id = ?", s.assetModel());
        var detail = request("GET", "sales/" + response.body().get("id").asLong(), null);
        assertEquals("Test buyer", detail.body().get("buyerName").asText());
        assertEquals("Test model", detail.body().get("items").get(0).get("modelName").asText());
        assertEquals("12.3456", detail.body().get("items").get(0).get("unitPrice").asText());
        assertEquals(1, request("GET", "sales?organizationId=" + s.organization(), null).body().get("totalElements").asInt());
    }

    @Test
    void genericCommercialOrderUsesScopedReservationAndSpecializedStockSale() throws Exception {
        login();
        var s = setup();
        String reference = "COM-" + unique();
        var created = request("POST", "commercial-sales/orders", Map.of(
            "reference", reference,
            "customerId", Long.toString(s.buyer()),
            "organizationId", s.organization(),
            "buyerId", s.buyer(),
            "paymentMethod", "PIX",
            "processNumber", "PROC-" + reference,
            "legalBasis", "Authorized commercial sale",
            "documentReference", "DOC-" + reference,
            "lines", List.of(Map.of("itemId", "ASSET:" + s.asset(), "description", "Test asset",
                "quantity", "1", "unitPrice", "12.3456"))));
        assertEquals(200, created.status(), created.raw());
        String id = created.body().get("id").asText();

        assertEquals("SUBMITTED", request("POST", "commercial-sales/orders/" + id + "/submit", null)
            .body().get("status").asText());
        assertEquals(409, request("POST", "commercial-sales/orders/" + id + "/approve", null).status());
        logout();
        login();
        assertEquals("APPROVED", request("POST", "commercial-sales/orders/" + id + "/approve", null)
            .body().get("status").asText());
        var confirmed = request("POST", "commercial-sales/orders/" + id + "/confirm", null);
        assertEquals(200, confirmed.status(), confirmed.raw());
        assertEquals("CONFIRMED", confirmed.body().get("status").asText());
        assertEquals("BLOCKED", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.asset()));
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_inventory_reservation", Long.class));

        var invoice = request("POST", "commercial-sales/orders/" + id + "/invoice", Map.of(
            "issuedOn", "2026-11-06", "dueOn", "2026-11-13"));
        assertEquals(200, invoice.status(), invoice.raw());
        var delivered = request("POST", "commercial-sales/orders/" + id + "/deliveries", Map.of(
            "reference", "DEL-" + reference,
            "lines", List.of(Map.of("itemId", "ASSET:" + s.asset(), "quantity", "1")),
            "metadata", Map.of()));
        assertEquals(200, delivered.status(), delivered.raw());
        assertEquals("SOLD", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, s.asset()));
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_sale where organization_id = ?", Long.class, s.organization()));
        assertTrue(jdbc.queryForObject("select setting_value from platform_setting where value_type = 'SALES_ROUNDING_ADJUSTMENT'", String.class)
                .contains("adjustment=0.0044"));
        assertEquals("DELIVERED", request("GET", "commercial-sales/orders/" + id, null).body().get("status").asText());
    }

    @Test
    void failedSecondLineRollsBackSaleAssetAndEveryMovement() throws Exception {
        var s = setup();
        var request = sale(s, List.of(line("assetId", s.asset(), "1"), line("balanceId", s.balance(), "11")));
        assertEquals(409, request("POST", "sales", request).status());
        stockUnchanged(s);
    }

    @Test
    void requiredFieldsQuantitiesDuplicatesAndScopeAreValidated() throws Exception {
        var s = setup();
        for (String missing : List.of("paymentMethod", "buyerId", "organizationId", "requestId", "processNumber", "legalBasis", "documentReference", "items")) {
            var data = mixed(s); data.remove(missing);
            assertEquals(400, request("POST", "sales", data).status(), missing);
        }
        for (String quantity : List.of("0", "-1", "0.00001", "1000000000000000")) {
            assertEquals(400, request("POST", "sales", sale(s, List.of(line("balanceId", s.balance(), quantity)))).status());
        }
        assertEquals(400, request("POST", "sales", sale(s, List.of(line("assetId", s.asset(), "2")))).status());
        var item = line("assetId", s.asset(), "1");
        assertEquals(400, request("POST", "sales", sale(s, List.of(item, item))).status());
        var other = setup();
        assertEquals(400, request("POST", "sales", sale(s, List.of(line("assetId", other.asset(), "1")))).status());
        stockUnchanged(s); stockUnchanged(other);
    }

    @Test
    void availabilityFiltersExpiredAndSoldStockAndRejectsChangedPrices() throws Exception {
        var s = setup();
        var assets = request("GET", "sales/stock?organizationId=" + s.organization() + "&kind=ASSET&search=Test", null);
        assertEquals(200, assets.status(), assets.raw());
        assertEquals(1, assets.body().get("totalElements").asInt());
        assertEquals(1, request("GET", "sales/stock?organizationId=" + s.organization() + "&kind=LOT", null).body().get("totalElements").asInt());
        jdbc.update("update erp_item_model set list_price = 20 where id = ?", s.assetModel());
        assertEquals(409, request("POST", "sales", mixed(s)).status());
        stockUnchanged(s);
        jdbc.update("update erp_stock_lot set valid_until = '2000-01-01' where id = ?", s.lot());
        assertEquals(0, request("GET", "sales/stock?organizationId=" + s.organization() + "&kind=LOT", null).body().get("totalElements").asInt());
        assertEquals(400, request("POST", "sales", sale(s, List.of(line("balanceId", s.balance(), "1")))).status());
        stockUnchanged(s);
    }

    @Test
    void soldAssetCannotBeReactivatedThroughInventoryCrud() throws Exception {
        var s = setup();
        assertEquals(200, request("POST", "sales", mixed(s)).status());
        var asset = request("GET", "inventory/assets/" + s.asset(), null).body();
        Map<String, Object> data = new HashMap<>();
        for (String field : List.of("modelId", "locationId", "assetCode", "serialNumber", "condition", "currentValue", "version")) data.put(field, asset.get(field).asText());
        data.put("status", "AVAILABLE");
        assertEquals(400, request("PUT", "inventory/assets/" + s.asset(), data).status());
        assertEquals(0, request("GET", "sales/stock?organizationId=" + s.organization() + "&kind=ASSET", null).body().get("totalElements").asInt());
    }

    @Test
    void retriesReturnSameSaleAndRejectDifferentPayload() throws Exception {
        var s = setup();
        var data = mixed(s);
        var first = request("POST", "sales", data);
        assertEquals(200, first.status(), first.raw());
        var retry = request("POST", "sales", data);
        assertEquals(200, retry.status(), retry.raw());
        assertEquals(first.body().get("id").asLong(), retry.body().get("id").asLong());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource_name = 'sales' and record_id = ?", Long.class, first.body().get("id").asLong()));
        data.put("paymentMethod", "CASH");
        assertEquals(409, request("POST", "sales", data).status());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_sale where organization_id = ?", Long.class, s.organization()));
    }

    @Test
    void competingSalesCannotOversellAndConcurrentRetriesDeductOnlyOnce() throws Exception {
        var s = setup();
        var a = sale(s, List.of(line("balanceId", s.balance(), "7")));
        var b = sale(s, List.of(line("balanceId", s.balance(), "7")));
        var results = concurrent(a, b);
        assertEquals(List.of(200, 409), results.stream().map(Result::status).sorted().toList(), results.toString());
        assertEquals("3.1250", jdbc.queryForObject("select available from erp_stock_balance where id = ?", BigDecimal.class, s.balance()).toPlainString());
        var asset = sale(s, List.of(line("assetId", s.asset(), "1")));
        var retries = concurrent(asset, asset);
        assertTrue(retries.stream().allMatch(result -> result.status() == 200), retries.toString());
        assertEquals(retries.get(0).body().get("id").asLong(), retries.get(1).body().get("id").asLong());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_stock_movement where asset_id = ? and nature = 'SALE'", Long.class, s.asset()));
    }

    @Test
    void unitSalesValidateBothStockKindsAndPreserveHistoricalOwnership() throws Exception {
        var s = setup();
        long unit = create("core/units", Map.of("organizationId", s.organization(), "code", "MAIN", "name", "Original unit", "type", "Office"));
        long sibling = create("core/units", Map.of("organizationId", s.organization(), "code", "OTHER", "name", "Other unit", "type", "Office"));
        var data = mixed(s); data.put("unitId", unit);
        assertEquals(400, request("POST", "sales", data).status());
        stockUnchanged(s);
        jdbc.update("update erp_stock_location set unit_id = ? where id = ?", unit, s.location());
        for (String kind : List.of("ASSET", "LOT")) {
            assertEquals(1, request("GET", "sales/stock?organizationId=" + s.organization() + "&unitId=" + unit + "&kind=" + kind, null).body().get("totalElements").asInt());
            assertEquals(0, request("GET", "sales/stock?organizationId=" + s.organization() + "&unitId=" + sibling + "&kind=" + kind, null).body().get("totalElements").asInt());
        }
        data.put("unitId", sibling);
        for (String kind : List.of("assetId", "balanceId")) {
            data.put("items", List.of(line(kind, kind.equals("assetId") ? s.asset() : s.balance(), "1")));
            assertEquals(400, request("POST", "sales", data).status());
            stockUnchanged(s);
        }
        long otherOrg = create("core/organizations", Map.of("name", unique(), "nature", "Company", "active", true, "publicOrganization", false));
        long foreignUnit = create("core/units", Map.of("organizationId", otherOrg, "code", "FOREIGN", "name", "Foreign unit", "type", "Office"));
        data.put("unitId", foreignUnit);
        assertEquals(400, request("POST", "sales", data).status());
        assertEquals(400, request("GET", "sales?organizationId=" + s.organization() + "&unitId=" + foreignUnit, null).status());
        data.put("unitId", unit); data.put("items", mixed(s).get("items"));
        var result = request("POST", "sales", data);
        assertEquals(200, result.status(), result.raw());
        long id = result.body().get("id").asLong();
        data.put("unitId", sibling);
        assertEquals(409, request("POST", "sales", data).status());
        data.remove("unitId");
        assertEquals(409, request("POST", "sales", data).status());
        assertEquals(1, request("GET", "sales?organizationId=" + s.organization(), null).body().get("totalElements").asInt());
        assertEquals(0, request("GET", "sales?organizationId=" + s.organization() + "&unitId=" + sibling, null).body().get("totalElements").asInt());
        jdbc.update("update erp_stock_location set unit_id = null where id = ?", s.location());
        var change = new HashMap<String, Object>(Map.of("organizationId", otherOrg, "code", "MAIN", "name", "Renamed unit", "type", "Office", "version", 0));
        var moved = request("PUT", "core/units/" + unit, change);
        assertEquals(400, moved.status(), moved.raw());
        assertTrue(moved.raw().contains("unit has sales"));
        change.put("organizationId", s.organization());
        assertEquals(200, request("PUT", "core/units/" + unit, change).status());
        assertEquals("Original unit", request("GET", "sales/" + id, null).body().get("unitName").asText());
    }

    @Test
    void partialReturnAndCancellationRestoreStockAndPreserveReceipt() throws Exception {
        var s = setup();
        var finalized = request("POST", "sales", mixed(s));
        long saleId = finalized.body().get("id").asLong();
        long reason = jdbc.queryForObject("select id from erp_sale_return_reason_type where code='CUSTOMER_RETURN'", Long.class);
        JsonNode assetLine = null, lotLine = null;
        for (var item : finalized.body().get("items")) if (item.get("assetId").isNull()) lotLine = item; else assetLine = item;
        var partial = new HashMap<String,Object>(); partial.put("requestId",unique());partial.put("reasonId",reason);partial.put("notes","Partial package return");partial.put("cancellation",false);partial.put("refundReference","REF-1");partial.put("items",List.of(Map.of("saleItemId",lotLine.get("id").asLong(),"quantity","1.125")));
        var returned = request("POST","sales/"+saleId+"/returns",partial);
        assertEquals(200,returned.status(),returned.raw());assertEquals("13.8888",returned.body().get("refundAmount").asText());
        assertEquals("9.1250",jdbc.queryForObject("select available from erp_stock_balance where id=?",BigDecimal.class,s.balance()).toPlainString());
        assertEquals(returned.body().get("id").asLong(),request("POST","sales/"+saleId+"/returns",partial).body().get("id").asLong());
        var cancel = new HashMap<String,Object>();cancel.put("requestId",unique());cancel.put("reasonId",reason);cancel.put("notes","Cancel remaining sale");cancel.put("cancellation",true);cancel.put("items",List.of(Map.of("saleItemId",assetLine.get("id").asLong(),"quantity","1"),Map.of("saleItemId",lotLine.get("id").asLong(),"quantity","1")));
        var cancelled=request("POST","sales/"+saleId+"/returns",cancel);assertEquals(200,cancelled.status(),cancelled.raw());assertTrue(cancelled.body().get("cancellation").asBoolean());
        assertEquals("AVAILABLE",jdbc.queryForObject("select status from erp_asset_item where id=?",String.class,s.asset()));assertEquals("10.1250",jdbc.queryForObject("select available from erp_stock_balance where id=?",BigDecimal.class,s.balance()).toPlainString());
        var receipt=request("GET","sales/"+saleId,null);assertEquals("FINALIZED",receipt.body().get("status").asText());assertEquals(2,receipt.body().get("returns").size());assertEquals(5,jdbc.queryForObject("select count(*) from erp_stock_movement where location_id=? and nature like 'SALE%'",Integer.class,s.location()));
    }

    private List<Result> concurrent(Map<String, Object> a, Map<String, Object> b) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Result>> futures = new ArrayList<>();
            for (var payload : List.of(a, b)) futures.add(executor.submit(() -> {
                ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS)); return request("POST", "sales", payload);
            }));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            return List.of(futures.get(0).get(30, TimeUnit.SECONDS), futures.get(1).get(30, TimeUnit.SECONDS));
        }
    }
}
