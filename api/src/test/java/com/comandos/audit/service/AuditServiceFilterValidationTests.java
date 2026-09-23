package com.comandos.audit.service;

import com.comandos.security.api.CurrentActorProvider;
import com.comandos.security.service.AccessPolicy;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AuditServiceFilterValidationTests {
    private AuditService service() {
        return new AuditService(
            mock(EntityManager.class),
            mock(AccessPolicy.class),
            mock(CurrentActorProvider.class)
        );
    }

    @Test
    void rejectsInvalidPageAndDateRange() {
        var service = service();

        assertEquals(
            400,
            assertThrows(
                ResponseStatusException.class,
                () -> service.list(null, null, null, null, null, null, -1)
            ).getStatusCode().value()
        );

        var from = Instant.parse("2026-09-22T12:00:00Z");
        var until = Instant.parse("2026-09-21T12:00:00Z");

        assertEquals(
            400,
            assertThrows(
                ResponseStatusException.class,
                () -> service.list(null, null, null, null, from, until, 0)
            ).getStatusCode().value()
        );
    }

    @Test
    void rejectsInvalidReferenceFilter() {
        var service = service();

        assertEquals(
            400,
            assertThrows(
                ResponseStatusException.class,
                () -> service.list(
                    null, null, null, null, null, null, 0,
                    0L, null, null, null, null
                )
            ).getStatusCode().value()
        );
    }
}
