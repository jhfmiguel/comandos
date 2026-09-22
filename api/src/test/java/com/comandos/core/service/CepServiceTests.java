package com.comandos.core.service;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class CepServiceTests {
    @Test
    void parsesProviderFieldsAndHandlesMissingInvalidAndUnavailableCep() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String body = path.contains("99999999") ? "{\"erro\":true}" :
                path.contains("88888888") ? "invalid json" :
                "{\"logradouro\":\"Praça da Sé\",\"bairro\":\"Sé\",\"localidade\":\"São Paulo\",\"uf\":\"SP\",\"complemento\":\"lado ímpar\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(path.contains("77777777") ? 503 : 200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        var service = new CepService(HttpClient.newHttpClient(), "http://127.0.0.1:" + server.getAddress().getPort() + "/");
        try {
            var address = service.lookup("01001-000");
            assertEquals("Praça da Sé", address.get("street"));
            assertEquals("Sé", address.get("district"));
            assertEquals("São Paulo", address.get("city"));
            assertEquals("SP", address.get("state"));
            assertEquals("lado ímpar", address.get("complement"));
            assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.lookup("0100A000")).getStatusCode().value());
            assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.lookup("99999999")).getStatusCode().value());
            assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.lookup("88888888")).getStatusCode().value());
            assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.lookup("77777777")).getStatusCode().value());
        } finally { server.stop(0); }
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.lookup("01001000")).getStatusCode().value());
    }
}
