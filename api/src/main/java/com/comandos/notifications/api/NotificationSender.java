package com.comandos.notifications.api;

/**
 * @deprecated Notification delivery is owned by Faria Miguel. Product code should
 * publish {@code com.fariamiguel.notifications.api.NotificationMessage} through the
 * shared dispatcher; this interface remains only for legacy callers.
 */
@Deprecated(forRemoval = true)
public interface NotificationSender {
    void send(NotificationMessage message);
}
