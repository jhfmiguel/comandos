package com.comandos.audit.service;

import com.fariamiguel.audit.api.AuditEvent;
import com.fariamiguel.audit.api.AuditSink;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product adapter: shared platform audit events are persisted in the existing
 * COMANDOS audit history while the product-specific query/read model remains local.
 */
@Component
public final class FariaMiguelAuditSinkAdapter implements AuditSink {
    private final AuditService audit;

    public FariaMiguelAuditSinkAdapter(AuditService audit) {
        this.audit = audit;
    }

    @Override
    @Transactional
    public void append(AuditEvent event) {
        long recordId = numericId(event.resourceId());
        Map<String, String> snapshot = new LinkedHashMap<>(event.metadata());
        snapshot.putIfAbsent("sharedAuditId", event.id().toString());
        snapshot.putIfAbsent("correlationId", event.correlationId() == null ? "" : event.correlationId());
        snapshot.putIfAbsent("sharedActor", event.actorId() == null ? "" : event.actorId());
        snapshot.putIfAbsent("sharedOccurredAt", event.occurredAt().toString());
        audit.record(event.resourceType(), recordId, event.action(), null, snapshot);
    }

    private static long numericId(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return Integer.toUnsignedLong(value.hashCode());
        }
    }
}
