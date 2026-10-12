package com.comandos.core.service;

import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.tenancy.api.OrganizationalUnitId;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductCanonicalScopeResolverTest {

    private static final TenantId TENANT = TenantId.of("comandos");

    @Test
    void resolvesOrganizationUnitAndPersonFromCanonicalDirectory() {
        CanonicalMasterDataDirectory directory = mock(CanonicalMasterDataDirectory.class);
        var organization = mock(com.fariamiguel.enterprise.organization.Organization.class);
        var unit = mock(com.fariamiguel.tenancy.api.OrganizationalUnit.class);
        var person = mock(com.fariamiguel.enterprise.people.Person.class);

        when(organization.id()).thenReturn(BusinessId.of("org-10"));
        when(unit.id()).thenReturn(OrganizationalUnitId.of("unit-20"));
        when(person.id()).thenReturn(BusinessId.of("person-30"));
        when(directory.findOrganization(10L, TENANT)).thenReturn(Optional.of(organization));
        when(directory.findUnit(20L, TENANT)).thenReturn(Optional.of(unit));
        when(directory.findPerson(30L, TENANT)).thenReturn(Optional.of(person));

        ProductCanonicalScopeResolver resolver = new ProductCanonicalScopeResolver(directory, true);

        assertTrue(resolver.enabled());
        assertEquals("org-10", resolver.organization(10L));
        assertEquals("unit-20", resolver.unit(20L));
        assertEquals("person-30", resolver.person(30L));

        var scope = resolver.scope(10L, 20L);
        assertEquals("org-10", scope.organizationId());
        assertEquals("unit-20", scope.unitId());
    }

    @Test
    void allowsOrganizationOnlyScope() {
        CanonicalMasterDataDirectory directory = mock(CanonicalMasterDataDirectory.class);
        var organization = mock(com.fariamiguel.enterprise.organization.Organization.class);
        when(organization.id()).thenReturn(BusinessId.of("org-10"));
        when(directory.findOrganization(10L, TENANT)).thenReturn(Optional.of(organization));

        ProductCanonicalScopeResolver resolver = new ProductCanonicalScopeResolver(directory, true);

        var scope = resolver.scope(10L, null);
        assertEquals("org-10", scope.organizationId());
        assertNull(scope.unitId());
    }

    @Test
    void failsFastWhenCanonicalRecordIsMissing() {
        CanonicalMasterDataDirectory directory = mock(CanonicalMasterDataDirectory.class);
        when(directory.findPerson(30L, TENANT)).thenReturn(Optional.empty());

        ProductCanonicalScopeResolver resolver = new ProductCanonicalScopeResolver(directory, true);

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> resolver.person(30L)
        );

        assertTrue(error.getMessage().contains("legacy id 30"));
    }
}
