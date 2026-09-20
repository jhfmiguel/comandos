package com.weaponsregistration.security.controller;

import com.weaponsregistration.core.model.*;
import com.weaponsregistration.inventory.model.*;
import com.weaponsregistration.sales.model.InventorySale;
import com.weaponsregistration.model.PaymentMethod;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:authorization-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "erp.security.require-login=true", "erp.security.enforce-permissions=true", "logging.level.root=WARN", "debug=false"
})
class AuthorizationApiTests {
    @LocalServerPort int port;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    private static final String PASSWORD = "Permission-test-password";
    record Fixture(long user, String login, long person, long organization, long otherOrganization, long unit,
        long location, long otherLocation, long model, long asset, long otherAsset, long otherSale) {}
    record Result(int status, JsonNode body, String raw) {}

    private <T> T persist(T entity) { em.persist(entity); return entity; }
    private Fixture fixture() {
        return tx.execute(status -> {
            var person = new Person(); person.fullName = "Buyer"; person.personType = "INDIVIDUAL"; persist(person);
            var user = new SystemUser(); user.person = person; user.login = UUID.randomUUID().toString();
            user.passwordHash = new BCryptPasswordEncoder(12).encode(PASSWORD); persist(user);
            var org = organization(); var other = organization();
            var unit = new OrganizationalUnit(); unit.organization = org; unit.code = "MAIN"; unit.name = "Main unit"; unit.type = "Office"; persist(unit);
            var location = location(org, unit); var otherLocation = location(other, null);
            var category = new ItemCategory(); category.name = "Equipment"; category.family = "FIREARM"; category.serialized = true; persist(category);
            var brand = new Brand(); brand.name = UUID.randomUUID().toString(); brand.manufacturer = "Factory"; persist(brand);
            var model = new ItemModel(); model.name = "Test model"; model.category = category; model.brand = brand; model.sku = UUID.randomUUID().toString();
            model.unitOfMeasure = "EA"; model.listPrice = new BigDecimal("10.0000"); persist(model);
            var asset = asset(model, location); var otherAsset = asset(model, otherLocation);
            asset(model, location(org, null));
            var sale = new InventorySale(); sale.organization = other; sale.buyer = person; sale.organizationName = other.name; sale.buyerName = person.fullName;
            sale.paymentMethod = PaymentMethod.PIX; sale.finalizedAt = LocalDateTime.now(); sale.total = BigDecimal.ZERO;
            sale.requestId = UUID.randomUUID().toString(); sale.requestFingerprint = "fixture"; persist(sale);
            return new Fixture(user.id, user.login, person.id, org.id, other.id, unit.id, location.id, otherLocation.id, model.id, asset.id, otherAsset.id, sale.id);
        });
    }
    private Organization organization() {
        var org = new Organization(); org.name = UUID.randomUUID().toString(); org.nature = "Company"; return persist(org);
    }
    private StockLocation location(Organization org, OrganizationalUnit unit) {
        var location = new StockLocation(); location.organization = org; location.unit = unit; location.name = UUID.randomUUID().toString();
        location.type = "Warehouse"; return persist(location);
    }
    private AssetItem asset(ItemModel model, StockLocation location) {
        var asset = new AssetItem(); asset.model = model; asset.location = location; asset.assetCode = UUID.randomUUID().toString();
        asset.serialNumber = UUID.randomUUID().toString(); asset.condition = "NEW"; asset.status = "AVAILABLE"; return persist(asset);
    }
    private long grant(Fixture fixture, String resource, String action, String level, Long unit) {
        return tx.execute(status -> {
            var permission = em.createQuery("select p from Permission p where p.resource = :resource and p.action = :action", Permission.class)
                .setParameter("resource", resource).setParameter("action", action).getResultStream().findFirst().orElseGet(() -> {
                    var p = new Permission(); p.resource = resource; p.action = action; return persist(p);
                });
            var profile = new AccessProfile(); profile.name = UUID.randomUUID().toString(); profile.level = level; persist(profile);
            var link = new ProfilePermission(); link.profile = profile; link.permission = permission; persist(link);
            var assignment = new UserProfile(); assignment.user = em.find(SystemUser.class, fixture.user()); assignment.profile = profile;
            assignment.organization = em.find(Organization.class, fixture.organization());
            if (unit != null) assignment.unit = em.find(OrganizationalUnit.class, unit);
            persist(assignment);
            return link.id;
        });
    }
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
    private void login(Fixture f) throws Exception {
        var result = request("POST", "/api/auth/login", "username=" + f.login() + "&password=" + PASSWORD);
        assertEquals(204, result.status(), result.raw());
    }

