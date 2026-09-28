package com.comandos.workflow.api;

@FunctionalInterface
public interface SensitiveOperation<T> {
    SensitiveOperationResult<T> execute();
}
