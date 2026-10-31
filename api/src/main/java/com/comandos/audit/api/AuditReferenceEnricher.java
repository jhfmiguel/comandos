package com.comandos.audit.api;

import java.util.function.BiConsumer;

/**
 * Extension point for product/vertical modules to add related references to an audit event
 * without making the audit foundation depend on those vertical domains.
 */
public interface AuditReferenceEnricher {
    void enrich(String kind, long targetId, BiConsumer<String, Long> addReference);
}
