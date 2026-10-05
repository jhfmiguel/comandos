package com.comandos.inventory.model;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StockLocationMasterDataBridgeTest {

    @Test
    void scalarLegacyIdsOwnTheDatabaseColumns() throws Exception {
        Column organization = StockLocation.class
            .getField("organizationLegacyId")
            .getAnnotation(Column.class);
        Column unit = StockLocation.class
            .getField("unitLegacyId")
            .getAnnotation(Column.class);

        assertNotNull(organization);
        assertEquals("organization_id", organization.name());
        assertFalse(organization.nullable());

        assertNotNull(unit);
        assertEquals("unit_id", unit.name());
    }

    @Test
    void legacyJpaAssociationsAreRemoved() {
        assertThrows(NoSuchFieldException.class,
            () -> StockLocation.class.getField("organization"));
        assertThrows(NoSuchFieldException.class,
            () -> StockLocation.class.getField("unit"));
    }
}
