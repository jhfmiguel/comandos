package com.comandos.core.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MasterDataMigrationRunnerTests {

    private final MasterDataMigrationService migration = mock(MasterDataMigrationService.class);
    private final MasterDataParityService parity = mock(MasterDataParityService.class);
    private final MasterDataCutoverStatusService cutover = mock(MasterDataCutoverStatusService.class);
    private final ProductMasterDataReferenceBackfillService backfill =
        mock(ProductMasterDataReferenceBackfillService.class);
    private final ProductMasterDataReferenceParityService productParity =
        mock(ProductMasterDataReferenceParityService.class);

    private MasterDataMigrationRunner runner(boolean readiness, boolean backfillLegacy,
                                             boolean parityLegacy, boolean backfillProducts,
                                             boolean retirement) {
        return new MasterDataMigrationRunner(
            migration, parity, cutover, backfill, productParity,
            readiness, backfillLegacy, parityLegacy, backfillProducts, retirement
        );
    }

    @Test
    void retirementCannotApproveTheSameStartupThatMutatesLegacyOrCanonicalData() {
        assertThrows(IllegalStateException.class,
            () -> runner(false, true, false, false, true)
                .run(new DefaultApplicationArguments()));
        assertThrows(IllegalStateException.class,
            () -> runner(false, false, false, true, true)
                .run(new DefaultApplicationArguments()));
        verifyNoInteractions(migration, parity, cutover, backfill, productParity);
    }

    @Test
    void canonicalRuntimeDoesNotTriggerMigrationWhenAllOperationalFlagsAreDisabled() {
        when(cutover.canonicalReadEnabled()).thenReturn(true);
        runner(false, false, false, false, false).run(new DefaultApplicationArguments());
        verifyNoInteractions(migration, parity, cutover, backfill, productParity);
    }

    @Test
    void explicitReadinessDoesNotExecuteParityOrBackfill() {
        when(migration.readiness()).thenReturn(
            new MasterDataMigrationService.ReadinessReport(true, java.util.List.of())
        );
        runner(true, false, false, false, false).run(new DefaultApplicationArguments());
        verify(migration).readiness();
        verifyNoInteractions(parity, cutover, backfill, productParity);
    }

    @Test
    void masterDataBackfillDoesNotPrematurelyCheckProductParity() {
        when(migration.readiness()).thenReturn(
            new MasterDataMigrationService.ReadinessReport(true, java.util.List.of())
        );
        when(migration.backfill()).thenReturn(
            new MasterDataMigrationService.BackfillReport(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
        );
        when(parity.verify()).thenReturn(
            new MasterDataParityService.ParityReport(true, 0, java.util.List.of())
        );
        runner(false, true, false, false, false).run(new DefaultApplicationArguments());
        verify(migration).backfill();
        verify(parity).verify();
        verifyNoInteractions(backfill, productParity);
    }

    @Test
    void productBackfillRejectsIncompleteReferencesBeforeParity() {
        when(backfill.backfill()).thenReturn(
            new ProductMasterDataReferenceBackfillService.BackfillReport(
                2, 1, java.util.Map.of("Purchase", 1L)
            )
        );
        when(backfill.readiness()).thenReturn(
            new ProductMasterDataReferenceBackfillService.ReadinessReport(
                false, 2, 1, java.util.Map.of("Purchase", 1L)
            )
        );
        assertThrows(IllegalStateException.class,
            () -> runner(false, false, false, true, false)
                .run(new DefaultApplicationArguments()));
        verifyNoInteractions(productParity);
    }

    @Test
    void retirementRejectsIncompleteProductReferencesWithoutRunningBackfill() {
        when(migration.readiness()).thenReturn(
            new MasterDataMigrationService.ReadinessReport(true, java.util.List.of())
        );
        when(backfill.readiness()).thenReturn(
            new ProductMasterDataReferenceBackfillService.ReadinessReport(
                false, 2, 1, java.util.Map.of("Custody", 1L)
            )
        );
        assertThrows(IllegalStateException.class,
            () -> runner(false, false, false, false, true)
                .run(new DefaultApplicationArguments()));
        verify(backfill, never()).backfill();
        verifyNoInteractions(productParity);
    }

    @Test
    void explicitProductParityFailsClosedWhenDiscrepanciesExist() {
        when(backfill.readiness()).thenReturn(
            new ProductMasterDataReferenceBackfillService.ReadinessReport(
                true, 1, 0, java.util.Map.of()
            )
        );
        when(backfill.backfill()).thenReturn(
            new ProductMasterDataReferenceBackfillService.BackfillReport(
                1, 1, java.util.Map.of()
            )
        );
        when(productParity.verify()).thenReturn(
            new ProductMasterDataReferenceParityService.ParityReport(
                false, 1, java.util.List.of()
            )
        );
        assertThrows(IllegalStateException.class,
            () -> runner(false, false, false, true, false)
                .run(new DefaultApplicationArguments()));
    }
}
