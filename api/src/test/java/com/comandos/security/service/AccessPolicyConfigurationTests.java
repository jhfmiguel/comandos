package com.comandos.security.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AccessPolicyConfigurationTests {
    @Test
    void rejectsPermissionEnforcementWithoutLoginEnforcement() {
        var em = mock(EntityManager.class);

        assertThrows(
            IllegalStateException.class,
            () -> new AccessPolicy(em, true, false)
        );
    }

    @Test
    void disabledPermissionsAllowAnyScope() {
        var policy = new AccessPolicy(mock(EntityManager.class), false, false);

        assertTrue(policy.canScope("sales", "DELETE", 10L, 20L));
        assertDoesNotThrow(() ->
            policy.requireScope("sales", "DELETE", 10L, 20L)
        );
    }
}
