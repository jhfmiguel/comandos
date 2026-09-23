package com.comandos.messaging.api;

import java.time.Instant;
import java.util.UUID;

public record PlatformEvent<T>(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    T payload
) {
    public static <T> PlatformEvent<T> of(String eventType, T payload) {
        return new PlatformEvent<>(
            UUID.randomUUID(),
            eventType,
            Instant.now(),
            payload
        );
    }
}