    @Test
    void ammunitionConsumptionRequiresCreateAndEnforcesUnitScopeOnStockAndRetries() throws Exception {
        var f = fixture();
        long balance = tx.execute(status -> {
            var category = new ItemCategory(); category.name = UUID.randomUUID().toString(); category.family = "AMMUNITION";
            category.serialized = false; category.lotControlled = true; category.consumable = true; persist(category);
            var model = new ItemModel(); model.category = category; model.brand = em.find(ItemModel.class, f.model()).brand;
            model.name = "Cartridge"; model.sku = UUID.randomUUID().toString(); model.unitOfMeasure = "EA"; persist(model);
            var lot = new StockLot(); lot.model = model; lot.openingLocation = em.find(StockLocation.class, f.location());
            lot.lotNumber = UUID.randomUUID().toString(); lot.initialQuantity = BigDecimal.TEN; lot.availableQuantity = BigDecimal.TEN; persist(lot);
            var stock = new StockBalance(); stock.lot = lot; stock.location = lot.openingLocation; stock.available = BigDecimal.TEN; persist(stock);
            return stock.id;
        });
        grant(f, "ammunition-consumptions", "READ", "UNIT", f.unit());
        grant(f, "core/people", "READ", "SYSTEM", null);
        login(f);
        String path = "/api/erp/ammunition-consumptions";
        var payload = new HashMap<String, Object>(Map.of("requestId", UUID.randomUUID().toString(),
            "organizationId", f.organization(), "unitId", f.unit(), "responsibleId", f.person(), "authorizerId", f.person(),
            "purpose", "Training", "items", List.of(Map.of("balanceId", balance, "quantity", "2", "result", "Consumed"))));
        assertEquals(403, request("POST", path, payload).status());
        assertEquals(403, request("GET", path + "/stock?organizationId=" + f.organization(), null).status());
        assertEquals(403, request("GET", path + "?organizationId=" + f.otherOrganization(), null).status());
        var stock = request("GET", path + "/stock?organizationId=" + f.organization() + "&unitId=" + f.unit(), null);
        assertEquals(200, stock.status(), stock.raw()); assertEquals(1, stock.body().get("totalElements").asInt());
        long createGrant = grant(f, "ammunition-consumptions", "CREATE", "UNIT", f.unit());
        payload.remove("unitId"); assertEquals(403, request("POST", path, payload).status());
        payload.put("unitId", f.unit());
        var created = request("POST", path, payload); assertEquals(200, created.status(), created.raw());
        assertEquals(200, request("GET", path + "/" + created.body().get("id").asLong(), null).status());
        jdbc.update("delete from erp_profile_permission where id = ?", createGrant);
        assertEquals(403, request("POST", path, payload).status());
        assertEquals(0, new BigDecimal("8").compareTo(jdbc.queryForObject("select available from erp_stock_balance where id = ?", BigDecimal.class, balance)));
    }

    @Test
    void reservationExpirationAndCancellationRequireTheirOwnActions() throws Exception {
        for (String action : List.of("EXPIRE", "CANCEL")) {
            var f = fixture();
            for (String grantAction : List.of("CREATE", "READ", action)) grant(f, "reservations", grantAction, "UNIT", f.unit());
            login(f);
            var created = request("POST", "/api/erp/reservations", Map.of("requestId", UUID.randomUUID().toString(),
                "organizationId", f.organization(), "unitId", f.unit(), "purpose", "Scoped reservation",
                "startsAt", LocalDateTime.now().toString(), "endsAt", LocalDateTime.now().plusDays(1).toString(),
                "items", List.of(Map.of("assetId", f.asset(), "quantity", 1))));
            assertEquals(200, created.status(), created.raw());
            long id = created.body().get("id").asLong();
            jdbc.update("update erp_inventory_reservation set ends_at=? where id=?", LocalDateTime.now().minusMinutes(1), id);
            String denied = action.equals("EXPIRE") ? "cancel" : "expire";
            assertEquals(403, request("POST", "/api/erp/reservations/" + id + "/" + denied, null).status());
            assertEquals("BLOCKED", jdbc.queryForObject("select status from erp_asset_item where id=?", String.class, f.asset()));
            var released = request("POST", "/api/erp/reservations/" + id + "/" + action.toLowerCase(Locale.ROOT), null);
            assertEquals(200, released.status(), released.raw());
            assertEquals(action.equals("EXPIRE") ? "EXPIRED" : "CANCELLED", released.body().get("statusCode").asText());
            assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id=?", String.class, f.asset()));
        }
    }

