package com.comandos.messaging.api;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PlatformEventContractTests {

    @Test
    void factoryCreatesIdentityTimestampAndPayload() {
        var before = Instant.now();
        var event = PlatformEvent.of("USER_CREATED", "payload");
        var after = Instant.now();

        assertNotNull(event.eventId());
        assertEquals("USER_CREATED", event.eventType());
        assertEquals("payload", event.payload());
        assertFalse(event.occurredAt().isBefore(before));
        assertFalse(event.occurredAt().isAfter(after));
    }

    @Test
    void publisherContractReturnsPublishedEvent() {
        var captured = new AtomicReference<PlatformEvent<?>>();

        PlatformEventPublisher publisher = new PlatformEventPublisher() {
            @Override
            public <T> PlatformEvent<T> publish(String eventType, T payload) {
                var event = PlatformEvent.of(eventType, payload);
                captured.set(event);
                return event;
            }
        };

        var published = publisher.publish("ORDER_UPDATED", 42L);

        assertSame(published, captured.get());
        assertEquals("ORDER_UPDATED", published.eventType());
        assertEquals(42L, published.payload());
    }
}
