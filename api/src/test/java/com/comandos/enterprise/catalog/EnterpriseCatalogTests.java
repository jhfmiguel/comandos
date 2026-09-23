package com.comandos.enterprise.catalog;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EnterpriseCatalogTests {

    @Test
    void normalizesCatalogIdentityWithoutLosingPunctuation() {
        assertEquals("BERETTA APX-9", CatalogIdentity.normalize("  Beretta   APX-9 "));
        assertTrue(CatalogIdentity.same("cbc 9mm", " CBC   9MM "));
    }

    @Test
    void resolvesTrackingModesAndRejectsContradictions() {
        assertEquals(CatalogTrackingMode.SERIAL, new CatalogTrackingPolicy(true, false, false).mode());
        assertEquals(CatalogTrackingMode.LOT, new CatalogTrackingPolicy(false, true, true).mode());
        assertEquals(CatalogTrackingMode.NONE, new CatalogTrackingPolicy(false, false, false).mode());

        assertThrows(IllegalArgumentException.class, () -> new CatalogTrackingPolicy(true, true, false));
        assertThrows(IllegalArgumentException.class, () -> new CatalogTrackingPolicy(true, false, true));
        assertThrows(IllegalArgumentException.class, () -> new CatalogTrackingPolicy(false, false, true));
    }

    @Test
    void validatesAndNormalizesUnitOfMeasureCodes() {
        assertEquals("EA", new UnitOfMeasureCode(" ea ").value());
        assertTrue(new UnitOfMeasureCode("kg").isCommon());
        assertThrows(IllegalArgumentException.class, () -> new UnitOfMeasureCode(" "));
        assertThrows(IllegalArgumentException.class, () -> new UnitOfMeasureCode("unit with spaces"));
    }
}
