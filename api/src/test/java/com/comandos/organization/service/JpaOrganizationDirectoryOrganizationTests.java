package com.comandos.organization.service;

import com.comandos.core.model.Organization;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaOrganizationDirectoryOrganizationTests {
    @Test
    void returnsEmptyWhenOrganizationDoesNotExist() {
        var em = mock(EntityManager.class);
        when(em.find(Organization.class, 99L)).thenReturn(null);

        var directory = new JpaOrganizationDirectory(em);
        assertFalse(directory.findOrganization(99L).isPresent());
    }

    @Test
    void mapsOrganizationToReusableView() {
        var organization = new Organization();
        organization.id = 10L;
        organization.nature = "ORGANIZATION";
        organization.name = "Example Organization";
        organization.acronym = "EX";
        organization.taxId = "12345678000199";
        organization.publicOrganization = true;
        organization.active = true;

        var em = mock(EntityManager.class);
        when(em.find(Organization.class, 10L)).thenReturn(organization);

        var view = new JpaOrganizationDirectory(em)
            .findOrganization(10L)
            .orElseThrow();

        assertEquals(10L, view.id());
        assertEquals("Example Organization", view.name());
        assertEquals("EX", view.acronym());
        assertEquals(true, view.publicOrganization());
        assertEquals(true, view.active());
    }
}
