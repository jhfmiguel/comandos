package com.comandos.security.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationServiceTests {
    @Test
    void requireAllowsAuthorizedScope() {
        AuthorizationService authorization =
            (resource, action, organizationId, unitId) -> true;

        assertDoesNotThrow(() ->
            authorization.require("inventory", "READ", 10L, 20L)
        );
    }

    @Test
    void requireRejectsUnauthorizedScope() {
        AuthorizationService authorization =
            (resource, action, organizationId, unitId) -> false;

        var exception = assertThrows(
            AccessDeniedException.class,
            () -> authorization.require("inventory", "WRITE", 10L, 20L)
        );

        assertEquals(
            "You do not have permission for this operation or scope.",
            exception.getMessage()
        );
    }
}
