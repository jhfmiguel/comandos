package com.comandos.audit.api;

public interface AuditRecorder {

    void record(
        String resource,
        long recordId,
        String action,
        Object before,
        Object after
    );
}
