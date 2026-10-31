package com.comandos.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.comandos.core.model.Person;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class JpaIdentityDirectoryTests {

    @Test
    void returnsEmptyWhenPersonDoesNotExist() {
        var em = mock(EntityManager.class);
        when(em.find(Person.class, 99L)).thenReturn(null);

        var directory = new JpaIdentityDirectory(em);

        assertFalse(directory.find("COMANDOS", "99").isPresent());
    }

    @Test
    void keepsLegacyLookupWhileExposingCanonicalIdentityDirectory() {
        var person = new Person();
        person.id = 7L;
        person.personType = "INDIVIDUAL";
        person.fullName = "Example Person";
        person.taxId = "123";
        person.birthDate = LocalDate.of(1990, 1, 2);
        person.phone = "62999999999";
        person.email = "person@example.com";
        person.active = true;

        var em = mock(EntityManager.class);
        when(em.find(Person.class, 7L)).thenReturn(person);

        var directory = new JpaIdentityDirectory(em);
        var canonical = directory.find("comandos", "7").orElseThrow();

        assertEquals("COMANDOS", canonical.provider());
        assertEquals("7", canonical.subject());
        assertEquals("Example Person", canonical.displayName());
        assertEquals("person@example.com", canonical.email());
        assertEquals("INDIVIDUAL", canonical.attributes().get("personType"));
        assertEquals("123", canonical.attributes().get("taxId"));
        assertEquals("1990-01-02", canonical.attributes().get("birthDate"));
        assertEquals("62999999999", canonical.attributes().get("phone"));
        assertEquals("true", canonical.attributes().get("active"));
    }

    @Test
    void ignoresUnknownProvidersAndMalformedSubjects() {
        var em = mock(EntityManager.class);
        var directory = new JpaIdentityDirectory(em);

        assertTrue(directory.find("SSP", "7").isEmpty());
        assertTrue(directory.find("COMANDOS", "not-a-number").isEmpty());
        assertTrue(directory.find("COMANDOS", "0").isEmpty());
        assertTrue(directory.find("COMANDOS", null).isEmpty());
    }
}
