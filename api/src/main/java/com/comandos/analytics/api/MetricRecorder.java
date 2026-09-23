package com.comandos.analytics.api;

public interface MetricRecorder {
    void record(MetricPoint metric);
}
