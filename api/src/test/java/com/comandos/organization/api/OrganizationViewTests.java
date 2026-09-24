package com.comandos.organization.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationViewTests {
    @Test
    void exposesReusableOrganizationData() {
        var organization = new OrganizationView(
            10L,
            "Public agency",
            "Public administration",
            "Example Organization",
            "EX",
            "12345678000199",
            true,
            true
        );

        assertEquals(10L, organization.id());
        assertEquals("Public administration", organization.economicActivity());
        assertEquals("Example Organization", organization.name());
        assertEquals("EX", organization.acronym());
        assertTrue(organization.publicOrganization());
        assertTrue(organization.active());

        var inactive = new OrganizationView(
            11L, "Private company", null, "Inactive", null, null, false, false
        );
        assertFalse(inactive.publicOrganization());
        assertFalse(inactive.active());
    }

    @Test
    void exposesReusableOrganizationalUnitData() {
        var unit = new OrganizationalUnitView(
            20L,
            10L,
            null,
            "HQ",
            "Headquarters",
            "Office",
            true
        );

        assertEquals(20L, unit.id());
        assertEquals(10L, unit.organizationId());
        assertNull(unit.parentUnitId());
        assertEquals("HQ", unit.code());
        assertTrue(unit.active());
    }
}
