package com.comandos.security.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
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

class AccessPolicyScopedGrantTests {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void organizationGrantIsLimitedToItsOrganization() {
        var policy = policyWithRows(List.<Object[]>of(
            new Object[]{"sales", "READ", "ORGANIZATION", 10L, null}
        ));

        assertTrue(policy.canScope("sales", "READ", 10L, null));
        assertTrue(policy.canScope("sales", "READ", 10L, 20L));
        assertFalse(policy.canScope("sales", "READ", 11L, null));
    }

    @Test
    void unitGrantRequiresExactOrganizationAndUnit() {
        var policy = policyWithRows(List.<Object[]>of(
            new Object[]{"sales", "READ", "UNIT", 10L, 20L}
        ));

        assertTrue(policy.canScope("sales", "READ", 10L, 20L));
        assertFalse(policy.canScope("sales", "READ", 10L, 21L));
        assertFalse(policy.canScope("sales", "READ", 11L, 20L));
        assertFalse(policy.canScope("sales", "READ", 10L, null));
    }

    private AccessPolicy policyWithRows(List<Object[]> rows) {
        var em = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
        when(query.setParameter("user", 42L)).thenReturn(query);
        when(query.getResultList()).thenReturn(rows);

        var principal = new AccountPrincipal(42L, 0L, "operator", "hash");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
            )
        );

        return new AccessPolicy(em, true, true);
    }
}
