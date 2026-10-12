package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fariamiguel.tenancy.api.TenantId;
import org.junit.jupiter.api.Test;

class ProductScopeInputValidationTest {
    @Test void rejectsNonPositiveIdentifiersBeforeCanonicalQueries() {
        var directory = mock(CanonicalMasterDataDirectory.class);
        var resolver = new ProductCanonicalScopeResolver(directory, true);
        assertThrows(IllegalArgumentException.class, () -> resolver.organization(0L));
        assertThrows(IllegalArgumentException.class, () -> resolver.person(-1L));
        assertThrows(IllegalArgumentException.class, () -> resolver.unit(0L));
        assertNull(resolver.unit(null));
        assertFalse(resolver.persistedCanonicalExists("PERSON", -1L));
        verifyNoInteractions(directory);
    }
}
