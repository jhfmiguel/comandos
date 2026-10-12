package com.comandos.core.service;

import java.util.ArrayList;
import java.util.List;
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
        boolean readinessPassed = readinessReport.ready()
            && readinessReport.issues() != null
            && readinessReport.issues().stream().noneMatch(issue ->
                issue == null || "BLOCKER".equalsIgnoreCase(issue.severity()));
        boolean parityPassed = parityReport.consistent() && parityReport.mismatches() != null
            && parityReport.mismatches().isEmpty();
        boolean shadowWriteEnabled = mirror.enabled();
        boolean productReferenceShadowEnabled = productReferences.enabled();
        boolean productReferencePrimaryReadEnabled = productCanonicalScope.enabled();
        boolean productReferencesReady = productReferenceReport.ready()
            && productReferenceReport.incomplete() == 0
            && productReferenceReport.incompleteByEntity() != null
            && productReferenceReport.incompleteByEntity().values().stream().allMatch(count -> count != null && count == 0);
        boolean productReferenceParityPassed = productReferenceParityReport.consistent()
            && productReferenceParityReport.mismatches() != null
            && productReferenceParityReport.mismatches().isEmpty();

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

    /**
     * Returns actionable reasons for blocking bridge retirement without
     * requiring callers to interpret a compound Boolean gate.
     */
    public List<String> retirementBlockers() {
        return retirementBlockers(status());
    }

    public List<String> retirementBlockers(CutoverStatus current) {
        if (current == null) {
            return List.of("CUTOVER_STATUS_UNAVAILABLE");
        }
        List<String> blockers = new ArrayList<>();
        if (!current.readinessPassed()) blockers.add("LEGACY_READINESS_FAILED");
        if (!current.parityPassed()) blockers.add("MASTER_DATA_PARITY_FAILED");
        if (!current.shadowWriteEnabled()) blockers.add("MASTER_DATA_SHADOW_WRITE_DISABLED");
        if (!current.productReferenceShadowEnabled()) blockers.add("PRODUCT_REFERENCE_SHADOW_DISABLED");
        if (!current.productReferencePrimaryReadEnabled()) blockers.add("PRODUCT_CANONICAL_READ_DISABLED");
        if (!current.productReferencesReady()) blockers.add("PRODUCT_REFERENCE_BACKFILL_INCOMPLETE");
        if (!current.productReferenceParityPassed()) blockers.add("PRODUCT_REFERENCE_PARITY_FAILED");
        if (!current.legacyProductAssociationsRetired()) blockers.add("LEGACY_PRODUCT_ASSOCIATIONS_ACTIVE");
        if (!current.canonicalReadEnabled()) blockers.add("MASTER_DATA_CANONICAL_READ_DISABLED");
        return List.copyOf(blockers);
    }

    public CutoverStatus requireReadyToRetireLegacyPersistence() {
        CutoverStatus status = status();

        if (!status.readyToRetireLegacyPersistence()) {
            throw new IllegalStateException(
                "Legacy master-data persistence cannot be retired yet: "
                    + "blockers=" + retirementBlockers(status)
                    + ", readinessPassed=" + status.readinessPassed()
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
