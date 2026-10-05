package com.comandos.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fariamiguel.security.api.PlatformPrincipal;
import org.junit.jupiter.api.Test;

class AccountPrincipalTests {

    @Test
    void exposesLocalAccountThroughCanonicalPlatformPrincipalContract() {
        PlatformPrincipal principal = new AccountPrincipal(
            42L,
            7L,
            "operator",
            "Operador de Armamento",
            "hash"
        );

        assertEquals("42", principal.subject());
        assertEquals("Operador de Armamento", principal.displayName());
        assertTrue(principal.authorities().contains("AUTHENTICATED"));
        assertTrue(principal.roles().isEmpty());
        assertTrue(principal.scopeGrants().isEmpty());
    }

    @Test
    void fallsBackToLoginWhenDisplayNameIsBlank() {
        PlatformPrincipal principal = new AccountPrincipal(
            7L,
            1L,
            "fallback-user",
            " ",
            "hash"
        );

        assertEquals("fallback-user", principal.displayName());
    }
}
