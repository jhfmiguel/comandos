package com.comandos.notifications.api;

import java.util.Map;

public record NotificationMessage(
    String channel,
    String recipient,
    String subject,
    String body,
    Map<String, String> metadata
) {
    public NotificationMessage {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
