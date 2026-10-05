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
        boolean productReferenceShadowEnabled,
        boolean productReferencePrimaryReadEnabled,
        boolean productReferencesReady,
        boolean canonicalReadEnabled,
        boolean readyToRetireLegacyPersistence,
        MasterDataMigrationService.ReadinessReport readiness,
        MasterDataParityService.ParityReport parity,
        ProductMasterDataReferenceBackfillService.ReadinessReport productReferences
    ) {}

    private final MasterDataMigrationService migration;
    private final MasterDataParityService parity;
    private final CanonicalMasterDataMirrorService mirror;
    private final ProductMasterDataReferenceSynchronizer productReferences;
    private final ProductCanonicalScopeResolver productCanonicalScope;
    private final ProductMasterDataReferenceBackfillService productReferenceBackfill;
    private final boolean canonicalReadEnabled;

    public MasterDataCutoverStatusService(
            MasterDataMigrationService migration,
            MasterDataParityService parity,
            CanonicalMasterDataMirrorService mirror,
            ProductMasterDataReferenceSynchronizer productReferences,
            ProductCanonicalScopeResolver productCanonicalScope,
            ProductMasterDataReferenceBackfillService productReferenceBackfill,
            @Value("${comandos.master-data.canonical-read.enabled:false}")
            boolean canonicalReadEnabled) {
        this.migration = migration;
        this.parity = parity;
        this.mirror = mirror;
        this.productReferences = productReferences;
        this.productCanonicalScope = productCanonicalScope;
        this.productReferenceBackfill = productReferenceBackfill;
        this.canonicalReadEnabled = canonicalReadEnabled;
    }

    public boolean canonicalReadEnabled() {
        return canonicalReadEnabled;
    }

    public CutoverStatus status() {
        var readinessReport = migration.readiness();
        var parityReport = parity.verify();

        var productReferenceReport = productReferenceBackfill.readiness();
        boolean readinessPassed = readinessReport.ready();
        boolean parityPassed = parityReport.consistent();
        boolean shadowWriteEnabled = mirror.enabled();
        boolean productReferenceShadowEnabled = productReferences.enabled();
        boolean productReferencePrimaryReadEnabled = productCanonicalScope.enabled();
        boolean productReferencesReady = productReferenceReport.ready();

        return new CutoverStatus(
            readinessPassed,
            parityPassed,
            shadowWriteEnabled,
            productReferenceShadowEnabled,
            productReferencePrimaryReadEnabled,
            productReferencesReady,
            canonicalReadEnabled,
            readinessPassed
                && parityPassed
                && shadowWriteEnabled
                && productReferenceShadowEnabled
                && productReferencePrimaryReadEnabled
                && productReferencesReady
                && canonicalReadEnabled,
            readinessReport,
            parityReport,
            productReferenceReport
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
                    + ", productReferenceShadowEnabled=" + status.productReferenceShadowEnabled()
                    + ", productReferencePrimaryReadEnabled=" + status.productReferencePrimaryReadEnabled()
                    + ", productReferencesReady=" + status.productReferencesReady()
                    + ", canonicalReadEnabled=" + status.canonicalReadEnabled()
                    + ", readinessIssues=" + status.readiness().issues()
                    + ", parityMismatches=" + status.parity().mismatches()
                    + ", productReferenceIncomplete=" + status.productReferences().incompleteByEntity()
            );
        }

        return status;
    }
}
