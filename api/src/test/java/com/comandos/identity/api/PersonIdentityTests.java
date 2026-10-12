package com.comandos.identity.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fariamiguel.identity.api.IdentitySubject;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PersonIdentityTests {
    @Test
    void preservesCanonicalIdentitySubjectContract() {
        IdentitySubject subject = new IdentitySubject(
            "COMANDOS", "10", "Example Person", "person@example.com",
            Map.of("legacyPersonId", "10", "active", "true")
        );
        assertEquals("COMANDOS", subject.provider());
        assertEquals("10", subject.subject());
        assertEquals("Example Person", subject.displayName());
        assertEquals("person@example.com", subject.email());
        assertTrue(Boolean.parseBoolean(subject.attributes().get("active")));
    }
}
