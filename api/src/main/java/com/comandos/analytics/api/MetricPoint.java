package com.comandos.analytics.api;

import java.time.Instant;
import java.util.Map;

public record MetricPoint(
    String name,
    double value,
    Instant occurredAt,
    Map<String, String> dimensions
) {
    public MetricPoint {
        dimensions = dimensions == null
            ? Map.of()
            : Map.copyOf(dimensions);
    }
}
