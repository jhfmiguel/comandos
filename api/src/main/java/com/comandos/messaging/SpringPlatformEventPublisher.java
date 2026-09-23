package com.comandos.messaging;

import com.comandos.messaging.api.PlatformEvent;
import com.comandos.messaging.api.PlatformEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringPlatformEventPublisher implements PlatformEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringPlatformEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public <T> PlatformEvent<T> publish(String eventType, T payload) {
        PlatformEvent<T> event = PlatformEvent.of(eventType, payload);
        publisher.publishEvent(event);
        return event;
    }
}
