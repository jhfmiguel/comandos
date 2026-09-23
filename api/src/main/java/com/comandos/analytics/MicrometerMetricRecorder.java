package com.comandos.analytics;

import com.comandos.analytics.api.MetricPoint;
import com.comandos.analytics.api.MetricRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class MicrometerMetricRecorder implements MetricRecorder {

    private final MeterRegistry registry;
    private final Map<MetricKey, AtomicReference<Double>> gauges =
        new ConcurrentHashMap<>();

    public MicrometerMetricRecorder(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void record(MetricPoint metric) {
        if (metric == null) {
            throw new IllegalArgumentException("metric is required.");
        }
        if (metric.name() == null || metric.name().isBlank()) {
            throw new IllegalArgumentException("metric name is required.");
        }

        MetricKey key = MetricKey.of(metric.name(), metric.dimensions());

        AtomicReference<Double> value = gauges.computeIfAbsent(
            key,
            ignored -> {
                AtomicReference<Double> reference =
                    new AtomicReference<>(metric.value());

                registry.gauge(
                    metric.name().trim(),
                    Tags.of(key.tags()),
                    reference,
                    current -> current.get()
                );

                return reference;
            }
        );

        value.set(metric.value());
    }

    private record MetricKey(
        String name,
        java.util.List<io.micrometer.core.instrument.Tag> tags
    ) {
        static MetricKey of(
            String name,
            Map<String, String> dimensions
        ) {
            var tags = dimensions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> io.micrometer.core.instrument.Tag.of(
                    entry.getKey(),
                    entry.getValue()
                ))
                .toList();

            return new MetricKey(name.trim(), tags);
        }
    }
}