    @Test
    void authenticatedAccountWithoutGrantsCannotUseBusinessApis() throws Exception {
        var f = fixture(); login(f);
        for (String path : List.of("/api/erp/core/people", "/api/erp/inventory/assets", "/api/erp/sales?organizationId=" + f.organization(), "/api/users", "/api/weapons"))
            assertEquals(403, request("GET", path, null).status(), path);
        assertEquals(0, request("GET", "/api/erp/core/catalog", null).body().size());
        assertEquals(0, request("GET", "/api/erp/inventory/catalog", null).body().size());
    }

    @Test
    void organizationAndUnitScopesFilterRowsCountsSearchAndDirectIds() throws Exception {
        var f = fixture();
        long link = grant(f, "inventory/assets", "READ", "ORGANIZATION", null); login(f);
        var list = request("GET", "/api/erp/inventory/assets", null);
        assertEquals(200, list.status(), list.raw());
        assertEquals(2, list.body().get("totalElements").asInt());
        assertEquals(200, request("GET", "/api/erp/inventory/assets/" + f.asset(), null).status());
        assertEquals(403, request("GET", "/api/erp/inventory/assets/" + f.otherAsset(), null).status());
        assertEquals(0, request("GET", "/api/erp/inventory/assets?organizationId=" + f.otherOrganization(), null).body().get("totalElements").asInt());
        String otherCode = jdbc.queryForObject("select asset_code from erp_asset_item where id = ?", String.class, f.otherAsset());
        assertEquals(0, request("GET", "/api/erp/inventory/assets?search=" + otherCode, null).body().get("totalElements").asInt());
        jdbc.update("delete from erp_profile_permission where id = ?", link);
        grant(f, "inventory/assets", "READ", "UNIT", f.unit());
        assertEquals(1, request("GET", "/api/erp/inventory/assets", null).body().get("totalElements").asInt());
    }

    @Test
    void readPermissionDoesNotAllowWritesOrAccessAdministration() throws Exception {
        var f = fixture(); grant(f, "inventory/assets", "READ", "ORGANIZATION", null);
        grant(f, "core/users", "UPDATE", "SYSTEM", null);
        grant(f, "*", "*", "ORGANIZATION", null); login(f);
        assertEquals(403, request("POST", "/api/erp/core/users", Map.of()).status());
        assertEquals(403, request("GET", "/api/erp/core/permissions", null).status());
        assertEquals(403, request("GET", "/api/erp/core/people", null).status());
        // A separate read-only account cannot mutate stock even within its scope.
        var reader = fixture(); grant(reader, "inventory/assets", "READ", "SYSTEM", null); login(reader);
        assertEquals(403, request("POST", "/api/erp/inventory/assets", Map.of()).status());
        assertEquals(403, request("POST", "/api/erp/inventory/assets/batch", Map.of()).status());
        assertEquals(403, request("POST", "/api/erp/inventory/assets/batch/review", Map.of()).status());
        assertEquals(403, request("DELETE", "/api/erp/inventory/assets/" + reader.asset() + "?version=0", null).status());
        var catalog = request("GET", "/api/erp/inventory/catalog", null).body();
        assertEquals(List.of("READ"), json.convertValue(catalog.get(0).get("actions"), List.class));
    }

    @Test
    void assetBatchChecksScopeAndAuditsAuthenticatedActor() throws Exception {
        var f = fixture();
        grant(f, "inventory/assets", "CREATE", "ORGANIZATION", null);
        grant(f, "inventory/models", "READ", "SYSTEM", null);
        grant(f, "inventory/locations", "READ", "ORGANIZATION", null); login(f);
        var common = new HashMap<String, Object>(Map.of("modelId", f.model(), "locationId", f.otherLocation(),
            "condition", "NEW", "status", "AVAILABLE", "currentValue", "0"));
        String code = UUID.randomUUID().toString();
        var payload = Map.of("requestId", UUID.randomUUID().toString(), "common", common,
            "items", List.of(Map.of("assetCode", code, "serialNumber", UUID.randomUUID().toString())));
        for (String suffix : List.of("", "/review"))
            assertEquals(403, request("POST", "/api/erp/inventory/assets/batch" + suffix, payload).status());
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_asset_item where asset_code=?", Long.class, code));
        common.put("locationId", f.location());
        var created = request("POST", "/api/erp/inventory/assets/batch", payload);
        assertEquals(200, created.status(), created.raw());
        long id = created.body().get("recordIds").get(0).asLong();
        assertEquals(f.user(), jdbc.queryForObject("select actor_id from erp_audit_record where action='BATCH_CREATE' and record_id=?", Long.class, id));
        assertNotNull(jdbc.queryForObject("select occurred_at from erp_audit_record where action='BATCH_CREATE' and record_id=?", java.sql.Timestamp.class, id));
    }

