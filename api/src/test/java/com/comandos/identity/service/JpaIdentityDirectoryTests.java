package com.comandos.identity.service;
import com.fariamiguel.app.identity.service.JpaIdentityDirectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fariamiguel.app.core.service.CanonicalMasterDataDirectory;
import com.fariamiguel.enterprise.people.Person;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JpaIdentityDirectoryTests {
    @Test
    void returnsEmptyWhenCanonicalPersonDoesNotExist() {
        var canonical = mock(CanonicalMasterDataDirectory.class);
        when(canonical.findPerson(eq(99L), any())).thenReturn(Optional.empty());
        assertFalse(new JpaIdentityDirectory(null, canonical, "comandos").find("COMANDOS", "99").isPresent());
    }

    @Test
    void mapsCanonicalPersonIntoIdentityContract() {
        var canonical = mock(CanonicalMasterDataDirectory.class);
        var person = mock(Person.class);
        when(person.name()).thenReturn("Example Person");
        when(person.taxId()).thenReturn("123");
        when(person.emails()).thenReturn(Set.of("person@example.com"));
        when(person.phones()).thenReturn(Set.of("62999999999"));
        when(person.attributes()).thenReturn(Map.of("personType", "INDIVIDUAL"));
        when(person.status()).thenReturn(LifecycleStatus.ACTIVE);
        when(canonical.findPerson(eq(7L), any())).thenReturn(Optional.of(person));

        var subject = new JpaIdentityDirectory(null, canonical, "comandos").find("comandos", "7").orElseThrow();
        assertEquals("COMANDOS", subject.provider());
        assertEquals("7", subject.subject());
        assertEquals("Example Person", subject.displayName());
        assertEquals("person@example.com", subject.email());
        assertEquals("INDIVIDUAL", subject.attributes().get("personType"));
        assertEquals("123", subject.attributes().get("taxId"));
        assertEquals("62999999999", subject.attributes().get("phone"));
        assertEquals("true", subject.attributes().get("active"));
    }

    @Test
    void rejectsUnknownProviderAndMalformedSubjects() {
        var directory = new JpaIdentityDirectory(null, mock(CanonicalMasterDataDirectory.class), "comandos");
        assertTrue(directory.find("SSP", "7").isEmpty());
        assertTrue(directory.find("COMANDOS", "not-a-number").isEmpty());
        assertTrue(directory.find("COMANDOS", "0").isEmpty());
        assertTrue(directory.find("COMANDOS", null).isEmpty());
    }
}
