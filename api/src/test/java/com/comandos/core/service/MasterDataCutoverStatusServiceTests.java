package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MasterDataCutoverStatusServiceTests {

    private static MasterDataCutoverStatusService createGate(
            boolean readiness,
            boolean parity,
            boolean masterShadow,
            boolean productShadow,
            boolean productPrimary,
            boolean productReady,
            boolean productParity,
            boolean associationsRetired,
            boolean canonicalRead) {
        var migration = mock(MasterDataMigrationService.class);
        var parityService = mock(MasterDataParityService.class);
        var mirror = mock(CanonicalMasterDataMirrorService.class);
        var synchronizer = mock(ProductMasterDataReferenceSynchronizer.class);
        var scope = mock(ProductCanonicalScopeResolver.class);
        var backfill = mock(ProductMasterDataReferenceBackfillService.class);
        var productParityService = mock(ProductMasterDataReferenceParityService.class);

        when(migration.readiness()).thenReturn(
            new MasterDataMigrationService.ReadinessReport(readiness, List.of())
        );
        when(parityService.verify()).thenReturn(
            new MasterDataParityService.ParityReport(parity, 12, List.of())
        );
        when(mirror.enabled()).thenReturn(masterShadow);
        when(synchronizer.enabled()).thenReturn(productShadow);
        when(scope.enabled()).thenReturn(productPrimary);
        when(backfill.readiness()).thenReturn(
            new ProductMasterDataReferenceBackfillService.ReadinessReport(
                productReady, 12, productReady ? 0 : 1, productReady ? Map.of() : Map.of("StockLocation", 1L))
        );
        when(productParityService.verify()).thenReturn(
            new ProductMasterDataReferenceParityService.ParityReport(productParity, 12, List.of())
        );
        return new MasterDataCutoverStatusService(
            migration, parityService, mirror, synchronizer, scope, backfill,
            productParityService, associationsRetired, canonicalRead
        );
    }

    @Test
    void reportsEveryBlockingGateWithStableActionableCodes() {
        var gate = createGate(false, false, false, false, false, false, false, false, false);
        var blockers = gate.retirementBlockers();
        assertTrue(blockers.contains("LEGACY_READINESS_FAILED"));
        assertTrue(blockers.contains("MASTER_DATA_PARITY_FAILED"));
        assertTrue(blockers.contains("MASTER_DATA_SHADOW_WRITE_DISABLED"));
        assertTrue(blockers.contains("PRODUCT_REFERENCE_SHADOW_DISABLED"));
        assertTrue(blockers.contains("PRODUCT_CANONICAL_READ_DISABLED"));
        assertTrue(blockers.contains("PRODUCT_REFERENCE_BACKFILL_INCOMPLETE"));
        assertTrue(blockers.contains("PRODUCT_REFERENCE_PARITY_FAILED"));
        assertTrue(blockers.contains("LEGACY_PRODUCT_ASSOCIATIONS_ACTIVE"));
        assertTrue(blockers.contains("MASTER_DATA_CANONICAL_READ_DISABLED"));
        org.junit.jupiter.api.Assertions.assertEquals(9, blockers.size());
    }

    @Test
    void providesNoRetirementBlockersWhenAllNineGatesPass() {
        var gate = createGate(true, true, true, true, true, true, true, true, true);
        assertTrue(gate.retirementBlockers().isEmpty());
        assertTrue(gate.requireReadyToRetireLegacyPersistence().readyToRetireLegacyPersistence());
    }

    @Test
    void rejectsUnavailableCutoverEvidence() {
        var gate = createGate(true, true, true, true, true, true, true, true, true);
        org.junit.jupiter.api.Assertions.assertEquals(
            List.of("CUTOVER_STATUS_UNAVAILABLE"),
            gate.retirementBlockers(null)
        );
    }

    @Test
    void rejectsInconsistentReportsEvenWhenTheirSummaryFlagsClaimSuccess() {
        var migration = mock(MasterDataMigrationService.class);
        var parity = mock(MasterDataParityService.class);
        var mirror = mock(CanonicalMasterDataMirrorService.class);
        var synchronizer = mock(ProductMasterDataReferenceSynchronizer.class);
        var scope = mock(ProductCanonicalScopeResolver.class);
        var backfill = mock(ProductMasterDataReferenceBackfillService.class);
        var productParity = mock(ProductMasterDataReferenceParityService.class);

        when(migration.readiness()).thenReturn(
            new MasterDataMigrationService.ReadinessReport(true, List.of())
        );
        when(parity.verify()).thenReturn(
            new MasterDataParityService.ParityReport(true, 12, List.of(
                new MasterDataParityService.ParityMismatch("person", "1", "canonical-1", "mismatch")
            ))
        );
        when(mirror.enabled()).thenReturn(true);
        when(synchronizer.enabled()).thenReturn(true);
        when(scope.enabled()).thenReturn(true);
        when(backfill.readiness()).thenReturn(
            new ProductMasterDataReferenceBackfillService.ReadinessReport(true, 12, 1, Map.of("Purchase", 1L))
        );
        when(productParity.verify()).thenReturn(
            new ProductMasterDataReferenceParityService.ParityReport(true, 12, List.of())
        );

        var gate = new MasterDataCutoverStatusService(
            migration, parity, mirror, synchronizer, scope, backfill, productParity, true, true
        );

        assertFalse(gate.status().parityPassed());
        assertFalse(gate.status().productReferencesReady());
        assertFalse(gate.status().readyToRetireLegacyPersistence());
        assertThrows(IllegalStateException.class, gate::requireReadyToRetireLegacyPersistence);
    }

    @Test
    void passesWhenAllNineConditionsAreTrue() {
        var gate = createGate(true, true, true, true, true, true, true, true, true);
        assertTrue(gate.requireReadyToRetireLegacyPersistence().readyToRetireLegacyPersistence());
    }

    @Test
    void blocksRetirementWhenAnyRequiredConditionIsFalse() {
        for (int failing = 0; failing < 9; failing++) {
            boolean[] values = {true, true, true, true, true, true, true, true, true};
            values[failing] = false;
            var gate = createGate(values[0], values[1], values[2], values[3], values[4],
                values[5], values[6], values[7], values[8]);
            assertFalse(gate.status().readyToRetireLegacyPersistence(),
                "Condition " + failing + " must block retirement");
            assertThrows(IllegalStateException.class, gate::requireReadyToRetireLegacyPersistence);
        }
    }
}
