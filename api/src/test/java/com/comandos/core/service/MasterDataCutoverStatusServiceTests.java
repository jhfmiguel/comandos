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
        when(scope.enabled()).thenReturn(productPrimary);
        when(backfill.readiness()).thenReturn(
            new ProductMasterDataReferenceBackfillService.ReadinessReport(
                productReady, 12, productReady ? 0 : 1, productReady ? Map.of() : Map.of("StockLocation", 1L))
        );
        when(productParityService.verify()).thenReturn(
            new ProductMasterDataReferenceParityService.ParityReport(productParity, 12, List.of())
        );
        return new MasterDataCutoverStatusService(
            migration, parityService, mirror, productShadow, scope, backfill,
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
    void rejectsProductParityWithInsufficientReferenceCoverage() {
        var migration = mock(MasterDataMigrationService.class);
        var parity = mock(MasterDataParityService.class);
        var mirror = mock(CanonicalMasterDataMirrorService.class);
        var scope = mock(ProductCanonicalScopeResolver.class);
        var backfill = mock(ProductMasterDataReferenceBackfillService.class);
        var productParity = mock(ProductMasterDataReferenceParityService.class);
        when(migration.readiness()).thenReturn(
            new MasterDataMigrationService.ReadinessReport(true, List.of())
        );
        when(parity.verify()).thenReturn(
            new MasterDataParityService.ParityReport(true, 1, List.of())
        );
        when(mirror.enabled()).thenReturn(true);
        when(scope.enabled()).thenReturn(true);
        when(backfill.readiness()).thenReturn(
            new ProductMasterDataReferenceBackfillService.ReadinessReport(
                true, 5, 0, Map.of()
            )
        );
        when(productParity.verify()).thenReturn(
            new ProductMasterDataReferenceParityService.ParityReport(true, 0, List.of())
        );
        var gate = new MasterDataCutoverStatusService(
            migration, parity, mirror, true, scope, backfill, productParity, true, true
        );
        assertFalse(gate.status().productReferenceParityPassed());
        assertTrue(gate.retirementBlockers().contains("PRODUCT_REFERENCE_PARITY_FAILED"));
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
        when(scope.enabled()).thenReturn(true);
        when(backfill.readiness()).thenReturn(
            new ProductMasterDataReferenceBackfillService.ReadinessReport(true, 12, 1, Map.of("Purchase", 1L))
        );
        when(productParity.verify()).thenReturn(
            new ProductMasterDataReferenceParityService.ParityReport(true, 12, List.of())
        );

        var gate = new MasterDataCutoverStatusService(
            migration, parity, mirror, true, scope, backfill, productParity, true, true
        );

        assertFalse(gate.status().parityPassed());
        assertFalse(gate.status().productReferencesReady());
        assertFalse(gate.status().readyToRetireLegacyPersistence());
        assertThrows(IllegalStateException.class, gate::requireReadyToRetireLegacyPersistence);
    }

    @Test
    void blocksRetirementWhenReadinessSummaryHidesBlockingIssues() {
        var migration = mock(MasterDataMigrationService.class);
        var parityService = mock(MasterDataParityService.class);
        var mirror = mock(CanonicalMasterDataMirrorService.class);
        var scope = mock(ProductCanonicalScopeResolver.class);
        var backfill = mock(ProductMasterDataReferenceBackfillService.class);
        var referenceParity = mock(ProductMasterDataReferenceParityService.class);
        when(migration.readiness()).thenReturn(
            new MasterDataMigrationService.ReadinessReport(true, List.of(
                new MasterDataMigrationService.ReadinessIssue(
                    "DUPLICATE_PERSON_TAX_ID", "BLOCKER", 2,
                    "Conflicting canonical tax identifiers"
                )
            ))
        );
        when(parityService.verify()).thenReturn(
            new MasterDataParityService.ParityReport(true, 12, List.of())
        );
        when(mirror.enabled()).thenReturn(true);
        when(scope.enabled()).thenReturn(true);
        when(backfill.readiness()).thenReturn(
            new ProductMasterDataReferenceBackfillService.ReadinessReport(
                true, 12, 0, Map.of()
            )
        );
        when(referenceParity.verify()).thenReturn(
            new ProductMasterDataReferenceParityService.ParityReport(true, 12, List.of())
        );
        var gate = new MasterDataCutoverStatusService(
            migration, parityService, mirror, true, scope, backfill,
            referenceParity, true, true
        );
        assertFalse(gate.status().readinessPassed());
        assertFalse(gate.status().readyToRetireLegacyPersistence());
        assertTrue(gate.retirementBlockers().contains("LEGACY_READINESS_FAILED"));
    }

    @Test
    void assessmentExplainsFailedCutoverWithConsistentEvidence() {
        var gate = createGate(true, true, true, true, false, false, true, false, true);
        var snapshot = gate.assessment();
        assertFalse(snapshot.ready());
        assertTrue(snapshot.blockers().contains("PRODUCT_CANONICAL_READ_DISABLED"));
        assertTrue(snapshot.blockers().contains("PRODUCT_REFERENCE_BACKFILL_INCOMPLETE"));
        assertTrue(snapshot.blockers().contains("LEGACY_PRODUCT_ASSOCIATIONS_ACTIVE"));
        org.junit.jupiter.api.Assertions.assertEquals(12, snapshot.productReferencesChecked());
        org.junit.jupiter.api.Assertions.assertEquals(1, snapshot.productReferencesIncomplete());
        org.junit.jupiter.api.Assertions.assertEquals(0, snapshot.masterDataParityMismatches());
    }

    @Test
    void assessmentPassesWithNoBlockersAndZeroMismatches() {
        var gate = createGate(true, true, true, true, true, true, true, true, true);
        var snapshot = gate.assessment();
        assertTrue(snapshot.ready());
        assertTrue(snapshot.blockers().isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(0, snapshot.productReferenceParityMismatches());
    }

    @Test
    void passesWhenAllNineConditionsAreTrue() {
        var gate = createGate(true, true, true, true, true, true, true, true, true);
        assertTrue(gate.requireReadyToRetireLegacyPersistence().readyToRetireLegacyPersistence());
    }

    @Test
    void evidenceIncludesAllNineFlagsAndParityCounts() {
        var gate = createGate(true, true, true, true, true, true, true, true, true);
        var evidence = gate.evidence();
        assertTrue(evidence.retirementAllowed());
        org.junit.jupiter.api.Assertions.assertEquals(9, evidence.gates().size());
        assertTrue(evidence.gates().values().stream().allMatch(Boolean::booleanValue));
        assertTrue(evidence.blockers().isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(12L, evidence.canonicalParityChecked());
        org.junit.jupiter.api.Assertions.assertEquals(12L, evidence.productParityChecked());
        org.junit.jupiter.api.Assertions.assertEquals(12L, evidence.productReferencesChecked());
        org.junit.jupiter.api.Assertions.assertEquals(0, evidence.canonicalParityMismatches());
        org.junit.jupiter.api.Assertions.assertEquals(0, evidence.productParityMismatches());
        org.junit.jupiter.api.Assertions.assertEquals(0L, evidence.productReferencesIncomplete());
    }

    @Test
    void evidenceReportsAllUnmetGatesWithTheirExplicitBlockers() {
        var gate = createGate(false, false, false, false, false, false, false, false, false);
        var evidence = gate.evidence();
        assertFalse(evidence.retirementAllowed());
        org.junit.jupiter.api.Assertions.assertEquals(9, evidence.gates().size());
        assertTrue(evidence.gates().values().stream().noneMatch(Boolean::booleanValue));
        org.junit.jupiter.api.Assertions.assertEquals(9, evidence.blockers().size());
        org.junit.jupiter.api.Assertions.assertEquals(1L, evidence.productReferencesIncomplete());
        org.junit.jupiter.api.Assertions.assertEquals(Map.of("StockLocation", 1L),
            evidence.productReferencesIncompleteByEntity());
    }

    @Test
    void evidenceIsImmutableAndFailsClosedOnMissingStatus() {
        var gate = createGate(true, true, true, true, true, true, true, true, true);
        var evidence = gate.evidence();
        assertThrows(UnsupportedOperationException.class,
            () -> evidence.gates().put("canonicalRead", false));
        assertThrows(UnsupportedOperationException.class,
            () -> evidence.blockers().add("FAKE"));
        assertThrows(IllegalArgumentException.class, () -> gate.evidence(null));
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
