package com.comandos.security.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccessPolicyScopeTests {
    @Test
    void resolvesKnownResourceScopes() {
        var units = AccessPolicy.scope("core/units");
        assertEquals("organization.id", units.organizationPath());
        assertEquals("id", units.unitPath());

        var assets = AccessPolicy.scope("inventory/assets");
        assertEquals("location.organization.id", assets.organizationPath());
        assertEquals("location.unit.id", assets.unitPath());
    }

    @Test
    void unknownResourceHasNoEntityScope() {
        var scope = AccessPolicy.scope("unknown/resource");

        assertNull(scope.organizationPath());
        assertNull(scope.unitPath());
    }
}
