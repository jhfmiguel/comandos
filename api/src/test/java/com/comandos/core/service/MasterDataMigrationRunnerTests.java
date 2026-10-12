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
    void explicitProductParityFailsClosedWhenDiscrepanciesExist() {
        when(backfill.readiness()).thenReturn(
            new ProductMasterDataReferenceBackfillService.ReadinessReport(
                true, 1, 0, java.util.Map.of()
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
