package com.weaponsregistration.core.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:core-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "logging.level.root=WARN", "logging.level.org.hibernate.SQL=OFF", "debug=false"
})
class CoreApiTests {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    record Result(int status, JsonNode body, String raw) {}

    private Result request(String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/erp/core/" + path));
        builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Result(response.statusCode(), response.body().isBlank() ? null : json.readTree(response.body()), response.body());
    }

    private JsonNode create(String resource, Map<String, Object> data) throws Exception {
        var result = request("POST", resource, data);
        assertEquals(201, result.status(), result.raw());
        return result.body();
    }

    private String unique() { return UUID.randomUUID().toString(); }

    private Map<String, Object> organizationData(String name) {
        return Map.of("name", name, "nature", "Private company", "publicOrganization", false, "active", true);
    }

    private JsonNode organization() throws Exception { return create("organizations", organizationData(unique())); }

    private JsonNode person() throws Exception {
        return create("people", Map.of("fullName", unique(), "personType", "INDIVIDUAL", "active", true));
    }

    private Map<String, Object> unitData(long organizationId) {
        return new HashMap<>(Map.of("organizationId", organizationId, "name", "Headquarters", "code", unique(), "type", "Office"));
    }

    @Test
    void catalogExposesAllCoreResourcesWithoutPersistenceClasses() throws Exception {
        var result = request("GET", "catalog", null);
        assertEquals(200, result.status());
        assertEquals(13, result.body().size());
        assertFalse(result.raw().contains("com.weaponsregistration"));
        for (var resource : result.body()) {
            var page = request("GET", resource.get("key").asText() + "?search=example", null);
            assertEquals(200, page.status(), page.raw());
        }
    }

    @Test
    void organizationCrudRejectsStaleWritesAndSupportsSearch() throws Exception {
        String name = unique();
        var created = create("organizations", organizationData(name));
        long id = created.get("id").asLong();
        var update = new HashMap<>(organizationData(name + " updated"));
        update.put("version", created.get("version").asLong());
        var saved = request("PUT", "organizations/" + id, update);
        assertEquals(200, saved.status(), saved.raw());
        assertEquals(409, request("PUT", "organizations/" + id, update).status());
        var matches = request("GET", "organizations?search=" + name, null);
        assertEquals(1, matches.body().get("totalElements").asInt());
        assertEquals(409, request("DELETE", "organizations/" + id + "?version=0", null).status());
        assertEquals(204, request("DELETE", "organizations/" + id + "?version=" + saved.body().get("version").asLong(), null).status());
        assertEquals(404, request("GET", "organizations/" + id, null).status());
    }

    @Test
    void unitHierarchyRejectsCyclesCrossOrganizationAndReferencedDeletion() throws Exception {
        long firstOrg = organization().get("id").asLong();
        long secondOrg = organization().get("id").asLong();
        var parentData = unitData(firstOrg);
        var parent = create("units", parentData);
        var childData = unitData(firstOrg);
        childData.put("parentUnitId", parent.get("id").asLong());
        var child = create("units", childData);

        var cycle = new HashMap<>(parentData);
        cycle.put("parentUnitId", child.get("id").asLong());
        cycle.put("version", parent.get("version").asLong());
        assertEquals(400, request("PUT", "units/" + parent.get("id").asLong(), cycle).status());

        var invalid = unitData(secondOrg);
        invalid.put("parentUnitId", parent.get("id").asLong());
        assertEquals(400, request("POST", "units", invalid).status());

        var move = new HashMap<>(parentData);
        move.put("organizationId", secondOrg);
        move.put("version", parent.get("version").asLong());
        assertEquals(400, request("PUT", "units/" + parent.get("id").asLong(), move).status());
        assertEquals(409, request("DELETE", "units/" + parent.get("id").asLong() + "?version=0", null).status());
        assertEquals(2, request("GET", "units?organizationId=" + firstOrg, null).body().get("totalElements").asInt());
    }

    @Test
    void personCanHaveMultipleRolesAndInvalidDatesAreRejected() throws Exception {
        long personId = person().get("id").asLong();
        long orgId = organization().get("id").asLong();
        long roleId = create("roles", Map.of("code", unique(), "name", "Buyer")).get("id").asLong();
        long otherRole = create("roles", Map.of("code", unique(), "name", "Employee")).get("id").asLong();
        var assignment = new HashMap<String, Object>(Map.of("personId", personId, "roleId", roleId,
            "organizationId", orgId, "startDate", "2026-01-01", "status", "ACTIVE"));
        var first = create("person-roles", assignment);
        assertTrue(first.get("referenceLabels").get("roleId").asText().contains("Buyer"));
        assignment.put("roleId", otherRole);
        create("person-roles", assignment);
        assignment.put("endDate", "2025-01-01");
        assertEquals(400, request("POST", "person-roles", assignment).status());
        assignment.remove("endDate");
        assignment.put("status", "ENDED");
        assertEquals(400, request("POST", "person-roles", assignment).status());
        var data = Map.<String, Object>of("personRoleId", first.get("id").asLong(), "key", "badge", "value", "A123");
        create("role-data", data);
        assertEquals(409, request("POST", "role-data", data).status());
        create("credentials", Map.of("personId", personId, "type", "License", "number", unique(), "validUntil", "2028-01-01"));
        create("qualifications", Map.of("personId", personId, "category", "Training", "validUntil", "2028-01-01", "status", "ACTIVE"));
    }

