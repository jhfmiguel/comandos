package com.comandos.messaging;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher {
    private final ApplicationEventPublisher publisher;

    public DomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public <T> DomainEventEnvelope<T> publish(String eventType, T payload) {
        DomainEventEnvelope<T> event = DomainEventEnvelope.of(eventType, payload);
        publisher.publishEvent(event);
        return event;
    }
}
