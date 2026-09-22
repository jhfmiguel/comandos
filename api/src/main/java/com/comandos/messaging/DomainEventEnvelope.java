package com.comandos.messaging;

import java.time.Instant;
import java.util.UUID;

public record DomainEventEnvelope<T>(UUID eventId, String eventType, Instant occurredAt, T payload) {
    public static <T> DomainEventEnvelope<T> of(String eventType, T payload) {
        return new DomainEventEnvelope<>(UUID.randomUUID(), eventType, Instant.now(), payload);
    }
}
