package com.comandos.core.service;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.StockMovement;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductMasterDataReferenceSynchronizerTest {

    @Test
    void synchronizesOrganizationAndUnitForStockLocation() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 10L))
            .thenReturn(Optional.of("org-canonical-10"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 20L))
            .thenReturn(Optional.of("unit-canonical-20"));

        Organization organization = new Organization();
        organization.id = 10L;
        OrganizationalUnit unit = new OrganizationalUnit();
        unit.id = 20L;

        StockLocation location = new StockLocation();
        location.organization = organization;
        location.unit = unit;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(location));
        assertEquals("org-canonical-10", location.organizationCanonicalId);
        assertEquals("unit-canonical-20", location.unitCanonicalId);
    }

    @Test
    void clearsUnitCanonicalIdWhenLocationHasNoUnit() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 10L))
            .thenReturn(Optional.of("org-canonical-10"));

        Organization organization = new Organization();
        organization.id = 10L;

        StockLocation location = new StockLocation();
        location.organization = organization;
        location.unitCanonicalId = "stale-unit";

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(location));
        assertEquals("org-canonical-10", location.organizationCanonicalId);
        assertNull(location.unitCanonicalId);
    }

    @Test
    void failsWhenCanonicalOrganizationReferenceIsMissing() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 10L))
            .thenReturn(Optional.empty());

        Organization organization = new Organization();
        organization.id = 10L;

        StockLocation location = new StockLocation();
        location.organization = organization;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> synchronizer.synchronize(location)
        );

        assertTrue(error.getMessage().contains("legacy id 10"));
    }

    @Test
    void ignoresIndirectInventoryEntities() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertFalse(synchronizer.synchronize(new StockMovement()));
        verifyNoInteractions(references);
    }

    @Test
    void runtimeFlagCanDisableSynchronization() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, false);

        StockLocation location = new StockLocation();

        assertFalse(synchronizer.synchronize(location));
        verifyNoInteractions(references);
    }
}
