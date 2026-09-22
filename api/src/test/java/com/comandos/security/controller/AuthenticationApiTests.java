package com.comandos.security.controller;

import com.comandos.core.service.CoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:authentication-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "erp.security.require-login=true", "logging.level.root=WARN", "debug=false"
})
class AuthenticationApiTests {
    @LocalServerPort int port;
    @Autowired CoreService core;
    @Autowired JdbcTemplate jdbc;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
    private final JsonMapper json = JsonMapper.builder().build();
    private static final String PASSWORD = "Test-password-12345";
    record Account(long id, long personId, String login) {}

    private Account account() {
        long person = (Long) core.save("people", null, Map.of("personType", "INDIVIDUAL", "fullName", "Test account", "active", true)).get("id");
        String login = UUID.randomUUID().toString();
        long user = (Long) core.save("users", null, Map.of("personId", person, "login", login, "password", PASSWORD, "blocked", false)).get("id");
        return new Account(user, person, login);
    }
    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode token() throws Exception { return json.readTree(get("/api/auth/csrf").body()); }
    private HttpResponse<String> post(String path, String body, JsonNode csrf) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body));
        if (csrf != null) builder.header(csrf.get("headerName").asText(), csrf.get("token").asText());
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> login(Account account) throws Exception {
        return post("/api/auth/login", "username=" + account.login() + "&password=" + PASSWORD, token());
    }

    @Test
    void allBusinessModulesRequireAuthenticationWhenEnabled() throws Exception {
        for (String path : List.of("/api/erp/core/catalog", "/api/erp/inventory/catalog", "/api/erp/sales?organizationId=1", "/api/users", "/api/weapons", "/api/sales"))
            assertEquals(401, get(path).statusCode(), path);
        var session = json.readTree(get("/api/auth/session").body());
        assertTrue(session.get("requireLogin").asBoolean());
        assertTrue(session.get("user").isNull());
    }

    @Test
    void loginRotatesSessionAndNeverReturnsCredentials() throws Exception {
        var account = account();
        var csrf = token();
        var oldId = cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
        var response = post("/api/auth/login", "username=" + account.login() + "&password=" + PASSWORD, csrf);
        assertEquals(204, response.statusCode(), response.body());
        var newId = cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
        assertNotEquals(oldId, newId);
        assertEquals(200, get("/api/erp/core/catalog").statusCode());
        String raw = get("/api/auth/session").body();
        assertEquals(account.id(), json.readTree(raw).get("user").get("id").asLong());
        assertFalse(raw.contains("password")); assertFalse(raw.contains("$2"));
    }

    @Test
    void loginAndLogoutRequireCsrfAndLogoutInvalidatesSession() throws Exception {
        var account = account();
        assertEquals(403, post("/api/auth/login", "username=" + account.login() + "&password=" + PASSWORD, null).statusCode());
        assertEquals(204, login(account).statusCode());
        assertEquals(403, post("/api/auth/logout", "", null).statusCode());
        assertEquals(200, get("/api/erp/core/catalog").statusCode());
        assertEquals(204, post("/api/auth/logout", "", token()).statusCode());
        assertEquals(401, get("/api/erp/core/catalog").statusCode());
    }

    @Test
    void invalidBlockedInactiveAndMfaAccountsCannotSignIn() throws Exception {
        var account = account();
        var wrong = post("/api/auth/login", "username=" + account.login() + "&password=wrong", token());
        assertEquals(401, wrong.statusCode());
        jdbc.update("update erp_system_user set blocked = true where id = ?", account.id());
        assertEquals(wrong.body(), login(account).body());
        jdbc.update("update erp_system_user set blocked = false, mfa_enabled = true where id = ?", account.id());
        assertEquals(401, login(account).statusCode());
        jdbc.update("update erp_system_user set mfa_enabled = false where id = ?", account.id());
        jdbc.update("update erp_person set active = false where id = ?", account.personId());
        assertEquals(401, login(account).statusCode());
    }

    @Test
    void accountChangesInvalidateExistingSessions() throws Exception {
        var account = account();
        assertEquals(204, login(account).statusCode());
        jdbc.update("update erp_system_user set blocked = true where id = ?", account.id());
        assertEquals(401, get("/api/erp/core/catalog").statusCode());
        jdbc.update("update erp_system_user set blocked = false where id = ?", account.id());
        assertEquals(204, login(account).statusCode());
        jdbc.update("update erp_system_user set version = version + 1 where id = ?", account.id());
        assertEquals(401, get("/api/erp/core/catalog").statusCode());
        assertEquals(204, login(account).statusCode());
        jdbc.update("update erp_person set active = false where id = ?", account.personId());
        assertEquals(401, get("/api/erp/core/catalog").statusCode());
    }

    @Test
    void authenticatedWritesRequireCsrfAndBrowserCorsAllowsCredentials() throws Exception {
        var account = account();
        assertEquals(204, login(account).statusCode());
        var uri = URI.create("http://localhost:" + port + "/api/erp/core/people");
        var payload = "{\"personType\":\"INDIVIDUAL\",\"fullName\":\"New person\",\"active\":true}";
        var builder = HttpRequest.newBuilder(uri).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload));
        assertEquals(403, client.send(builder.build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        var csrf = token();
        builder.header(csrf.get("headerName").asText(), csrf.get("token").asText()).header("Origin", "http://localhost:3000");
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), response.body());
        assertEquals("true", response.headers().firstValue("Access-Control-Allow-Credentials").orElseThrow());
        assertEquals("http://localhost:3000", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    }
}
