package com.comandos.security.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessPolicyScopeTest {

    @Test
    void migratedProductResourcesUseScalarLegacyScopePaths() {
        assertScope("inventory/locations", "organizationLegacyId", "unitLegacyId");
        assertScope("inventory/assets", "location.organizationLegacyId", "location.unitLegacyId");
        assertScope("inventory/balances", "location.organizationLegacyId", "location.unitLegacyId");
        assertScope("inventory/movements", "location.organizationLegacyId", "location.unitLegacyId");
        assertScope("inventory/regulatory-controls",
            "asset.location.organizationLegacyId",
            "asset.location.unitLegacyId");
        assertScope("inventory/lots",
            "openingLocation.organizationLegacyId",
            "openingLocation.unitLegacyId");
        assertScope("inventory/item-values",
            "asset.location.organizationLegacyId",
            "asset.location.unitLegacyId");
        assertScope("sales", "organizationLegacyId", "unitLegacyId");
        assertScope("custodies", "organizationLegacyId", "unitLegacyId");
        assertScope("donations", "organizationLegacyId", "unitLegacyId");
        assertScope("transfers", "organizationLegacyId", "sourceUnitLegacyId");
        assertScope("disposals", "organizationLegacyId", "unitLegacyId");
        assertScope("maintenance", "organizationLegacyId", "unitLegacyId");
        assertScope("reservations", "organizationLegacyId", "unitLegacyId");
        assertScope("inventory-counts", "organizationLegacyId", "unitLegacyId");
    }

    @Test
    void ammunitionConsumptionUsesScalarScopePaths() {
        assertScope("ammunition-consumptions", "organizationLegacyId", "unitLegacyId");
    }

    private static void assertScope(
            String resource,
            String organizationPath,
            String unitPath) {
        AccessPolicy.Scope scope = AccessPolicy.scope(resource);
        assertEquals(organizationPath, scope.organizationPath());
        assertEquals(unitPath, scope.unitPath());
    }
}
