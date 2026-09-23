package com.comandos.identity.api;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonIdentityTests {
    @Test
    void exposesReusablePersonIdentityData() {
        var birthDate = LocalDate.of(1990, 5, 20);
        var identity = new PersonIdentity(
            10L,
            "INDIVIDUAL",
            "Example Person",
            "12345678900",
            birthDate,
            "62999999999",
            "person@example.com",
            true
        );

        assertEquals(10L, identity.id());
        assertEquals("INDIVIDUAL", identity.personType());
        assertEquals("Example Person", identity.fullName());
        assertEquals("12345678900", identity.taxId());
        assertEquals(birthDate, identity.birthDate());
        assertEquals("62999999999", identity.phone());
        assertEquals("person@example.com", identity.email());
        assertTrue(identity.active());
        assertFalse(new PersonIdentity(
            11L, "INDIVIDUAL", "Inactive", null, null, null, null, false
        ).active());
    }
}
