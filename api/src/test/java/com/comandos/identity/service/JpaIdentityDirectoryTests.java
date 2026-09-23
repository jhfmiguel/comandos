package com.comandos.identity.service;

import com.comandos.core.model.Person;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaIdentityDirectoryTests {
    @Test
    void returnsEmptyWhenPersonDoesNotExist() {
        var em = mock(EntityManager.class);
        when(em.find(Person.class, 99L)).thenReturn(null);

        var directory = new JpaIdentityDirectory(em);

        assertFalse(directory.findPerson(99L).isPresent());
    }

    @Test
    void mapsPersonToReusableIdentity() {
        var person = new Person();
        person.id = 7L;
        person.personType = "INDIVIDUAL";
        person.fullName = "Example Person";
        person.taxId = "123";
        person.phone = "62999999999";
        person.email = "person@example.com";
        person.active = true;

        var em = mock(EntityManager.class);
        when(em.find(Person.class, 7L)).thenReturn(person);

        var identity = new JpaIdentityDirectory(em)
            .findPerson(7L)
            .orElseThrow();

        assertEquals(7L, identity.id());
        assertEquals("Example Person", identity.fullName());
        assertEquals("person@example.com", identity.email());
        assertEquals(true, identity.active());
    }
}
