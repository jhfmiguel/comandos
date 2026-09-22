package com.comandos.audit.controller;

import java.net.URI;
import java.net.http.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

public final class AuditTraceAssertions {
    private AuditTraceAssertions() {}

    public static void trace(int port, String resource, long recordId, String filters, String... actions) throws Exception {
        var client = HttpClient.newHttpClient();
        var json = JsonMapper.builder().build();
        String base = "http://localhost:" + port + "/api/erp/audit";
        var response = client.send(HttpRequest.newBuilder(URI.create(base + "?resource=" + resource
            + "&recordId=" + recordId + "&" + filters)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        var page = json.readTree(response.body());
        assertEquals(actions.length, page.get("totalElements").asInt(), response.body());
        List<String> actual = new ArrayList<>();
        for (var event : page.get("content")) {
            actual.add(event.get("action").asText());
            assertFalse(event.get("occurredAt").asText().isBlank());
            var detailResponse = client.send(HttpRequest.newBuilder(URI.create(base + "/" + event.get("id").asLong())).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, detailResponse.statusCode());
            var detail = json.readTree(detailResponse.body());
            assertTrue(detail.get("after").isObject());
            assertEquals(recordId, detail.get("event").get("recordId").asLong());
        }
        var expected = new ArrayList<>(List.of(actions));
        Collections.sort(expected); Collections.sort(actual);
        assertEquals(expected, actual);
    }
}
