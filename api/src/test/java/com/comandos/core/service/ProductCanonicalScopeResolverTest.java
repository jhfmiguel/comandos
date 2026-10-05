package com.comandos.core.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductCanonicalScopeResolverTest {

    @Test
    void resolvesOrganizationUnitAndPersonFromCrosswalk() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 10L))
            .thenReturn(Optional.of("org-10"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 20L))
            .thenReturn(Optional.of("unit-20"));
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 30L))
            .thenReturn(Optional.of("person-30"));

        ProductCanonicalScopeResolver resolver =
            new ProductCanonicalScopeResolver(references, true);

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
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 10L))
            .thenReturn(Optional.of("org-10"));

        ProductCanonicalScopeResolver resolver =
            new ProductCanonicalScopeResolver(references, true);

        var scope = resolver.scope(10L, null);
        assertEquals("org-10", scope.organizationId());
        assertNull(scope.unitId());
    }

    @Test
    void failsFastWhenCanonicalCrosswalkIsMissing() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 30L))
            .thenReturn(Optional.empty());

        ProductCanonicalScopeResolver resolver =
            new ProductCanonicalScopeResolver(references, true);

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> resolver.person(30L)
        );

        assertTrue(error.getMessage().contains("legacy id 30"));
    }
}
