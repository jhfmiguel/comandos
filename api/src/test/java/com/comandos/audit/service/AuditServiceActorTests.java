package com.comandos.audit.service;

import com.fariamiguel.security.api.CurrentActor;
import com.fariamiguel.security.api.CurrentActorProvider;
import com.comandos.security.service.AccessPolicy;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditServiceActorTests {
    @Test
    void mapsAuthenticatedActor() {
        var actors = mock(CurrentActorProvider.class);
        var current = mock(CurrentActor.class);
        when(current.authenticated()).thenReturn(true);
        when(current.id()).thenReturn("42");
        when(current.displayName()).thenReturn("operator");
        when(actors.currentActor()).thenReturn(current);

        var service = new AuditService(
            mock(EntityManager.class),
            mock(AccessPolicy.class),
            actors,
            List.of()
        );

        var actor = service.actor();

        assertEquals(42L, actor.id());
        assertEquals("operator", actor.login());
        assertEquals("ACCOUNT", actor.type());
    }

    @Test
    void mapsAnonymousActor() {
        var actors = mock(CurrentActorProvider.class);
        when(actors.currentActor()).thenReturn(CurrentActor.anonymous());

        var service = new AuditService(
            mock(EntityManager.class),
            mock(AccessPolicy.class),
            actors,
            List.of()
        );

        var actor = service.actor();

        assertNull(actor.id());
        assertNull(actor.login());
        assertEquals("UNAUTHENTICATED", actor.type());
    }
}
