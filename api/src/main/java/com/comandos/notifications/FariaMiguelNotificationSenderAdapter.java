package com.comandos.notifications;

import com.comandos.notifications.api.NotificationMessage;
import com.comandos.notifications.api.NotificationSender;
import com.fariamiguel.notifications.service.NotificationDispatcher;
import org.springframework.stereotype.Component;

/** Compatibility adapter for legacy COMANDOS callers. */
@Component
public final class FariaMiguelNotificationSenderAdapter implements NotificationSender {
    private final NotificationDispatcher dispatcher;

    public FariaMiguelNotificationSenderAdapter(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void send(NotificationMessage message) {
        dispatcher.send(message.toShared());
    }
}