    @Test
    void accountsHashPasswordsNeverExposeThemAndKeepPasswordOnBlankUpdate() throws Exception {
        long personId = person().get("id").asLong();
        String login = unique();
        String password = "A-test-password-123";
        var data = new HashMap<String, Object>(Map.of("personId", personId, "login", login, "password", password, "blocked", false));
        var account = create("users", data);
        long id = account.get("id").asLong();
        assertFalse(account.toString().contains("password"));
        String hash = jdbc.queryForObject("select password_hash from erp_system_user where id = ?", String.class, id);
        assertNotEquals(password, hash);
        assertTrue(new BCryptPasswordEncoder().matches(password, hash));
        assertFalse(request("GET", "users", null).raw().contains(hash));
        data.put("password", "");
        data.put("version", account.get("version").asLong());
        assertEquals(200, request("PUT", "users/" + id, data).status());
        assertEquals(hash, jdbc.queryForObject("select password_hash from erp_system_user where id = ?", String.class, id));
        data.remove("version");
        data.put("password", "short");
        assertEquals(400, request("POST", "users", data).status());
        data.put("password", password);
        assertEquals(409, request("POST", "users", data).status());
    }

    @Test
    void accessAssignmentsValidateOrganizationAndPreventDuplicateScope() throws Exception {
        long personId = person().get("id").asLong();
        long userId = create("users", Map.of("personId", personId, "login", unique(), "password", "Another-password-123", "blocked", false)).get("id").asLong();
        long orgId = organization().get("id").asLong();
        long otherOrg = organization().get("id").asLong();
        long unitId = create("units", unitData(otherOrg)).get("id").asLong();
        long profileId = create("profiles", Map.of("name", unique(), "level", "Operator")).get("id").asLong();
        long permissionId = create("permissions", Map.of("resource", unique(), "action", "READ")).get("id").asLong();
        create("profile-permissions", Map.of("profileId", profileId, "permissionId", permissionId));
        var assignment = new HashMap<String, Object>(Map.of("userId", userId, "profileId", profileId, "organizationId", orgId));
        create("user-profiles", assignment);
        assertEquals(400, request("POST", "user-profiles", assignment).status());
        assignment.put("unitId", unitId);
        assertEquals(400, request("POST", "user-profiles", assignment).status());
    }

    @Test
    void validatesRequiredFieldsReferencesChoicesAndAllowlist() throws Exception {
        assertEquals(400, request("POST", "people", Map.of("personType", "INDIVIDUAL")).status());
        assertEquals(400, request("POST", "people", Map.of("personType", "INVALID", "fullName", "Example", "active", true)).status());
        var data = new HashMap<>(organizationData(unique()));
        data.put("id", 10);
        assertEquals(400, request("POST", "organizations", data).status());
        assertEquals(404, request("POST", "units", unitData(Long.MAX_VALUE)).status());
        assertEquals(400, request("GET", "people?size=101", null).status());
        assertEquals(404, request("GET", "unknown", null).status());
    }

    @Test
    void concurrentParentChangesCannotCreateACycle() throws Exception {
        long organizationId = organization().get("id").asLong();
        var firstData = unitData(organizationId);
        var secondData = unitData(organizationId);
        var first = create("units", firstData);
        var second = create("units", secondData);
        firstData.put("version", 0);
        firstData.put("parentUnitId", second.get("id").asLong());
        secondData.put("version", 0);
        secondData.put("parentUnitId", first.get("id").asLong());
        var firstRequest = CompletableFuture.supplyAsync(() -> {
            try { return request("PUT", "units/" + first.get("id").asLong(), firstData).status(); }
            catch (Exception ex) { throw new RuntimeException(ex); }
        });
        var secondRequest = CompletableFuture.supplyAsync(() -> {
            try { return request("PUT", "units/" + second.get("id").asLong(), secondData).status(); }
            catch (Exception ex) { throw new RuntimeException(ex); }
        });
        var statuses = java.util.List.of(firstRequest.get(), secondRequest.get());
        assertTrue(statuses.contains(200), statuses.toString());
        assertTrue(statuses.contains(400), statuses.toString());
    }
}