    @Test
    void crossOrganizationReferencesAndNewRecordScopeAreRejectedAtomically() throws Exception {
        var f = fixture();
        grant(f, "inventory/assets", "CREATE", "ORGANIZATION", null);
        grant(f, "inventory/models", "READ", "SYSTEM", null);
        grant(f, "inventory/locations", "READ", "ORGANIZATION", null);
        grant(f, "core/organizations", "READ", "SYSTEM", null);
        grant(f, "inventory/locations", "CREATE", "ORGANIZATION", null); login(f);
        var data = new HashMap<String, Object>(Map.of("modelId", f.model(), "locationId", f.otherLocation(), "assetCode", UUID.randomUUID().toString(),
            "serialNumber", UUID.randomUUID().toString(), "condition", "NEW", "status", "AVAILABLE", "currentValue", "0"));
        assertEquals(403, request("POST", "/api/erp/inventory/assets", data).status());
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_asset_item where asset_code = ?", Long.class, data.get("assetCode")));
        data.put("locationId", f.location());
        var created = request("POST", "/api/erp/inventory/assets", data);
        assertEquals(201, created.status(), created.raw());
        long assetId = created.body().get("id").asLong();
        assertEquals(f.user(), jdbc.queryForObject("select actor_id from erp_audit_record where resource='inventory/assets' and action='CREATE' and record_id=?", Long.class, assetId));
        data.put("version", created.body().get("version").asLong()); data.put("condition", "GOOD");
        assertEquals(403, request("PUT", "/api/erp/inventory/assets/" + assetId, data).status());
        grant(f, "inventory/assets", "UPDATE", "ORGANIZATION", null);
        assertEquals(403, request("PUT", "/api/erp/inventory/assets/" + f.otherAsset(), data).status());
        var edited = request("PUT", "/api/erp/inventory/assets/" + assetId, data);
        assertEquals(200, edited.status(), edited.raw());
        assertEquals(f.user(), jdbc.queryForObject("select actor_id from erp_audit_record where resource='inventory/assets' and action='UPDATE' and record_id=?", Long.class, assetId));
        assertEquals(2L, jdbc.queryForObject("select count(*) from erp_audit_record where resource='inventory/assets' and record_id=?", Long.class, assetId));
        assertEquals(403, request("POST", "/api/erp/inventory/locations", Map.of("organizationId", f.otherOrganization(), "name", "Forbidden location", "type", "Warehouse", "controlled", false)).status());
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_stock_location where name = 'Forbidden location'", Long.class));
    }

    @Test
    void permissionRevocationAndInactiveOrganizationsApplyWithoutAnotherLogin() throws Exception {
        var f = fixture(); long link = grant(f, "inventory/assets", "READ", "ORGANIZATION", null); login(f);
        assertEquals(200, request("GET", "/api/erp/inventory/assets", null).status());
        jdbc.update("delete from erp_profile_permission where id = ?", link);
        assertEquals(403, request("GET", "/api/erp/inventory/assets", null).status());
        grant(f, "inventory/assets", "READ", "ORGANIZATION", null);
        jdbc.update("update erp_organization set active = false where id = ?", f.organization());
        assertEquals(403, request("GET", "/api/erp/inventory/assets", null).status());
        assertFalse(request("GET", "/api/auth/session", null).body().get("user").isNull());
    }

