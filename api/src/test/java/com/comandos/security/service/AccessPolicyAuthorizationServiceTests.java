package com.comandos.security.service;

import com.comandos.security.api.AuthorizationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessPolicyAuthorizationServiceTests {
    @Test
    void delegatesScopeDecisionToAccessPolicy() {
        var policy = mock(AccessPolicy.class);
        AuthorizationService service =
            new AccessPolicyAuthorizationService(policy);

        when(policy.canScope("sales", "READ", 10L, 20L))
            .thenReturn(true);
        when(policy.canScope("sales", "DELETE", 10L, 20L))
            .thenReturn(false);

        assertTrue(service.can("sales", "READ", 10L, 20L));
        assertFalse(service.can("sales", "DELETE", 10L, 20L));
    }
}
