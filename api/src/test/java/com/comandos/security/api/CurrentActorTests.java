package com.comandos.security.api;

import com.fariamiguel.security.api.CurrentActor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CurrentActorTests {
    @Test
    void consumesCanonicalAnonymousActor() {
        var actor = CurrentActor.anonymous();

        assertFalse(actor.authenticated());
        assertEquals("anonymous", actor.id());
        assertEquals("Anonymous", actor.displayName());
    }
}
