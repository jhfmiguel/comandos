package com.comandos.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
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
    void legacyJpaAssociationsAreReadOnlyCompatibilityBridges() throws Exception {
        JoinColumn organization = StockLocation.class
            .getField("organization")
            .getAnnotation(JoinColumn.class);
        JoinColumn unit = StockLocation.class
            .getField("unit")
            .getAnnotation(JoinColumn.class);

        assertNotNull(organization);
        assertFalse(organization.insertable());
        assertFalse(organization.updatable());

        assertNotNull(unit);
        assertFalse(unit.insertable());
        assertFalse(unit.updatable());
    }
}
