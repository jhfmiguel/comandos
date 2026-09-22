package com.comandos;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ComandosModularityTests {

    @Test
    void discoversApplicationModules() {
        var modules = ApplicationModules.of(ComandosApiApplication.class);
        assertTrue(modules.stream().findAny().isPresent());
    }
}
