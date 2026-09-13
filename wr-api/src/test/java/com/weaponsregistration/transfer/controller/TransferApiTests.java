package com.weaponsregistration.transfer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:transfer-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "logging.level.root=WARN", "debug=false"
})
class TransferApiTests {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    record Result(int status, JsonNode body, String raw) {}
    record Setup(long organization, long sourceUnit, long destinationUnit, long sourceLocation,
        long destinationLocation, long asset, long lot, long sourceBalance) {}

    @Test
    void transfersAssetsAndLotsIdempotently() throws Exception {
        Setup setup = setup();
        Map<String, Object> payload = payload(setup);
        Result first = request("POST", "transfers", payload);
        assertEquals(200, first.status(), first.raw());
        long transferId = first.body().get("id").asLong();
        assertEquals(setup.destinationLocation(), jdbc.queryForObject(
            "select location_id from erp_asset_item where id=?", Long.class, setup.asset()));
        assertEquals("7.5000", decimal("select available from erp_stock_balance where id=?", setup.sourceBalance()));
        assertEquals("2.5000", decimal("select available from erp_stock_balance where lot_id=? and location_id=?",
            setup.lot(), setup.destinationLocation()));
        assertEquals("10.0000", decimal("select available_quantity from erp_stock_lot where id=?", setup.lot()));
        assertEquals(2, jdbc.queryForObject("select count(*) from erp_stock_movement where nature='TRANSFER_OUT'", Integer.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from erp_stock_movement where nature='TRANSFER_IN'", Integer.class));
        Result repeated = request("POST", "transfers", payload);
        assertEquals(200, repeated.status(), repeated.raw());
        assertEquals(transferId, repeated.body().get("id").asLong());
        assertEquals(2, jdbc.queryForObject("select count(*) from erp_inventory_transfer_item where transfer_id=?", Integer.class, transferId));
        assertEquals(1, jdbc.queryForObject("select count(*) from erp_audit_record where resource='transfers' and record_id=?", Integer.class, transferId));
    }

    @Test
    void invalidSecondItemRollsBackTheWholeTransfer() throws Exception {
        Setup setup = setup();
        Map<String, Object> payload = new HashMap<>(payload(setup));
        payload.put("items", List.of(Map.of("assetId", setup.asset(), "quantity", 1),
            Map.of("balanceId", setup.sourceBalance(), "quantity", "20.0000")));
        Result response = request("POST", "transfers", payload);
        assertEquals(409, response.status(), response.raw());
        assertEquals(setup.sourceLocation(), jdbc.queryForObject(
            "select location_id from erp_asset_item where id=?", Long.class, setup.asset()));
        assertEquals("10.0000", decimal("select available from erp_stock_balance where id=?", setup.sourceBalance()));
        assertEquals(0, jdbc.queryForObject("select count(*) from erp_inventory_transfer where request_id=?", Integer.class,
            payload.get("requestId")));
        assertEquals(0, jdbc.queryForObject("select count(*) from erp_stock_movement where nature like 'TRANSFER_%'", Integer.class));
    }

    @Test
    void exposesSourceStockAndHistoryForBothUnits() throws Exception {
        Setup setup = setup();
        Result stock = request("GET", "transfers/stock?organizationId=" + setup.organization()
            + "&sourceUnitId=" + setup.sourceUnit() + "&kind=ASSET", null);
        assertEquals(200, stock.status(), stock.raw());
        assertEquals(1, stock.body().get("totalElements").asInt());
        Result created = request("POST", "transfers", payload(setup));
        assertEquals(200, created.status(), created.raw());
        assertEquals(0, request("GET", "transfers/stock?organizationId=" + setup.organization()
            + "&sourceUnitId=" + setup.sourceUnit() + "&kind=ASSET", null).body().get("totalElements").asInt());
        assertEquals(1, request("GET", "transfers?organizationId=" + setup.organization()
            + "&unitId=" + setup.sourceUnit(), null).body().get("totalElements").asInt());
        assertEquals(1, request("GET", "transfers?organizationId=" + setup.organization()
            + "&unitId=" + setup.destinationUnit(), null).body().get("totalElements").asInt());
        long id = created.body().get("id").asLong();
        assertEquals(id, request("GET", "transfers/" + id, null).body().get("id").asLong());
    }

    private Setup setup() throws Exception {
        long organization = create("core/organizations", Map.of("name", unique(), "nature", "Public safety",
            "publicOrganization", true, "active", true));
        long sourceUnit = create("core/units", Map.of("organizationId", organization, "code", unique(),
            "name", "Source unit", "type", "Unit"));
        long destinationUnit = create("core/units", Map.of("organizationId", organization, "code", unique(),
            "name", "Destination unit", "type", "Unit"));
        long assetCategory = create("inventory/categories", Map.of("name", unique(), "family", "GENERAL",
            "serialized", true, "lotControlled", false, "consumable", false));
        long lotCategory = create("inventory/categories", Map.of("name", unique(), "family", "GENERAL",
            "serialized", false, "lotControlled", true, "consumable", true));
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Maker"));
        long assetModel = create("inventory/models", Map.of("categoryId", assetCategory, "brandId", brand,
            "name", "Asset", "unitOfMeasure", "EA", "sku", unique(), "listPrice", "10"));
        long lotModel = create("inventory/models", Map.of("categoryId", lotCategory, "brandId", brand,
            "name", "Supply", "unitOfMeasure", "EA", "sku", unique(), "listPrice", "1"));
        long sourceLocation = create("inventory/locations", Map.of("organizationId", organization, "unitId", sourceUnit,
            "name", "Source store", "type", "Controlled", "controlled", true));
        long destinationLocation = create("inventory/locations", Map.of("organizationId", organization,
            "unitId", destinationUnit, "name", "Destination store", "type", "Controlled", "controlled", true));
        long asset = create("inventory/assets", Map.of("modelId", assetModel, "locationId", sourceLocation,
            "assetCode", unique(), "serialNumber", unique(), "condition", "GOOD", "status", "AVAILABLE", "currentValue", "10"));
        long lot = create("inventory/lots", Map.of("modelId", lotModel, "openingLocationId", sourceLocation,
            "lotNumber", unique(), "initialQuantity", "10"));
        long sourceBalance = jdbc.queryForObject("select id from erp_stock_balance where lot_id=?", Long.class, lot);
        return new Setup(organization, sourceUnit, destinationUnit, sourceLocation, destinationLocation, asset, lot, sourceBalance);
    }

    private Map<String, Object> payload(Setup setup) {
        return Map.of("requestId", unique(), "organizationId", setup.organization(), "sourceUnitId", setup.sourceUnit(),
            "destinationUnitId", setup.destinationUnit(), "destinationLocationId", setup.destinationLocation(),
            "purpose", "Operational redistribution", "items", List.of(Map.of("assetId", setup.asset(), "quantity", 1),
                Map.of("balanceId", setup.sourceBalance(), "quantity", "2.5000")));
    }

    private Result request(String method, String path, Object body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/erp/" + path))
            .header("Content-Type", "application/json").method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new Result(response.statusCode(), response.body().isBlank() ? null : json.readTree(response.body()), response.body());
    }

    private long create(String path, Map<String, Object> body) throws Exception {
        Result result = request("POST", path, body);
        assertEquals(201, result.status(), result.raw());
        return result.body().get("id").asLong();
    }

    private String decimal(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, BigDecimal.class, arguments).setScale(4).toPlainString();
    }

    private String unique() { return UUID.randomUUID().toString(); }
}
