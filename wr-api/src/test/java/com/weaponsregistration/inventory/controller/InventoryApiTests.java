package com.weaponsregistration.inventory.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:inventory-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "logging.level.root=WARN", "debug=false"
})
class InventoryApiTests {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    record Result(int status, JsonNode body, String raw) {}
    record Setup(long category, long model, long organization, long location) {}

    private Result request(String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/erp/" + path));
        builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Result(response.statusCode(), response.body().isBlank() ? null : json.readTree(response.body()), response.body());
    }
    private JsonNode create(String path, Map<String, Object> data) throws Exception {
        var result = request("POST", path, data);
        assertEquals(201, result.status(), result.raw());
        return result.body();
    }
    private String unique() { return UUID.randomUUID().toString(); }
    private Map<String, Object> categoryData(boolean lot) {
        return new HashMap<>(Map.of("name", unique(), "family", "GENERAL", "serialized", !lot, "lotControlled", lot, "consumable", lot));
    }
    private long organization() throws Exception {
        return create("core/organizations", Map.of("name", unique(), "nature", "Company", "publicOrganization", false, "active", true)).get("id").asLong();
    }
    private Setup setup(boolean lot) throws Exception {
        long category = create("inventory/categories", categoryData(lot)).get("id").asLong();
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Example manufacturer")).get("id").asLong();
        long model = create("inventory/models", Map.of("name", unique(), "categoryId", category, "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "123.4500")).get("id").asLong();
        long org = organization();
        long location = create("inventory/locations", Map.of("organizationId", org, "name", unique(), "type", "Warehouse", "controlled", true)).get("id").asLong();
        return new Setup(category, model, org, location);
    }
    private Map<String, Object> assetData(Setup setup) {
        return new HashMap<>(Map.of("modelId", setup.model(), "locationId", setup.location(), "assetCode", unique(),
            "serialNumber", unique(), "condition", "NEW", "status", "AVAILABLE", "currentValue", "123.45"));
    }
    private Map<String, Object> lotData(Setup setup) {
        return new HashMap<>(Map.of("modelId", setup.model(), "openingLocationId", setup.location(), "lotNumber", unique(), "initialQuantity", "10.1250"));
    }

    @Test
    void assetBatchPreservesPairsCountsSerialsAndRecoversAnIdenticalRetry() throws Exception {
        var s = setup(false);
        var common = assetData(s); common.remove("assetCode"); common.remove("serialNumber");
        String code = unique(); String serial = unique();
        var rows = List.of(Map.of("assetCode", code, "serialNumber", serial), Map.of("assetCode", unique(), "serialNumber", unique()));
        var payload = new HashMap<String, Object>(Map.of("requestId", unique(), "common", common, "items", rows));
        var result = request("POST", "inventory/assets/batch", payload);
        assertEquals(200, result.status(), result.raw());
        assertEquals("2", result.body().get("quantity").asText());
        assertEquals(2, result.body().get("recordIds").size());
        long first = result.body().get("recordIds").get(0).asLong();
        assertEquals(code, jdbc.queryForObject("select asset_code from erp_asset_item where id=?", String.class, first));
        assertEquals(serial, jdbc.queryForObject("select serial_number from erp_asset_item where id=?", String.class, first));
        var retry = request("POST", "inventory/assets/batch", payload);
        assertEquals(200, retry.status(), retry.raw());
        assertEquals(result.body(), retry.body());
        assertEquals(2L, jdbc.queryForObject("select count(*) from erp_asset_item where model_id=?", Long.class, s.model()));
        assertEquals(2L, jdbc.queryForObject("select count(*) from erp_stock_movement where location_id=?", Long.class, s.location()));
        common.put("currentValue", "999");
        assertEquals(409, request("POST", "inventory/assets/batch", payload).status());
    }

    @Test
    void invalidBatchRowRollsBackAssetsMovementsAndAuditAndCanBeCorrected() throws Exception {
        var s = setup(false);
        var common = assetData(s); common.remove("assetCode"); common.remove("serialNumber");
        String serial = unique();
        var payload = new HashMap<String, Object>(Map.of("requestId", unique(), "common", common,
            "items", List.of(Map.of("assetCode", unique(), "serialNumber", serial), Map.of("assetCode", unique(), "serialNumber", serial))));
        var invalid = request("POST", "inventory/assets/batch", payload);
        assertEquals(400, invalid.status(), invalid.raw()); assertTrue(invalid.raw().contains("Row 2"));
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_asset_item where model_id=?", Long.class, s.model()));
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_stock_movement where location_id=?", Long.class, s.location()));
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_stock_intake where request_id=?", Long.class, payload.get("requestId")));
        payload.put("items", List.of(Map.of("assetCode", unique(), "serialNumber", " ")));
        assertEquals(400, request("POST", "inventory/assets/batch", payload).status());
        payload.put("items", List.of(Map.of("assetCode", unique(), "serialNumber", serial)));
        assertEquals(200, request("POST", "inventory/assets/batch", payload).status());
        payload.put("requestId", unique());
        payload.put("items", List.of(Map.of("assetCode", unique(), "serialNumber", serial)));
        var duplicate = request("POST", "inventory/assets/batch", payload);
        assertEquals(400, duplicate.status(), duplicate.raw()); assertTrue(duplicate.raw().contains("Row 1"));
    }

    @Test
    void batchDuplicateSerialReportsItsRowForNumericStringModelIds() throws Exception {
        var s = setup(false);
        var registered = assetData(s);
        create("inventory/assets", registered);
        var common = assetData(s); common.remove("assetCode"); common.remove("serialNumber");
        common.put("modelId", "0" + s.model());
        var payload = Map.of("requestId", unique(), "common", common, "items", List.of(
            Map.of("assetCode", unique(), "serialNumber", unique()),
            Map.of("assetCode", unique(), "serialNumber", registered.get("serialNumber"))));
        var result = request("POST", "inventory/assets/batch", payload);
        assertEquals(400, result.status(), result.raw());
        assertTrue(result.raw().contains("Row 2"), result.raw());
        assertTrue(result.raw().contains("Serial number is already registered"), result.raw());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_asset_item where model_id=?", Long.class, s.model()));
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_stock_movement where location_id=?", Long.class, s.location()));
    }

    @Test
    void ammunitionBoxesCalculateAndPersistMixedBoxSizesAsRounds() throws Exception {
        var s = setup(true);
        jdbc.update("update erp_item_category set family='AMMUNITION' where id=?", s.category());
        var payload = new HashMap<String, Object>(Map.of("requestId", unique(), "modelId", s.model(),
            "openingLocationId", s.location(), "lotNumber", unique(),
            "boxes", List.of(Map.of("boxes", "10", "roundsPerBox", "50"), Map.of("boxes", "2", "roundsPerBox", "25"))));
        var created = request("POST", "inventory/lots/from-boxes", payload);
        assertEquals(200, created.status(), created.raw());
        assertEquals("550", created.body().get("quantity").asText());
        long lot = created.body().get("recordIds").get(0).asLong();
        assertEquals(550, jdbc.queryForObject("select available from erp_stock_balance where lot_id=?", java.math.BigDecimal.class, lot).intValueExact());
        assertEquals(550, jdbc.queryForObject("select quantity from erp_stock_movement where lot_id=?", java.math.BigDecimal.class, lot).intValueExact());
        assertEquals("10 x 50 + 2 x 25", request("GET", "inventory/lots/" + lot, null).body().get("openingPackaging").asText());
        assertEquals(created.body(), request("POST", "inventory/lots/from-boxes", payload).body());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_stock_movement where lot_id=?", Long.class, lot));
        payload.put("boxes", List.of(Map.of("boxes", "11", "roundsPerBox", "50")));
        assertEquals(409, request("POST", "inventory/lots/from-boxes", payload).status());
    }

    @Test
    void ammunitionBoxEntryRejectsFractionsZeroOverflowAndOtherModels() throws Exception {
        var s = setup(true);
        var payload = new HashMap<String, Object>(Map.of("requestId", unique(), "modelId", s.model(), "openingLocationId", s.location(),
            "lotNumber", unique(), "boxes", List.of(Map.of("boxes", "10", "roundsPerBox", "50"))));
        assertEquals(400, request("POST", "inventory/lots/from-boxes", payload).status());
        jdbc.update("update erp_item_category set family='AMMUNITION' where id=?", s.category());
        for (String invalid : List.of("0", "-1", "1.5", "", "1e2", "999999999999999")) {
            payload.put("boxes", List.of(Map.of("boxes", invalid, "roundsPerBox", "50")));
            assertEquals(400, request("POST", "inventory/lots/from-boxes", payload).status());
        }
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_stock_lot where model_id=?", Long.class, s.model()));
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_stock_movement where location_id=?", Long.class, s.location()));
    }

    @Test
    void catalogAndSearchCoverEveryResourceAndStockHistoryIsReadOnly() throws Exception {
        var catalog = request("GET", "inventory/catalog", null);
        assertEquals(200, catalog.status());
        assertEquals(31, catalog.body().size());
        for (var resource : catalog.body()) {
            var list = request("GET", "inventory/" + resource.get("key").asText() + "?search=test", null);
            assertEquals(200, list.status(), list.raw());
        }
        assertEquals(405, request("POST", "inventory/balances", Map.of()).status());
        assertEquals(405, request("PUT", "inventory/movements/1", Map.of()).status());
        assertEquals(405, request("DELETE", "inventory/movements/1?version=0", null).status());
        assertEquals(400, request("GET", "inventory/models?size=101", null).status());
    }

    @Test
    void equipmentSetsRequireValidComponentsBeforeActivation() throws Exception {
        var assetSetup = setup(false);
        long asset = create("inventory/assets", assetData(assetSetup)).get("id").asLong();
        var lotSetup = setup(true);
        long lot = create("inventory/lots", lotData(lotSetup)).get("id").asLong();
        long balance = jdbc.queryForObject("select id from erp_stock_balance where lot_id = ?", Long.class, lot);

        var setData = new HashMap<String, Object>(Map.of("organizationId", assetSetup.organization(), "code", unique(),
            "name", "Patrol equipment set", "description", "Operational equipment", "active", false));
        var set = create("inventory/equipment-sets", setData);
        long setId = set.get("id").asLong();
        setData.put("version", set.get("version").asLong());
        setData.put("active", true);
        assertEquals(400, request("PUT", "inventory/equipment-sets/" + setId, setData).status());

        var assetComponent = create("inventory/equipment-set-components", Map.of("equipmentSetId", setId,
            "assetId", asset, "role", "Primary equipment", "quantity", "1"));
        assertEquals("Patrol equipment set", set.get("name").asText());
        assertTrue(assetComponent.get("label").asText().contains(set.get("code").asText()));

        assertEquals(400, request("POST", "inventory/equipment-set-components", Map.of("equipmentSetId", setId,
            "balanceId", balance, "role", "Supplies", "quantity", "1")).status());

        var activated = request("PUT", "inventory/equipment-sets/" + setId, setData);
        assertEquals(200, activated.status(), activated.raw());
        assertTrue(activated.body().get("active").asBoolean());
        assertEquals(400, request("DELETE", "inventory/equipment-set-components/" + assetComponent.get("id").asLong()
            + "?version=" + assetComponent.get("version").asLong(), null).status());

        var otherSet = create("inventory/equipment-sets", Map.of("organizationId", assetSetup.organization(), "code", unique(),
            "name", "Second set", "active", false));
        create("inventory/equipment-set-components", Map.of("equipmentSetId", otherSet.get("id").asLong(),
            "assetId", asset, "role", "Duplicate equipment", "quantity", "1"));
        assertEquals(400, request("PUT", "inventory/equipment-sets/" + otherSet.get("id").asLong(),
            Map.of("organizationId", assetSetup.organization(), "code", otherSet.get("code").asText(), "name", "Second set",
                "active", true, "version", otherSet.get("version").asLong())).status());
    }

    @Test
    void categoriesRejectCyclesAndTrackingChangesAfterModelsExist() throws Exception {
        var data = categoryData(false);
        var parent = create("inventory/categories", data);
        var childData = categoryData(false);
        childData.put("parentCategoryId", parent.get("id").asLong());
        var child = create("inventory/categories", childData);
        data.put("version", 0);
        data.put("parentCategoryId", child.get("id").asLong());
        assertEquals(400, request("PUT", "inventory/categories/" + parent.get("id").asLong(), data).status());
        var setup = setup(false);
        var changed = categoryData(true);
        changed.put("version", 0);
        assertEquals(400, request("PUT", "inventory/categories/" + setup.category(), changed).status());
    }

    @Test
    void assetsRequireSerialNumbersAndCreateOnlyOneOpeningMovement() throws Exception {
        var setup = setup(false);
        var data = assetData(setup);
        data.remove("serialNumber");
        assertEquals(400, request("POST", "inventory/assets", data).status());
        data.put("serialNumber", unique());
        var asset = create("inventory/assets", data);
        long id = asset.get("id").asLong();
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_stock_movement where asset_id = ?", Long.class, id));
        assertEquals("1.0000", jdbc.queryForObject("select quantity from erp_stock_movement where asset_id = ?", java.math.BigDecimal.class, id).toPlainString());
        var duplicate = new HashMap<>(data);
        duplicate.put("assetCode", unique());
        assertEquals(409, request("POST", "inventory/assets", duplicate).status());
        data.put("version", 0); data.put("condition", "GOOD");
        var updated = request("PUT", "inventory/assets/" + id, data);
        assertEquals(200, updated.status(), updated.raw());
        assertEquals(409, request("PUT", "inventory/assets/" + id, data).status());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_stock_movement where asset_id = ?", Long.class, id));
        data.put("version", updated.body().get("version").asLong()); data.put("assetCode", unique());
        assertEquals(400, request("PUT", "inventory/assets/" + id, data).status());
        assertEquals(400, request("DELETE", "inventory/assets/" + id + "?version=" + updated.body().get("version").asLong(), null).status());
    }

    @Test
    void lotOpeningPersistsExactQuantitiesBalanceAndMovementAtomically() throws Exception {
        var setup = setup(true);
        var data = lotData(setup);
        var lot = create("inventory/lots", data);
        long id = lot.get("id").asLong();
        assertEquals("10.125", lot.get("availableQuantity").asText());
        assertEquals("10.1250", jdbc.queryForObject("select available from erp_stock_balance where lot_id = ?", java.math.BigDecimal.class, id).toPlainString());
        assertEquals("10.1250", jdbc.queryForObject("select quantity from erp_stock_movement where lot_id = ?", java.math.BigDecimal.class, id).toPlainString());
        assertEquals(409, request("POST", "inventory/lots", data).status());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_stock_balance where location_id = ?", Long.class, setup.location()));
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_stock_movement where location_id = ?", Long.class, setup.location()));
        data.put("availableQuantity", "999");
        assertEquals(400, request("POST", "inventory/lots", data).status());
        data.remove("availableQuantity"); data.put("version", 0); data.put("initialQuantity", "11");
        assertEquals(400, request("PUT", "inventory/lots/" + id, data).status());
        assertEquals(1, request("GET", "inventory/balances?organizationId=" + setup.organization(), null).body().get("totalElements").asInt());
    }

    @Test
    void wrongTrackingModeAndInvalidQuantitiesLeaveNoStockBehind() throws Exception {
        var serialized = setup(false);
        assertEquals(400, request("POST", "inventory/lots", lotData(serialized)).status());
        var lotSetup = setup(true);
        assertEquals(400, request("POST", "inventory/assets", assetData(lotSetup)).status());
        for (String quantity : List.of("0", "-1", "0.00001", "1000000000000000", "invalid")) {
            var data = lotData(lotSetup); data.put("initialQuantity", quantity);
            assertEquals(400, request("POST", "inventory/lots", data).status(), quantity);
        }
        var expired = lotData(lotSetup);
        expired.put("validUntil", "2000-01-01");
        assertEquals(400, request("POST", "inventory/lots", expired).status());
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_stock_movement where location_id = ?", Long.class, lotSetup.location()));
    }

    @Test
    void modelCharacteristicsValidateTypeAndBlockStockUntilRequiredValuesExist() throws Exception {
        var setup = setup(true);
        long characteristic = create("inventory/characteristics", Map.of("name", unique(), "dataType", "DECIMAL", "unitOfMeasure", "mm")).get("id").asLong();
        create("inventory/category-characteristics", Map.of("categoryId", setup.category(), "characteristicId", characteristic, "requiredValue", true, "perItem", false));
        assertEquals(400, request("POST", "inventory/lots", lotData(setup)).status());
        var valueData = new HashMap<String, Object>(Map.of("modelId", setup.model(), "characteristicId", characteristic, "value", "invalid"));
        assertEquals(400, request("POST", "inventory/model-values", valueData).status());
        valueData.put("value", "9.50");
        var value = create("inventory/model-values", valueData);
        create("inventory/lots", lotData(setup));
        assertEquals(400, request("DELETE", "inventory/model-values/" + value.get("id").asLong() + "?version=0", null).status());
        assertEquals(400, request("PUT", "inventory/characteristics/" + characteristic, Map.of("name", "Changed", "dataType", "BOOLEAN", "version", 0)).status());
    }

    @Test
    void perItemCharacteristicsUseDraftBeforeMakingAssetAvailable() throws Exception {
        var setup = setup(false);
        long characteristic = create("inventory/characteristics", Map.of("name", unique(), "dataType", "BOOLEAN")).get("id").asLong();
        create("inventory/category-characteristics", Map.of("categoryId", setup.category(), "characteristicId", characteristic, "requiredValue", true, "perItem", true));
        var data = assetData(setup);
        assertEquals(400, request("POST", "inventory/assets", data).status());
        data.put("status", "DRAFT");
        var asset = create("inventory/assets", data);
        var value = create("inventory/item-values", Map.of("assetId", asset.get("id").asLong(), "characteristicId", characteristic, "value", "true"));
        data.put("status", "AVAILABLE"); data.put("version", 0);
        var updated = request("PUT", "inventory/assets/" + asset.get("id").asLong(), data);
        assertEquals(200, updated.status(), updated.raw());
        assertEquals(400, request("DELETE", "inventory/item-values/" + value.get("id").asLong() + "?version=0", null).status());
    }

    @Test
    void stockLocationsEnforceOrganizationAndProtectCoreUnitChanges() throws Exception {
        long org = organization();
        long other = organization();
        var unitData = new HashMap<String, Object>(Map.of("organizationId", org, "name", unique(), "code", unique(), "type", "Office"));
        var unit = create("core/units", unitData);
        var locationData = new HashMap<String, Object>(Map.of("organizationId", other, "unitId", unit.get("id").asLong(), "name", unique(), "type", "Warehouse", "controlled", true));
        assertEquals(400, request("POST", "inventory/locations", locationData).status());
        locationData.put("organizationId", org);
        var location = create("inventory/locations", locationData);
        unitData.put("version", 0); unitData.put("organizationId", other);
        assertEquals(400, request("PUT", "core/units/" + unit.get("id").asLong(), unitData).status());
        assertEquals(409, request("DELETE", "core/units/" + unit.get("id").asLong() + "?version=0", null).status());
        locationData.put("version", 0); locationData.put("organizationId", other); locationData.remove("unitId");
        assertEquals(400, request("PUT", "inventory/locations/" + location.get("id").asLong(), locationData).status());
    }

    @Test
    void firearmSpecificationsRequireOneFirearmModelAndValidTechnicalValues() throws Exception {
        long firearmCategory = create("inventory/categories", Map.of("name", unique(), "family", "FIREARM",
            "serialized", true, "lotControlled", false, "consumable", false)).get("id").asLong();
        long generalCategory = create("inventory/categories", categoryData(false)).get("id").asLong();
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Test manufacturer")).get("id").asLong();
        long firearmModel = create("inventory/models", Map.of("name", "Service pistol", "categoryId", firearmCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "2500")).get("id").asLong();
        long generalModel = create("inventory/models", Map.of("name", "General item", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "10")).get("id").asLong();
        var data = new HashMap<String, Object>(Map.of("modelId", generalModel, "caliber", "9x19 mm",
            "operatingMechanism", "SEMI_AUTOMATIC", "capacity", 17, "barrelLength", "102.5000"));
        assertEquals(400, request("POST", "inventory/firearm-specifications", data).status());
        data.put("modelId", firearmModel); data.put("capacity", "1.5");
        assertEquals(400, request("POST", "inventory/firearm-specifications", data).status());
        data.put("capacity", 0);
        assertEquals(400, request("POST", "inventory/firearm-specifications", data).status());
        data.put("capacity", 17); data.put("barrelLength", "0");
        assertEquals(400, request("POST", "inventory/firearm-specifications", data).status());
        data.put("barrelLength", "102.5000");
        var specification = create("inventory/firearm-specifications", data);
        assertEquals("Service pistol / 9x19 mm (#" + specification.get("id").asLong() + ")", specification.get("label").asText());
        assertEquals(17, specification.get("capacity").asInt());
        assertEquals("102.5", specification.get("barrelLength").asText());
        assertEquals(409, request("POST", "inventory/firearm-specifications", data).status());
        data.put("version", 0); data.put("caliber", ".40 S&W"); data.put("operatingMechanism", "REVOLVER");
        var updated = request("PUT", "inventory/firearm-specifications/" + specification.get("id").asLong(), data);
        assertEquals(200, updated.status(), updated.raw());
        assertEquals(".40 S&W", updated.body().get("caliber").asText());
        var model = request("GET", "inventory/models/" + firearmModel, null).body();
        assertEquals(400, request("PUT", "inventory/models/" + firearmModel, Map.of("name", "Changed", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", model.get("sku").asText(), "listPrice", "2500", "version", 0)).status());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'inventory/firearm-specifications' and record_id = ? and action = 'CREATE'", Long.class, specification.get("id").asLong()));
        assertEquals(204, request("DELETE", "inventory/firearm-specifications/" + specification.get("id").asLong() + "?version=" + updated.body().get("version").asLong(), null).status());
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_firearm_specification where id = ?", Long.class, specification.get("id").asLong()));
    }

    @Test
    void ammunitionSpecificationsRequireOneAmmunitionModelAndProtectItsClassification() throws Exception {
        long ammunitionCategory = create("inventory/categories", Map.of("name", unique(), "family", "AMMUNITION",
            "serialized", false, "lotControlled", true, "consumable", true)).get("id").asLong();
        long generalCategory = create("inventory/categories", categoryData(false)).get("id").asLong();
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Test manufacturer")).get("id").asLong();
        long ammunitionModel = create("inventory/models", Map.of("name", "Duty cartridge", "categoryId", ammunitionCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "2")).get("id").asLong();
        long generalModel = create("inventory/models", Map.of("name", "General item", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "2")).get("id").asLong();
        var data = new HashMap<String, Object>(Map.of("modelId", generalModel, "caliber", "12 GA",
            "lethalityClassification", "LESS_LETHAL", "projectileType", "Kinetic impact projectile", "caseType", "Polymer", "primerType", "Boxer"));
        assertEquals(400, request("POST", "inventory/ammunition-specifications", data).status());
        data.put("modelId", ammunitionModel);
        var specification = create("inventory/ammunition-specifications", data);
        assertEquals("Duty cartridge / 12 GA (#" + specification.get("id").asLong() + ")", specification.get("label").asText());
        assertEquals(409, request("POST", "inventory/ammunition-specifications", data).status());
        data.put("version", 0); data.put("projectileType", "Hollow point");
        var updated = request("PUT", "inventory/ammunition-specifications/" + specification.get("id").asLong(), data);
        assertEquals(200, updated.status(), updated.raw()); assertEquals("Hollow point", updated.body().get("projectileType").asText());
        var model = request("GET", "inventory/models/" + ammunitionModel, null).body();
        assertEquals(400, request("PUT", "inventory/models/" + ammunitionModel, Map.of("name", "Changed", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", model.get("sku").asText(), "listPrice", "2", "version", 0)).status());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'inventory/ammunition-specifications' and record_id = ? and action = 'CREATE'", Long.class, specification.get("id").asLong()));
        assertEquals(204, request("DELETE", "inventory/ammunition-specifications/" + specification.get("id").asLong() + "?version=" + updated.body().get("version").asLong(), null).status());
    }

    @Test
    void grenadeSpecificationsValidateFamilyDelayRadiusAndUniqueness() throws Exception {
        long grenadeCategory = create("inventory/categories", Map.of("name", unique(), "family", "GRENADE",
            "serialized", true, "lotControlled", false, "consumable", false)).get("id").asLong();
        long generalCategory = create("inventory/categories", categoryData(false)).get("id").asLong();
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Test manufacturer")).get("id").asLong();
        long grenadeModel = create("inventory/models", Map.of("name", "Training grenade", "categoryId", grenadeCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "100")).get("id").asLong();
        long generalModel = create("inventory/models", Map.of("name", "General item", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "10")).get("id").asLong();
        var data = new HashMap<String, Object>(Map.of("modelId", generalModel, "grenadeType", "TRAINING",
            "agent", "INERT", "delaySeconds", 4, "safetyRadius", "15.5000"));
        assertEquals(400, request("POST", "inventory/grenade-specifications", data).status());
        data.put("modelId", grenadeModel); data.put("delaySeconds", 0);
        assertEquals(400, request("POST", "inventory/grenade-specifications", data).status());
        data.put("delaySeconds", 4); data.put("safetyRadius", "0");
        assertEquals(400, request("POST", "inventory/grenade-specifications", data).status());
        data.put("safetyRadius", "15.5000");
        var specification = create("inventory/grenade-specifications", data);
        assertEquals("Training grenade / TRAINING (#" + specification.get("id").asLong() + ")", specification.get("label").asText());
        assertEquals(409, request("POST", "inventory/grenade-specifications", data).status());
        data.put("version", 0); data.put("agent", "CS");
        var updated = request("PUT", "inventory/grenade-specifications/" + specification.get("id").asLong(), data);
        assertEquals(200, updated.status(), updated.raw()); assertEquals("CS", updated.body().get("agent").asText());
        var model = request("GET", "inventory/models/" + grenadeModel, null).body();
        assertEquals(400, request("PUT", "inventory/models/" + grenadeModel, Map.of("name", "Changed", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", model.get("sku").asText(), "listPrice", "100", "version", 0)).status());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'inventory/grenade-specifications' and record_id = ? and action = 'CREATE'", Long.class, specification.get("id").asLong()));
        assertEquals(204, request("DELETE", "inventory/grenade-specifications/" + specification.get("id").asLong() + "?version=" + updated.body().get("version").asLong(), null).status());
    }

    @Test
    void spraySpecificationsValidateFamilyAndPhysicalValues() throws Exception {
        long sprayCategory = create("inventory/categories", Map.of("name", unique(), "family", "SPRAY",
            "serialized", true, "lotControlled", false, "consumable", false)).get("id").asLong();
        long generalCategory = create("inventory/categories", categoryData(false)).get("id").asLong();
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Test manufacturer")).get("id").asLong();
        long sprayModel = create("inventory/models", Map.of("name", "OC spray", "categoryId", sprayCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "80")).get("id").asLong();
        long generalModel = create("inventory/models", Map.of("name", "General item", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "10")).get("id").asLong();
        var data = new HashMap<String, Object>(Map.of("modelId", generalModel, "agent", "OC",
            "concentration", "10", "volumeMl", "50", "rangeMeters", "4.5"));
        assertEquals(400, request("POST", "inventory/spray-specifications", data).status());
        data.put("modelId", sprayModel); data.put("concentration", "101");
        assertEquals(400, request("POST", "inventory/spray-specifications", data).status());
        data.put("concentration", "10"); data.put("volumeMl", "0");
        assertEquals(400, request("POST", "inventory/spray-specifications", data).status());
        data.put("volumeMl", "50"); data.put("rangeMeters", "0");
        assertEquals(400, request("POST", "inventory/spray-specifications", data).status());
        data.put("rangeMeters", "4.5");
        var specification = create("inventory/spray-specifications", data);
        assertEquals("OC spray / OC (#" + specification.get("id").asLong() + ")", specification.get("label").asText());
        assertEquals(409, request("POST", "inventory/spray-specifications", data).status());
        data.put("version", 0); data.put("volumeMl", "60");
        var updated = request("PUT", "inventory/spray-specifications/" + specification.get("id").asLong(), data);
        assertEquals(200, updated.status(), updated.raw()); assertEquals("60", updated.body().get("volumeMl").asText());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'inventory/spray-specifications' and record_id = ? and action = 'CREATE'", Long.class, specification.get("id").asLong()));
        assertEquals(204, request("DELETE", "inventory/spray-specifications/" + specification.get("id").asLong() + "?version=" + updated.body().get("version").asLong(), null).status());
    }

    @Test
    void ballisticProtectionSpecificationsValidateFamilyAndRemainAuditable() throws Exception {
        long ballisticCategory = create("inventory/categories", Map.of("name", unique(), "family", "BALLISTIC_PROTECTION",
            "serialized", true, "lotControlled", false, "consumable", false)).get("id").asLong();
        long generalCategory = create("inventory/categories", categoryData(false)).get("id").asLong();
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Test manufacturer")).get("id").asLong();
        long ballisticModel = create("inventory/models", Map.of("name", "Ballistic vest", "categoryId", ballisticCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "1500")).get("id").asLong();
        long generalModel = create("inventory/models", Map.of("name", "General item", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "10")).get("id").asLong();
        var data = new HashMap<String, Object>(Map.of("modelId", generalModel, "protectionType", "VEST",
            "protectionLevel", "LEVEL III-A", "material", "Aramid", "certification", "CERT-001"));
        assertEquals(400, request("POST", "inventory/ballistic-protection-specifications", data).status());
        data.put("modelId", ballisticModel);
        var specification = create("inventory/ballistic-protection-specifications", data);
        assertEquals("Ballistic vest / LEVEL III-A (#" + specification.get("id").asLong() + ")", specification.get("label").asText());
        assertEquals(409, request("POST", "inventory/ballistic-protection-specifications", data).status());
        data.put("version", 0); data.put("certification", "CERT-002");
        var updated = request("PUT", "inventory/ballistic-protection-specifications/" + specification.get("id").asLong(), data);
        assertEquals(200, updated.status(), updated.raw()); assertEquals("CERT-002", updated.body().get("certification").asText());
        var model = request("GET", "inventory/models/" + ballisticModel, null).body();
        assertEquals(400, request("PUT", "inventory/models/" + ballisticModel, Map.of("name", "Changed", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", model.get("sku").asText(), "listPrice", "1500", "version", 0)).status());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'inventory/ballistic-protection-specifications' and record_id = ? and action = 'CREATE'", Long.class, specification.get("id").asLong()));
        assertEquals(204, request("DELETE", "inventory/ballistic-protection-specifications/" + specification.get("id").asLong() + "?version=" + updated.body().get("version").asLong(), null).status());
    }

    @Test
    void electricalDeviceSpecificationsValidateFamilyVoltageAndCycles() throws Exception {
        long deviceCategory = create("inventory/categories", Map.of("name", unique(), "family", "ELECTRICAL_DEVICE",
            "serialized", true, "lotControlled", false, "consumable", false)).get("id").asLong();
        long generalCategory = create("inventory/categories", categoryData(false)).get("id").asLong();
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Test manufacturer")).get("id").asLong();
        long deviceModel = create("inventory/models", Map.of("name", "Electrical incapacitation device", "categoryId", deviceCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "3000")).get("id").asLong();
        long generalModel = create("inventory/models", Map.of("name", "General item", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "10")).get("id").asLong();
        var data = new HashMap<String, Object>(Map.of("modelId", generalModel, "voltage", "50000", "cycles", 5, "cartridgeType", "STANDARD"));
        assertEquals(400, request("POST", "inventory/electrical-device-specifications", data).status());
        data.put("modelId", deviceModel); data.put("voltage", "0");
        assertEquals(400, request("POST", "inventory/electrical-device-specifications", data).status());
        data.put("voltage", "50000"); data.put("cycles", 0);
        assertEquals(400, request("POST", "inventory/electrical-device-specifications", data).status());
        data.put("cycles", 5);
        var specification = create("inventory/electrical-device-specifications", data);
        assertEquals("Electrical incapacitation device / STANDARD (#" + specification.get("id").asLong() + ")", specification.get("label").asText());
        assertEquals(409, request("POST", "inventory/electrical-device-specifications", data).status());
        data.put("version", 0); data.put("cartridgeType", "EXTENDED_RANGE");
        var updated = request("PUT", "inventory/electrical-device-specifications/" + specification.get("id").asLong(), data);
        assertEquals(200, updated.status(), updated.raw()); assertEquals("EXTENDED_RANGE", updated.body().get("cartridgeType").asText());
        var model = request("GET", "inventory/models/" + deviceModel, null).body();
        assertEquals(400, request("PUT", "inventory/models/" + deviceModel, Map.of("name", "Changed", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", model.get("sku").asText(), "listPrice", "3000", "version", 0)).status());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'inventory/electrical-device-specifications' and record_id = ? and action = 'CREATE'", Long.class, specification.get("id").asLong()));
        assertEquals(204, request("DELETE", "inventory/electrical-device-specifications/" + specification.get("id").asLong() + "?version=" + updated.body().get("version").asLong(), null).status());
    }

    @Test
    void opticalSpecificationsValidateFamilyMagnificationAndCapabilities() throws Exception {
        long opticalCategory = create("inventory/categories", Map.of("name", unique(), "family", "OPTICAL",
            "serialized", true, "lotControlled", false, "consumable", false)).get("id").asLong();
        long generalCategory = create("inventory/categories", categoryData(false)).get("id").asLong();
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Test manufacturer")).get("id").asLong();
        long opticalModel = create("inventory/models", Map.of("name", "Thermal sight", "categoryId", opticalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "5000")).get("id").asLong();
        long generalModel = create("inventory/models", Map.of("name", "General item", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "10")).get("id").asLong();
        var data = new HashMap<String, Object>(Map.of("modelId", generalModel, "opticalType", "WEAPON_SIGHT",
            "maximumMagnification", "8", "nightVision", false, "thermalVision", true));
        assertEquals(400, request("POST", "inventory/optical-specifications", data).status());
        data.put("modelId", opticalModel); data.put("maximumMagnification", "0");
        assertEquals(400, request("POST", "inventory/optical-specifications", data).status());
        data.put("maximumMagnification", "8");
        var specification = create("inventory/optical-specifications", data);
        assertEquals("Thermal sight / WEAPON_SIGHT (#" + specification.get("id").asLong() + ")", specification.get("label").asText());
        assertTrue(specification.get("thermalVision").asBoolean()); assertFalse(specification.get("nightVision").asBoolean());
        assertEquals(409, request("POST", "inventory/optical-specifications", data).status());
        data.put("version", 0); data.put("nightVision", true);
        var updated = request("PUT", "inventory/optical-specifications/" + specification.get("id").asLong(), data);
        assertEquals(200, updated.status(), updated.raw()); assertTrue(updated.body().get("nightVision").asBoolean());
        var model = request("GET", "inventory/models/" + opticalModel, null).body();
        assertEquals(400, request("PUT", "inventory/models/" + opticalModel, Map.of("name", "Changed", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", model.get("sku").asText(), "listPrice", "5000", "version", 0)).status());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'inventory/optical-specifications' and record_id = ? and action = 'CREATE'", Long.class, specification.get("id").asLong()));
        assertEquals(204, request("DELETE", "inventory/optical-specifications/" + specification.get("id").asLong() + "?version=" + updated.body().get("version").asLong(), null).status());
    }

    @Test
    void regulatoryControlsValidateFirearmStatusValidityAndUniqueExternalIdentity() throws Exception {
        long firearmCategory = create("inventory/categories", Map.of("name", unique(), "family", "FIREARM",
            "serialized", true, "lotControlled", false, "consumable", false)).get("id").asLong();
        long generalCategory = create("inventory/categories", categoryData(false)).get("id").asLong();
        long brand = create("inventory/brands", Map.of("name", unique(), "manufacturer", "Test manufacturer")).get("id").asLong();
        long firearmModel = create("inventory/models", Map.of("name", "Registered pistol", "categoryId", firearmCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "2500")).get("id").asLong();
        long generalModel = create("inventory/models", Map.of("name", "General asset", "categoryId", generalCategory,
            "brandId", brand, "unitOfMeasure", "EA", "sku", unique(), "listPrice", "10")).get("id").asLong();
        long org = organization();
        long location = create("inventory/locations", Map.of("organizationId", org, "name", unique(), "type", "Armory", "controlled", true)).get("id").asLong();
        long firearm = create("inventory/assets", Map.of("modelId", firearmModel, "locationId", location, "assetCode", unique(),
            "serialNumber", unique(), "condition", "NEW", "status", "AVAILABLE", "currentValue", "2500")).get("id").asLong();
        long otherFirearm = create("inventory/assets", Map.of("modelId", firearmModel, "locationId", location, "assetCode", unique(),
            "serialNumber", unique(), "condition", "GOOD", "status", "AVAILABLE", "currentValue", "2400")).get("id").asLong();
        long generalAsset = create("inventory/assets", Map.of("modelId", generalModel, "locationId", location, "assetCode", unique(),
            "serialNumber", unique(), "condition", "NEW", "status", "AVAILABLE", "currentValue", "10")).get("id").asLong();
        var data = new HashMap<String, Object>(Map.of("assetId", generalAsset, "externalSystem", "NATIONAL_REGISTRY",
            "registrationNumber", "REG-001", "status", "ACTIVE", "validUntil", "2030-12-31"));
        assertEquals(400, request("POST", "inventory/regulatory-controls", data).status());
        data.put("assetId", firearm); data.put("validUntil", "2000-01-01");
        assertEquals(400, request("POST", "inventory/regulatory-controls", data).status());
        data.put("validUntil", "2030-12-31");
        var control = create("inventory/regulatory-controls", data);
        assertEquals("REG-001", control.get("registrationNumber").asText());
        assertEquals(1, request("GET", "inventory/regulatory-controls?organizationId=" + org, null).body().get("totalElements").asInt());
        assertEquals(409, request("POST", "inventory/regulatory-controls", data).status());
        data.put("assetId", otherFirearm);
        assertEquals(409, request("POST", "inventory/regulatory-controls", data).status());
        data.put("registrationNumber", "REG-002");
        var second = create("inventory/regulatory-controls", data);
        var update = new HashMap<String, Object>(Map.of("assetId", firearm, "externalSystem", "OTHER_SYSTEM",
            "registrationNumber", "REG-001-A", "status", "SUSPENDED", "version", control.get("version").asLong()));
        assertEquals(400, request("PUT", "inventory/regulatory-controls/" + control.get("id").asLong(), update).status());
        update.put("externalSystem", "NATIONAL_REGISTRY");
        jdbc.update("update erp_asset_item set status = 'SOLD' where id = ?", firearm);
        assertEquals(400, request("PUT", "inventory/regulatory-controls/" + control.get("id").asLong(), update).status());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'inventory/regulatory-controls' and record_id = ? and action = 'CREATE'", Long.class, control.get("id").asLong()));
        assertEquals(204, request("DELETE", "inventory/regulatory-controls/" + second.get("id").asLong() + "?version=" + second.get("version").asLong(), null).status());
    }
}
