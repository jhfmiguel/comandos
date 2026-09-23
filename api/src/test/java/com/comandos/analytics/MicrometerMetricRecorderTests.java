package com.comandos.analytics;

import static org.junit.jupiter.api.Assertions.*;

import com.comandos.analytics.api.MetricPoint;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MicrometerMetricRecorderTests {

    @Test
    void recordsAndUpdatesGaugeWithStableTags() {
        var registry = new SimpleMeterRegistry();
        var recorder = new MicrometerMetricRecorder(registry);

        recorder.record(new MetricPoint(
            "platform.queue.depth",
            3.0,
            Instant.parse("2026-09-23T10:00:00Z"),
            Map.of("module", "workflow", "region", "go")
        ));

        var gauge = registry.find("platform.queue.depth")
            .tag("module", "workflow")
            .tag("region", "go")
            .gauge();

        assertNotNull(gauge);
        assertEquals(3.0, gauge.value());

        recorder.record(new MetricPoint(
            "platform.queue.depth",
            8.0,
            Instant.parse("2026-09-23T10:01:00Z"),
            Map.of("region", "go", "module", "workflow")
        ));

        assertEquals(8.0, gauge.value());
        assertEquals(
            1,
            registry.find("platform.queue.depth").meters().size()
        );
    }

    @Test
    void rejectsMissingMetric() {
        var recorder = new MicrometerMetricRecorder(
            new SimpleMeterRegistry()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> recorder.record(null)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> recorder.record(new MetricPoint(
                " ",
                1.0,
                Instant.now(),
                Map.of()
            ))
        );
    }
}
