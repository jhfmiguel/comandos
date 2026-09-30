package com.comandos.notifications.api;

import java.util.Map;

/**
 * @deprecated Use {@link com.fariamiguel.notifications.api.NotificationMessage}.
 * This record remains only as a source-compatible adapter for product code.
 */
@Deprecated(forRemoval = true)
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

    public com.fariamiguel.notifications.api.NotificationMessage toShared() {
        return new com.fariamiguel.notifications.api.NotificationMessage(
            channel, recipient, subject, body, metadata
        );
    }

    public static NotificationMessage fromShared(com.fariamiguel.notifications.api.NotificationMessage message) {
        return new NotificationMessage(
            message.channel(), message.recipient(), message.subject(), message.body(), message.metadata()
        );
    }
}
