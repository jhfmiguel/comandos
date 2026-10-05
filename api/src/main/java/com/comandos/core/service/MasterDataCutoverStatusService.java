package com.comandos.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Objective cutover status for the last COMANDOS master-data persistence bridge.
 *
 * <p>The migration is technically ready to retire legacy persistence only when:
 * legacy data is compatible, canonical parity is zero-mismatch, shadow writes
 * are active and the canonical read path is already enabled.</p>
 */
@Service
@Transactional(readOnly = true)
public class MasterDataCutoverStatusService {

    public record CutoverStatus(
        boolean readinessPassed,
        boolean parityPassed,
        boolean shadowWriteEnabled,
        boolean canonicalReadEnabled,
        boolean readyToRetireLegacyPersistence,
        MasterDataMigrationService.ReadinessReport readiness,
        MasterDataParityService.ParityReport parity
    ) {}

    private final MasterDataMigrationService migration;
    private final MasterDataParityService parity;
    private final CanonicalMasterDataMirrorService mirror;
    private final boolean canonicalReadEnabled;

    public MasterDataCutoverStatusService(
            MasterDataMigrationService migration,
            MasterDataParityService parity,
            CanonicalMasterDataMirrorService mirror,
            @Value("${comandos.master-data.canonical-read.enabled:false}")
            boolean canonicalReadEnabled) {
        this.migration = migration;
        this.parity = parity;
        this.mirror = mirror;
        this.canonicalReadEnabled = canonicalReadEnabled;
    }

    public CutoverStatus status() {
        var readinessReport = migration.readiness();
        var parityReport = parity.verify();

        boolean readinessPassed = readinessReport.ready();
        boolean parityPassed = parityReport.consistent();
        boolean shadowWriteEnabled = mirror.enabled();

        return new CutoverStatus(
            readinessPassed,
            parityPassed,
            shadowWriteEnabled,
            canonicalReadEnabled,
            readinessPassed
                && parityPassed
                && shadowWriteEnabled
                && canonicalReadEnabled,
            readinessReport,
            parityReport
        );
    }

    public CutoverStatus requireReadyToRetireLegacyPersistence() {
        CutoverStatus status = status();

        if (!status.readyToRetireLegacyPersistence()) {
            throw new IllegalStateException(
                "Legacy master-data persistence cannot be retired yet: "
                    + "readinessPassed=" + status.readinessPassed()
                    + ", parityPassed=" + status.parityPassed()
                    + ", shadowWriteEnabled=" + status.shadowWriteEnabled()
                    + ", canonicalReadEnabled=" + status.canonicalReadEnabled()
                    + ", readinessIssues=" + status.readiness().issues()
                    + ", parityMismatches=" + status.parity().mismatches()
            );
        }

        return status;
    }
}
