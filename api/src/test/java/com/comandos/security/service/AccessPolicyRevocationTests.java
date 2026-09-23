package com.comandos.security.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessPolicyRevocationTests {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void permissionRevocationTakesEffectWithoutNewLogin() {
        var rows = new AtomicReference<List<Object[]>>(List.<Object[]>of(
            new Object[]{"sales", "READ", "UNIT", 10L, 20L}
        ));

        var em = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
        when(query.setParameter("user", 42L)).thenReturn(query);
        when(query.getResultList()).thenAnswer(invocation -> rows.get());

        var principal = new AccountPrincipal(42L, 0L, "operator", "hash");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
            )
        );

        var policy = new AccessPolicy(em, true, true);

        assertTrue(policy.canScope("sales", "READ", 10L, 20L));

        rows.set(List.of());

        assertFalse(policy.canScope("sales", "READ", 10L, 20L));
    }
}
