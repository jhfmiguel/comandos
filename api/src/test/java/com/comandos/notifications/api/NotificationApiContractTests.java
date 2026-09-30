package com.comandos.notifications.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fariamiguel.notifications.api.NotificationMessage;
import com.fariamiguel.notifications.api.NotificationSender;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NotificationApiContractTests {

    @Test
    void nullMetadataBecomesEmptyImmutableMap() {
        var message = new NotificationMessage(
            "EMAIL",
            "user@example.com",
            "Subject",
            "Body",
            null
        );

        assertNotNull(message.metadata());
        assertTrue(message.metadata().isEmpty());
        assertThrows(
            UnsupportedOperationException.class,
            () -> message.metadata().put("key", "value")
        );
    }

    @Test
    void metadataIsDefensivelyCopied() {
        var source = new HashMap<String, String>();
        source.put("tenant", "alpha");

        var message = new NotificationMessage(
            "EMAIL",
            "user@example.com",
            "Subject",
            "Body",
            source
        );

        source.put("tenant", "changed");

        assertEquals("alpha", message.metadata().get("tenant"));
    }

    @Test
    void senderReceivesCompleteMessage() {
        var captured = new AtomicReference<NotificationMessage>();
        NotificationSender sender = new NotificationSender() {
            @Override
            public boolean supports(String channel) {
                return "PUSH".equals(channel);
            }

            @Override
            public void send(NotificationMessage message) {
                captured.set(message);
            }
        };

        var message = new NotificationMessage(
            "PUSH",
            "account:42",
            "Alert",
            "Something happened",
            Map.of("priority", "high")
        );

        assertTrue(sender.supports(message.channel()));
        sender.send(message);

        assertSame(message, captured.get());
        assertEquals("PUSH", captured.get().channel());
        assertEquals("account:42", captured.get().recipient());
        assertEquals("high", captured.get().metadata().get("priority"));
    }
}
