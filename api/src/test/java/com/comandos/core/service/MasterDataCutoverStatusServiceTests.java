package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class MasterDataCutoverStatusServiceTests {

    @Test
    void refusesLegacyRetirementUntilAllCutoverConditionsPass() {
        var migration = mock(MasterDataMigrationService.class);
        var parity = mock(MasterDataParityService.class);
        var mirror = mock(CanonicalMasterDataMirrorService.class);

        when(migration.readiness()).thenReturn(
            new MasterDataMigrationService.ReadinessReport(true, List.of())
        );
        when(parity.verify()).thenReturn(
            new MasterDataParityService.ParityReport(true, 12, List.of())
        );
        when(mirror.enabled()).thenReturn(false);

        var service = new MasterDataCutoverStatusService(
            migration,
            parity,
            mirror,
            true
        );

        var status = service.status();

        assertTrue(status.readinessPassed());
        assertTrue(status.parityPassed());
        assertFalse(status.shadowWriteEnabled());
        assertTrue(status.canonicalReadEnabled());
        assertFalse(status.readyToRetireLegacyPersistence());

        assertThrows(
            IllegalStateException.class,
            service::requireReadyToRetireLegacyPersistence
        );
    }

    @Test
    void marksPersistenceBridgeReadyOnlyWithParityReadinessAndBothModesEnabled() {
        var migration = mock(MasterDataMigrationService.class);
        var parity = mock(MasterDataParityService.class);
        var mirror = mock(CanonicalMasterDataMirrorService.class);

        when(migration.readiness()).thenReturn(
            new MasterDataMigrationService.ReadinessReport(true, List.of())
        );
        when(parity.verify()).thenReturn(
            new MasterDataParityService.ParityReport(true, 25, List.of())
        );
        when(mirror.enabled()).thenReturn(true);

        var service = new MasterDataCutoverStatusService(
            migration,
            parity,
            mirror,
            true
        );

        var status = service.requireReadyToRetireLegacyPersistence();

        assertTrue(status.readyToRetireLegacyPersistence());
    }
}
