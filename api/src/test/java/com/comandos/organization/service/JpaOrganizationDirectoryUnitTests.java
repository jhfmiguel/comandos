package com.comandos.organization.service;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaOrganizationDirectoryUnitTests {
    @Test
    void returnsEmptyWhenUnitDoesNotExist() {
        var em = mock(EntityManager.class);
        when(em.find(OrganizationalUnit.class, 99L)).thenReturn(null);

        var directory = new JpaOrganizationDirectory(em);
        assertFalse(directory.findUnit(99L).isPresent());
    }

    @Test
    void mapsUnitAndOptionalParentToReusableView() {
        var organization = new Organization();
        organization.id = 10L;

        var parent = new OrganizationalUnit();
        parent.id = 20L;

        var unit = new OrganizationalUnit();
        unit.id = 21L;
        unit.organization = organization;
        unit.parentUnit = parent;
        unit.code = "OPS";
        unit.name = "Operations";
        unit.type = "Office";
        unit.active = true;

        var em = mock(EntityManager.class);
        when(em.find(OrganizationalUnit.class, 21L)).thenReturn(unit);

        var view = new JpaOrganizationDirectory(em)
            .findUnit(21L)
            .orElseThrow();

        assertEquals(21L, view.id());
        assertEquals(10L, view.organizationId());
        assertEquals(20L, view.parentUnitId());
        assertEquals("OPS", view.code());

        unit.parentUnit = null;
        assertNull(new JpaOrganizationDirectory(em).findUnit(21L).orElseThrow().parentUnitId());
    }
}
