package com.comandos.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ComandosModuleCatalogTests {
    @Test
    void productModulesAreOwnedAndDeclaredByComandos() {
        var modules = new ComandosModuleCatalog().modules();

        assertEquals(5, modules.size());
        assertTrue(modules.stream().allMatch(module -> module.productId().equals("comandos")));
        assertTrue(modules.stream().anyMatch(module -> module.id().value().equals("comandos.armament")));
        assertTrue(modules.stream().anyMatch(module -> module.id().value().equals("comandos.operations")));
    }
}