package com.comandos.security.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class CurrentActorTests {
    @Test
    void createsAnonymousActorWithoutIdentity() {
        var actor = CurrentActor.anonymous();

        assertFalse(actor.authenticated());
        assertNull(actor.accountId());
        assertNull(actor.login());
    }
}
