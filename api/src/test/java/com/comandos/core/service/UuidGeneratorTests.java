package com.comandos.core.service;

import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UuidGeneratorTests {
    @Test
    void generatesNonNullUniqueUuids() {
        var generator = new UuidGenerator();
        var generated = new HashSet<UUID>();

        for (int i = 0; i < 100; i++) {
            var id = generator.next();
            assertNotNull(id);
            generated.add(id);
        }

        assertEquals(100, generated.size());
    }
}
