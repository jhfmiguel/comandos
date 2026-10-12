package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.tenancy.api.OrganizationalUnitId;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProductCanonicalScopeResolverTests {

    private static final TenantId TENANT = TenantId.of("comandos");

    @Test
    void resolvesProductScopeFromCanonicalDirectoryWithoutCrosswalk() {
        var directory = mock(CanonicalMasterDataDirectory.class);
        var organization = mock(com.fariamiguel.enterprise.organization.Organization.class);
        var unit = mock(com.fariamiguel.tenancy.api.OrganizationalUnit.class);
        var person = mock(com.fariamiguel.enterprise.people.Person.class);

        when(organization.id()).thenReturn(BusinessId.of("comandos:organization:7"));
        when(unit.id()).thenReturn(OrganizationalUnitId.of("comandos:unit:9"));
        when(person.id()).thenReturn(BusinessId.of("comandos:person:5"));

        when(directory.findOrganization(7L, TENANT)).thenReturn(Optional.of(organization));
        when(directory.findUnit(9L, TENANT)).thenReturn(Optional.of(unit));
        when(directory.findPerson(5L, TENANT)).thenReturn(Optional.of(person));

        var resolver = new ProductCanonicalScopeResolver(directory, true);
        assertEquals("comandos:organization:7", resolver.organization(7L));
        assertEquals("comandos:person:5", resolver.person(5L));
        assertEquals("comandos:unit:9", resolver.unit(9L));
        assertEquals(
            new ProductCanonicalScopeResolver.Scope("comandos:organization:7", "comandos:unit:9"),
            resolver.scope(7L, 9L)
        );
        assertNull(resolver.unit(null));
    }

    @Test
    void failsClosedIfCanonicalRecordDoesNotExist() {
        var directory = mock(CanonicalMasterDataDirectory.class);
        when(directory.findOrganization(7L, TENANT)).thenReturn(Optional.empty());
        var resolver = new ProductCanonicalScopeResolver(directory, true);
        assertThrows(IllegalArgumentException.class, () -> resolver.organization(null));
        assertThrows(IllegalStateException.class, () -> resolver.organization(7L));
    }
}
