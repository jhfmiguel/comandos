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
        boolean productReferenceParityPassed,
        boolean legacyProductAssociationsRetired,
        boolean canonicalReadEnabled,
        boolean readyToRetireLegacyPersistence,
        MasterDataMigrationService.ReadinessReport readiness,
        MasterDataParityService.ParityReport parity,
        ProductMasterDataReferenceBackfillService.ReadinessReport productReferences,
        ProductMasterDataReferenceParityService.ParityReport productReferenceParity
    ) {}

    private final MasterDataMigrationService migration;
    private final MasterDataParityService parity;
    private final CanonicalMasterDataMirrorService mirror;
    private final ProductMasterDataReferenceSynchronizer productReferences;
    private final ProductCanonicalScopeResolver productCanonicalScope;
    private final ProductMasterDataReferenceBackfillService productReferenceBackfill;
    private final ProductMasterDataReferenceParityService productReferenceParity;
    private final boolean legacyProductAssociationsRetired;
    private final boolean canonicalReadEnabled;

    public MasterDataCutoverStatusService(
            MasterDataMigrationService migration,
            MasterDataParityService parity,
            CanonicalMasterDataMirrorService mirror,
            ProductMasterDataReferenceSynchronizer productReferences,
            ProductCanonicalScopeResolver productCanonicalScope,
            ProductMasterDataReferenceBackfillService productReferenceBackfill,
            ProductMasterDataReferenceParityService productReferenceParity,
            @Value("${comandos.master-data.legacy-product-associations-retired:false}")
            boolean legacyProductAssociationsRetired,
            @Value("${comandos.master-data.canonical-read.enabled:false}")
            boolean canonicalReadEnabled) {
        this.migration = migration;
        this.parity = parity;
        this.mirror = mirror;
        this.productReferences = productReferences;
        this.productCanonicalScope = productCanonicalScope;
        this.productReferenceBackfill = productReferenceBackfill;
        this.productReferenceParity = productReferenceParity;
        this.legacyProductAssociationsRetired = legacyProductAssociationsRetired;
        this.canonicalReadEnabled = canonicalReadEnabled;
    }

    public boolean canonicalReadEnabled() {
        return canonicalReadEnabled;
    }

    public CutoverStatus status() {
        var readinessReport = migration.readiness();
        var parityReport = parity.verify();

        var productReferenceReport = productReferenceBackfill.readiness();
        var productReferenceParityReport = productReferenceParity.verify();
        boolean readinessPassed = readinessReport.ready();
        boolean parityPassed = parityReport.consistent();
        boolean shadowWriteEnabled = mirror.enabled();
        boolean productReferenceShadowEnabled = productReferences.enabled();
        boolean productReferencePrimaryReadEnabled = productCanonicalScope.enabled();
        boolean productReferencesReady = productReferenceReport.ready();
        boolean productReferenceParityPassed = productReferenceParityReport.consistent();

        return new CutoverStatus(
            readinessPassed,
            parityPassed,
            shadowWriteEnabled,
            productReferenceShadowEnabled,
            productReferencePrimaryReadEnabled,
            productReferencesReady,
            productReferenceParityPassed,
            legacyProductAssociationsRetired,
            canonicalReadEnabled,
            readinessPassed
                && parityPassed
                && shadowWriteEnabled
                && productReferenceShadowEnabled
                && productReferencePrimaryReadEnabled
                && productReferencesReady
                && productReferenceParityPassed
                && legacyProductAssociationsRetired
                && canonicalReadEnabled,
            readinessReport,
            parityReport,
            productReferenceReport,
            productReferenceParityReport
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
                    + ", productReferenceParityPassed=" + status.productReferenceParityPassed()
                    + ", legacyProductAssociationsRetired=" + status.legacyProductAssociationsRetired()
                    + ", canonicalReadEnabled=" + status.canonicalReadEnabled()
                    + ", readinessIssues=" + status.readiness().issues()
                    + ", parityMismatches=" + status.parity().mismatches()
                    + ", productReferenceIncomplete=" + status.productReferences().incompleteByEntity()
                    + ", productReferenceMismatches=" + status.productReferenceParity().mismatches()
            );
        }

        return status;
    }
}
