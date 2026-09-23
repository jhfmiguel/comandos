package com.comandos.identity.service;

import com.comandos.core.model.Person;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaIdentityDirectoryActiveFlagTests {
    @Test
    void treatsNullOrFalseActiveFlagAsInactive() {
        var em = mock(EntityManager.class);

        var nullActive = new Person();
        nullActive.id = 1L;
        nullActive.personType = "INDIVIDUAL";
        nullActive.fullName = "Null Active";
        nullActive.active = null;

        when(em.find(Person.class, 1L)).thenReturn(nullActive);

        var directory = new JpaIdentityDirectory(em);

        assertFalse(directory.findPerson(1L).orElseThrow().active());

        var falseActive = new Person();
        falseActive.id = 2L;
        falseActive.personType = "INDIVIDUAL";
        falseActive.fullName = "False Active";
        falseActive.active = false;

        when(em.find(Person.class, 2L)).thenReturn(falseActive);

        assertFalse(directory.findPerson(2L).orElseThrow().active());
    }
}
