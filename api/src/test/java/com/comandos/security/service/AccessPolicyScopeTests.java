package com.comandos.security.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccessPolicyScopeTests {
    @Test
    void resolvesKnownResourceScopes() {
        var units = ComandosAccessPolicy.scope("core/units");
        assertEquals("organization.id", units.organizationPath());
        assertEquals("id", units.unitPath());

        var assets = ComandosAccessPolicy.scope("inventory/assets");
        assertEquals("location.organizationLegacyId", assets.organizationPath());
        assertEquals("location.unitLegacyId", assets.unitPath());
    }

    @Test
    void unknownResourceHasNoEntityScope() {
        var scope = ComandosAccessPolicy.scope("unknown/resource");

        assertNull(scope.organizationPath());
        assertNull(scope.unitPath());
    }
}
