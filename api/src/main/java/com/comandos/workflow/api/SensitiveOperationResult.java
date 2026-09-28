package com.comandos.workflow.api;

public record SensitiveOperationResult<T>(
    T value,
    Long recordId
) {
    public SensitiveOperationResult {
        if (recordId != null && recordId <= 0) {
            throw new IllegalArgumentException("Record id must be positive when informed.");
        }
    }

    public static <T> SensitiveOperationResult<T> of(T value) {
        return new SensitiveOperationResult<>(value, null);
    }

    public static <T> SensitiveOperationResult<T> withRecord(T value, long recordId) {
        return new SensitiveOperationResult<>(value, recordId);
    }
}
