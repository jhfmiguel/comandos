package com.comandos.messaging.api;

public interface PlatformEventPublisher {
    <T> PlatformEvent<T> publish(String eventType, T payload);
}