    @Test
    void updatesCheckExistingAndProposedScopeAndCannotMoveAnAssignmentAcrossOrganizations() throws Exception {
        var f = fixture();
        long assignmentId = tx.execute(status -> {
            var role = new PersonRole(); role.code = UUID.randomUUID().toString(); role.name = "Operator"; persist(role);
            var assignment = new PersonRoleAssignment(); assignment.person = em.find(Person.class, f.person()); assignment.role = role;
            assignment.organization = em.find(Organization.class, f.organization()); assignment.startDate = java.time.LocalDate.now();
            assignment.status = "ACTIVE"; persist(assignment); return assignment.id;
        });
        grant(f, "core/person-roles", "READ", "ORGANIZATION", null);
        grant(f, "core/person-roles", "UPDATE", "ORGANIZATION", null);
        for (String resource : List.of("core/people", "core/roles", "core/organizations")) grant(f, resource, "READ", "SYSTEM", null);
        login(f);
        var existing = request("GET", "/api/erp/core/person-roles/" + assignmentId, null).body();
        var data = new HashMap<String, Object>(Map.of("personId", f.person(), "roleId", existing.get("roleId").asLong(),
            "organizationId", f.otherOrganization(), "startDate", existing.get("startDate").asText(), "status", "ACTIVE", "version", 0));
        assertEquals(403, request("PUT", "/api/erp/core/person-roles/" + assignmentId, data).status());
        assertEquals(f.organization(), jdbc.queryForObject("select organization_id from erp_person_role_assignment where id = ?", Long.class, assignmentId));
        data.put("organizationId", f.organization());
        assertEquals(200, request("PUT", "/api/erp/core/person-roles/" + assignmentId, data).status());
        jdbc.update("update erp_person_role_assignment set organization_id = ? where id = ?", f.otherOrganization(), assignmentId);
        assertEquals(403, request("PUT", "/api/erp/core/person-roles/" + assignmentId, data).status());
    }

