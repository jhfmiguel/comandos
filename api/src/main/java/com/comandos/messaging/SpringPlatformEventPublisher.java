package com.comandos.messaging;

import com.comandos.messaging.api.PlatformEvent;
import com.comandos.messaging.api.PlatformEventPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SpringPlatformEventPublisher implements PlatformEventPublisher {

    private final com.fariamiguel.messaging.api.PlatformEventPublisher publisher;

    public SpringPlatformEventPublisher(
            com.fariamiguel.messaging.api.PlatformEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public <T> PlatformEvent<T> publish(String eventType, T payload) {
        PlatformEvent<T> event = PlatformEvent.of(eventType, payload);

        publisher.publish(new com.fariamiguel.messaging.api.PlatformEvent(
            event.eventId(),
            event.eventType(),
            event.occurredAt(),
            null,
            null,
            canonicalPayload(payload)
        ));

        return event;
    }

    private Map<String, Object> canonicalPayload(Object payload) {
        if (payload == null) {
            return Map.of();
        }

        if (payload instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, value) -> result.put(String.valueOf(key), value));
            return Map.copyOf(result);
        }

        return Map.of("value", payload);
    }
}
