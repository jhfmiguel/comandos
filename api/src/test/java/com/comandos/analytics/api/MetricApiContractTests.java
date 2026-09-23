package com.comandos.analytics.api;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MetricApiContractTests {

    @Test
    void nullDimensionsBecomeEmptyImmutableMap() {
        var metric = new MetricPoint(
            "requests.total",
            12.0,
            Instant.parse("2026-09-23T10:00:00Z"),
            null
        );

        assertNotNull(metric.dimensions());
        assertTrue(metric.dimensions().isEmpty());
        assertThrows(
            UnsupportedOperationException.class,
            () -> metric.dimensions().put("service", "api")
        );
    }

    @Test
    void dimensionsAreDefensivelyCopied() {
        var source = new HashMap<String, String>();
        source.put("module", "core");

        var metric = new MetricPoint(
            "operation.duration",
            3.5,
            Instant.parse("2026-09-23T10:00:00Z"),
            source
        );

        source.put("module", "changed");

        assertEquals("core", metric.dimensions().get("module"));
    }

    @Test
    void recorderReceivesCompleteMetric() {
        var captured = new AtomicReference<MetricPoint>();
        MetricRecorder recorder = captured::set;

        var metric = new MetricPoint(
            "items.processed",
            7.0,
            Instant.parse("2026-09-23T10:00:00Z"),
            Map.of("module", "inventory")
        );

        recorder.record(metric);

        assertSame(metric, captured.get());
        assertEquals("items.processed", captured.get().name());
        assertEquals(7.0, captured.get().value());
        assertEquals("inventory", captured.get().dimensions().get("module"));
    }
}