    @Test
    void salesAndLegacyRoutesRespectTheirExplicitPermissions() throws Exception {
        var f = fixture(); grant(f, "sales", "READ", "ORGANIZATION", null); grant(f, "sales", "CREATE", "ORGANIZATION", null);
        grant(f, "core/people", "READ", "SYSTEM", null); login(f);
        assertEquals(200, request("GET", "/api/erp/sales/stock?organizationId=" + f.organization() + "&kind=ASSET", null).status());
        assertEquals(403, request("GET", "/api/erp/sales/stock?organizationId=" + f.otherOrganization() + "&kind=ASSET", null).status());
        assertEquals(403, request("GET", "/api/erp/sales/" + f.otherSale(), null).status());
        var sale = new HashMap<String, Object>(Map.of("requestId", UUID.randomUUID().toString(), "organizationId", f.otherOrganization(),
            "buyerId", f.person(), "paymentMethod", "PIX", "items", List.of(Map.of("assetId", f.otherAsset(), "quantity", "1", "expectedUnitPrice", "10"))));
        assertEquals(403, request("POST", "/api/erp/sales", sale).status());
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, f.otherAsset()));
        sale.put("organizationId", f.organization()); sale.put("items", List.of(Map.of("assetId", f.asset(), "quantity", "1", "expectedUnitPrice", "10")));
        var result = request("POST", "/api/erp/sales", sale); assertEquals(200, result.status(), result.raw());
        assertEquals(f.user(), result.body().get("finalizedById").asLong());
        assertEquals(f.login(), result.body().get("finalizedByLogin").asText());
        assertEquals(f.user(), jdbc.queryForObject("select actor_id from erp_audit_record where resource = 'sales' and record_id = ?", Long.class, result.body().get("id").asLong()));
        assertEquals(403, request("GET", "/api/users", null).status());
        grant(f, "legacy/users", "READ", "SYSTEM", null);
        assertEquals(200, request("GET", "/api/users", null).status());
    }

    @Test
    void systemAdministratorCanManageAccessAndInvalidScopesGrantNothing() throws Exception {
        var f = fixture(); grant(f, "*", "*", "SYSTEM", null); login(f);
        var result = request("GET", "/api/erp/core/catalog", null);
        assertEquals(200, result.status(), result.raw()); assertEquals(14, result.body().size());
        assertEquals(201, request("POST", "/api/erp/core/profiles", Map.of("name", UUID.randomUUID().toString(), "level", "SYSTEM")).status());
        var invalid = fixture();
        grant(invalid, "*", "*", "SYSTEM", invalid.unit());
        grant(invalid, "*", "*", "Operator", null);
        grant(invalid, "sales", "READ", "UNIT", invalid.unit()); login(invalid);
        assertEquals(403, request("GET", "/api/erp/core/users", null).status());
        assertEquals(403, request("GET", "/api/erp/sales?organizationId=" + invalid.organization(), null).status());
    }

    @Test
    void auditRequiresAnExplicitGlobalReadGrant() throws Exception {
        var f = fixture(); grant(f, "audit", "READ", "ORGANIZATION", null); login(f);
        assertEquals(403, request("GET", "/api/erp/audit", null).status());
        assertEquals(403, request("GET", "/api/erp/audit/1", null).status());
        grant(f, "audit", "READ", "SYSTEM", null);
        assertEquals(200, request("GET", "/api/erp/audit", null).status());
    }

    @Test
    void unitSalesRestrictStockRequestsReceiptsAndRetries() throws Exception {
        var f = fixture();
        grant(f, "sales", "READ", "UNIT", f.unit());
        grant(f, "sales", "CREATE", "UNIT", f.unit());
        grant(f, "core/people", "READ", "SYSTEM", null);
        long siblingAsset = tx.execute(status -> {
            var unit = new OrganizationalUnit(); unit.organization = em.find(Organization.class, f.organization());
            unit.code = "SECOND"; unit.name = "Second unit"; unit.type = "Office"; persist(unit);
            return asset(em.find(ItemModel.class, f.model()), location(unit.organization, unit)).id;
        });
        long siblingUnit = jdbc.queryForObject("select l.unit_id from erp_stock_location l join erp_asset_item a on a.location_id = l.id where a.id = ?", Long.class, siblingAsset);
        long unassignedAsset = jdbc.queryForObject("select a.id from erp_asset_item a join erp_stock_location l on a.location_id = l.id where l.organization_id = ? and l.unit_id is null", Long.class, f.organization());
        login(f);
        String scope = "organizationId=" + f.organization() + "&unitId=" + f.unit();
        var stock = request("GET", "/api/erp/sales/stock?" + scope + "&kind=ASSET", null);
        assertEquals(200, stock.status(), stock.raw());
        assertEquals(1, stock.body().get("totalElements").asInt());
        assertEquals(f.asset(), stock.body().get("content").get(0).get("stockId").asLong());
        assertEquals(403, request("GET", "/api/erp/sales/stock?organizationId=" + f.organization() + "&kind=ASSET", null).status());
        assertEquals(403, request("GET", "/api/erp/sales?organizationId=" + f.organization() + "&unitId=" + siblingUnit, null).status());
        assertEquals(403, request("GET", "/api/erp/sales/" + f.otherSale(), null).status());
        var data = new HashMap<String, Object>(Map.of("requestId", UUID.randomUUID().toString(), "organizationId", f.organization(),
            "buyerId", f.person(), "paymentMethod", "PIX", "items", List.of(Map.of("assetId", f.asset(), "quantity", "1", "expectedUnitPrice", "10"))));
        assertEquals(403, request("POST", "/api/erp/sales", data).status());
        data.put("unitId", f.unit());
        for (long forbidden : List.of(siblingAsset, unassignedAsset, f.otherAsset())) {
            data.put("items", List.of(Map.of("assetId", f.asset(), "quantity", "1", "expectedUnitPrice", "10"),
                Map.of("assetId", forbidden, "quantity", "1", "expectedUnitPrice", "10")));
            assertEquals(403, request("POST", "/api/erp/sales", data).status());
            assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, f.asset()));
            assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, forbidden));
        }
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_sale where request_id = ?", Long.class, data.get("requestId")));
        assertEquals(0L, jdbc.queryForObject("select count(*) from erp_stock_movement where asset_id = ?", Long.class, f.asset()));
        data.put("items", List.of(Map.of("assetId", f.asset(), "quantity", "1", "expectedUnitPrice", "10")));
        var result = request("POST", "/api/erp/sales", data);
        assertEquals(200, result.status(), result.raw());
        long id = result.body().get("id").asLong();
        assertEquals(f.unit(), result.body().get("unitId").asLong());
        assertEquals("Main unit", result.body().get("unitName").asText());
        assertEquals(200, request("GET", "/api/erp/sales/" + id, null).status());
        assertEquals(1, request("GET", "/api/erp/sales?" + scope, null).body().get("totalElements").asInt());
        var retry = request("POST", "/api/erp/sales", data);
        assertEquals(200, retry.status(), retry.raw());
        assertEquals(id, retry.body().get("id").asLong());
        assertEquals(1L, jdbc.queryForObject("select count(*) from erp_audit_record where resource = 'sales' and record_id = ?", Long.class, id));
        var audit = json.readTree(jdbc.queryForObject("select after_json from erp_audit_record where resource = 'sales' and record_id = ?", String.class, id));
        assertEquals(f.unit(), audit.get("sale").get("unitId").asLong());
        // A second unit grant does not expose the first unit's receipt.
        jdbc.update("delete from erp_user_profile where user_id = ?", f.user());
        grant(f, "sales", "READ", "UNIT", siblingUnit);
        assertEquals(403, request("GET", "/api/erp/sales/" + id, null).status());
    }

    @Test
    void organizationWideReceiptsRemainOutsideUnitScope() throws Exception {
        var f = fixture();
        long read = grant(f, "sales", "READ", "ORGANIZATION", null);
        long create = grant(f, "sales", "CREATE", "ORGANIZATION", null);
        grant(f, "core/people", "READ", "SYSTEM", null); login(f);
        var data = Map.of("requestId", UUID.randomUUID().toString(), "organizationId", f.organization(),
            "buyerId", f.person(), "paymentMethod", "PIX", "items", List.of(Map.of("assetId", f.asset(), "quantity", "1", "expectedUnitPrice", "10")));
        var result = request("POST", "/api/erp/sales", data);
        assertEquals(200, result.status(), result.raw());
        assertTrue(result.body().get("unitId").isNull());
        assertEquals(1, request("GET", "/api/erp/sales?organizationId=" + f.organization(), null).body().get("totalElements").asInt());
        jdbc.update("delete from erp_profile_permission where id in (?, ?)", read, create);
        grant(f, "sales", "READ", "UNIT", f.unit());
        grant(f, "sales", "CREATE", "UNIT", f.unit());
        assertEquals(0, request("GET", "/api/erp/sales?organizationId=" + f.organization() + "&unitId=" + f.unit(), null).body().get("totalElements").asInt());
        assertEquals(403, request("GET", "/api/erp/sales/" + result.body().get("id").asLong(), null).status());
        assertEquals(403, request("POST", "/api/erp/sales", data).status());
    }

    @Test
    void assetBatchHonorsCreateScopeAndRetryCannotBypassRevocation() throws Exception {
        var f = fixture();
        long createGrant = grant(f, "inventory/assets", "CREATE", "UNIT", f.unit());
        grant(f, "inventory/assets", "READ", "UNIT", f.unit());
        grant(f, "inventory/models", "READ", "SYSTEM", null);
        grant(f, "inventory/locations", "READ", "SYSTEM", null); login(f);
        var common = new HashMap<String, Object>(Map.of("modelId", f.model(), "locationId", f.otherLocation(),
            "condition", "GOOD", "status", "DRAFT", "currentValue", "0"));
        var payload = Map.of("requestId", UUID.randomUUID().toString(), "common", common,
            "items", List.of(Map.of("assetCode", UUID.randomUUID().toString(), "serialNumber", UUID.randomUUID().toString())));
        assertEquals(403, request("POST", "/api/erp/inventory/assets/batch", payload).status());
        common.put("locationId", f.location());
        var result = request("POST", "/api/erp/inventory/assets/batch", payload);
        assertEquals(200, result.status(), result.raw());
        jdbc.update("delete from erp_profile_permission where id=?", createGrant);
        assertEquals(403, request("POST", "/api/erp/inventory/assets/batch", payload).status());
    }

    @Test
    void custodyReceivingUnitRequiresReadPermissionWithoutChangingIssuingScope() throws Exception {
        var f = fixture();
        for (String action : List.of("READ", "CREATE", "RETURN")) grant(f, "custodies", action, "UNIT", f.unit());
        grant(f, "core/people", "READ", "SYSTEM", null); login(f);
        var data = Map.of("requestId", UUID.randomUUID().toString(), "organizationId", f.organization(),
            "unitId", f.unit(), "recipientUnitId", f.unit(), "authorizerId", f.person(),
            "purpose", "Unit custody", "assetIds", List.of(f.asset()));
        assertEquals(403, request("POST", "/api/erp/custodies", data).status());
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id=?", String.class, f.asset()));
        grant(f, "core/units", "READ", "UNIT", f.unit());
        var issued = request("POST", "/api/erp/custodies", data);
        assertEquals(200, issued.status(), issued.raw());
        assertEquals("UNIT", issued.body().get("recipientType").asText());
        long custody = issued.body().get("id").asLong();
        long item = issued.body().get("items").get(0).get("id").asLong();
        assertEquals(403, request("GET", "/api/erp/custodies?organizationId=" + f.organization(), null).status());
        var returned = request("POST", "/api/erp/custodies/" + custody + "/returns",
            Map.of("requestId", UUID.randomUUID().toString(), "itemIds", List.of(item)));
        assertEquals(200, returned.status(), returned.raw());
        assertEquals("RETURNED", returned.body().get("status").asText());
    }

    @Test
    void unitCustodyRestrictsStockIssueReceiptAndReturn() throws Exception {
        var f = fixture();
        grant(f, "custodies", "READ", "UNIT", f.unit());
        grant(f, "custodies", "CREATE", "UNIT", f.unit());
        grant(f, "custodies", "RETURN", "UNIT", f.unit());
        grant(f, "core/people", "READ", "SYSTEM", null); login(f);
        String scope = "organizationId=" + f.organization() + "&unitId=" + f.unit();
        var stock = request("GET", "/api/erp/custodies/stock?" + scope, null);
        assertEquals(200, stock.status(), stock.raw()); assertEquals(1, stock.body().get("totalElements").asInt());
        assertEquals(403, request("GET", "/api/erp/custodies/stock?organizationId=" + f.organization(), null).status());
        var data = new HashMap<String, Object>(Map.of("requestId", UUID.randomUUID().toString(), "organizationId", f.organization(),
            "recipientId", f.person(), "authorizerId", f.person(), "purpose", "Service duty", "assetIds", List.of(f.asset())));
        assertEquals(403, request("POST", "/api/erp/custodies", data).status());
        data.put("unitId", f.unit()); data.put("assetIds", List.of(f.asset(), f.otherAsset()));
        assertEquals(403, request("POST", "/api/erp/custodies", data).status());
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id = ?", String.class, f.asset()));
        data.put("assetIds", List.of(f.asset()));
        var issued = request("POST", "/api/erp/custodies", data); assertEquals(200, issued.status(), issued.raw());
        long custody = issued.body().get("id").asLong(); long item = issued.body().get("items").get(0).get("id").asLong();
        assertEquals(200, request("GET", "/api/erp/custodies/" + custody, null).status());
        assertEquals(1, request("GET", "/api/erp/custodies?" + scope, null).body().get("totalElements").asInt());
        var returned = request("POST", "/api/erp/custodies/" + custody + "/returns",
            Map.of("requestId", UUID.randomUUID().toString(), "itemIds", List.of(item)));
        assertEquals(200, returned.status(), returned.raw()); assertEquals("RETURNED", returned.body().get("status").asText());
    }

    @Test
    void regulatoryControlUsesTheFirearmCurrentOrganizationAndUnitScope() throws Exception {
        var f = fixture();
        long own = tx.execute(status -> {
            var control = new RegulatoryControl(); control.asset = em.find(AssetItem.class, f.asset());
            control.externalSystem = "REGISTRY_A"; control.registrationNumber = UUID.randomUUID().toString(); control.status = "ACTIVE";
            return persist(control).id;
        });
        long foreign = tx.execute(status -> {
            var control = new RegulatoryControl(); control.asset = em.find(AssetItem.class, f.otherAsset());
            control.externalSystem = "REGISTRY_A"; control.registrationNumber = UUID.randomUUID().toString(); control.status = "ACTIVE";
            return persist(control).id;
        });
        grant(f, "inventory/regulatory-controls", "READ", "UNIT", f.unit());
        grant(f, "inventory/regulatory-controls", "CREATE", "UNIT", f.unit());
        grant(f, "inventory/assets", "READ", "UNIT", f.unit()); login(f);
        var list = request("GET", "/api/erp/inventory/regulatory-controls", null);
        assertEquals(200, list.status(), list.raw());
        assertEquals(1, list.body().get("totalElements").asInt());
        assertEquals(own, list.body().get("content").get(0).get("id").asLong());
        assertEquals(403, request("GET", "/api/erp/inventory/regulatory-controls/" + foreign, null).status());
        var created = request("POST", "/api/erp/inventory/regulatory-controls", Map.of("assetId", f.asset(),
            "externalSystem", "REGISTRY_B", "registrationNumber", UUID.randomUUID().toString(), "status", "PENDING"));
        assertEquals(201, created.status(), created.raw());
        assertEquals(403, request("POST", "/api/erp/inventory/regulatory-controls", Map.of("assetId", f.otherAsset(),
            "externalSystem", "REGISTRY_B", "registrationNumber", UUID.randomUUID().toString(), "status", "PENDING")).status());
    }
}
