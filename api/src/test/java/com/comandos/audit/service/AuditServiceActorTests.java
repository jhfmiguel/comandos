package com.comandos.audit.service;

import com.comandos.security.api.CurrentActor;
import com.comandos.security.api.CurrentActorProvider;
import com.comandos.security.service.AccessPolicy;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditServiceActorTests {
    @Test
    void mapsAuthenticatedActor() {
        var actors = mock(CurrentActorProvider.class);
        when(actors.current()).thenReturn(
            new CurrentActor(42L, "operator", true)
        );

        var service = new AuditService(
            mock(EntityManager.class),
            mock(AccessPolicy.class),
            actors
        );

        var actor = service.actor();

        assertEquals(42L, actor.id());
        assertEquals("operator", actor.login());
        assertEquals("ACCOUNT", actor.type());
    }

    @Test
    void mapsAnonymousActor() {
        var actors = mock(CurrentActorProvider.class);
        when(actors.current()).thenReturn(CurrentActor.anonymous());

        var service = new AuditService(
            mock(EntityManager.class),
            mock(AccessPolicy.class),
            actors
        );

        var actor = service.actor();

        assertNull(actor.id());
        assertNull(actor.login());
        assertEquals("UNAUTHENTICATED", actor.type());
    }
}
