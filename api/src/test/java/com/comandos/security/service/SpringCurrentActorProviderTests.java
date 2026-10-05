package com.comandos.security.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringCurrentActorProviderTests {
    private final SpringCurrentActorProvider provider =
        new SpringCurrentActorProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCanonicalAnonymousActorWhenAuthenticationIsMissing() {
        var actor = provider.currentActor();

        assertFalse(actor.authenticated());
        assertEquals("anonymous", actor.id());
    }

    @Test
    void exposesComandosAccountThroughCanonicalCurrentActor() {
        var principal =
            new AccountPrincipal(42L, 3L, "operator", "hash");
        var authentication =
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var actor = provider.currentActor();

        assertTrue(actor.authenticated());
        assertEquals("42", actor.id());
        assertEquals("operator", actor.displayName());
    }
}
