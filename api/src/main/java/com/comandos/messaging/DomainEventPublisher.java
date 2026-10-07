package com.comandos.messaging;

import com.comandos.messaging.api.PlatformEvent;
import com.comandos.messaging.api.PlatformEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher {
    private final PlatformEventPublisher publisher;

    public DomainEventPublisher(PlatformEventPublisher publisher) {
        this.publisher = publisher;
    }

    public <T> DomainEventEnvelope<T> publish(String eventType, T payload) {
        PlatformEvent<T> event = publisher.publish(eventType, payload);
        return new DomainEventEnvelope<>(
            event.eventId(),
            event.eventType(),
            event.occurredAt(),
            event.payload()
        );
    }
}
