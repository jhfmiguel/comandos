package com.comandos.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.comandos.core.service.CanonicalMasterDataDirectory;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.enterprise.people.Person;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JpaIdentityDirectoryActiveFlagTests {
    @Test
    void mapsCanonicalNonActiveLifecycleToInactive() {
        var canonical = mock(CanonicalMasterDataDirectory.class);
        var person = mock(Person.class);
        when(person.name()).thenReturn("Example Person");
        when(person.phones()).thenReturn(Set.of());
        when(person.emails()).thenReturn(Set.of());
        when(person.attributes()).thenReturn(Map.of());
        when(person.status()).thenReturn(null);
        when(canonical.findPerson(eq(1L), any())).thenReturn(Optional.of(person));

        var subject = new JpaIdentityDirectory(canonical).find("COMANDOS", "1").orElseThrow();
        assertEquals("false", subject.attributes().get("active"));
        assertFalse(Boolean.parseBoolean(subject.attributes().get("active")));
    }
}
