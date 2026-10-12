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
        // An empty product domain is legitimate; when it is not empty every
        // persisted product row must contribute its expected reference checks.
        boolean productReferenceParityPassed = productReferenceParityReport.consistent()
            && productReferenceParityReport.mismatches() != null
            && productReferenceParityReport.mismatches().isEmpty()
            && (productReferenceReport.checked() == 0
                || productReferenceParityReport.checkedReferences() >= productReferenceReport.checked());

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

    public record RetirementAssessment(
        boolean ready,
        List<String> blockers,
        long legacyReadinessIssues,
        long masterDataParityMismatches,
        long productReferencesChecked,
        long productReferencesIncomplete,
        long productReferenceParityMismatches
    ) {}

    /**
     * Snapshot of the independent migration checks. Incomplete or unavailable
     * evidence must never silently authorize retiring legacy persistence.
     */
    public RetirementAssessment assessment() {
        CutoverStatus snapshot = status();
        return new RetirementAssessment(
            snapshot.readyToRetireLegacyPersistence(),
            retirementBlockers(snapshot),
            snapshot.readiness().issues() == null ? -1 : snapshot.readiness().issues().size(),
            snapshot.parity().mismatches() == null ? -1 : snapshot.parity().mismatches().size(),
            snapshot.productReferences().checked(),
            snapshot.productReferences().incomplete(),
            snapshot.productReferenceParity().mismatches() == null
                ? -1 : snapshot.productReferenceParity().mismatches().size()
        );
    }

    /**
     * Immutable operational evidence for a cutover decision. A snapshot is
     * computed once, so the reported gates and their blockers are consistent.
     * No migration or destructive operation is triggered by this method.
     */
    public record CutoverEvidence(
        boolean retirementAllowed,
        java.util.Map<String, Boolean> gates,
        java.util.List<String> blockers,
        long canonicalParityChecked,
        int canonicalParityMismatches,
        long productReferencesChecked,
        long productReferencesIncomplete,
        java.util.Map<String, Long> productReferencesIncompleteByEntity,
        long productParityChecked,
        int productParityMismatches,
        java.util.List<MasterDataMigrationService.ReadinessIssue> readinessIssues,
        java.util.List<MasterDataParityService.ParityMismatch> canonicalMismatches,
        java.util.List<ProductMasterDataReferenceParityService.Mismatch> productMismatches
    ) {}

    public CutoverEvidence evidence() {
        return evidence(status());
    }

    public CutoverEvidence evidence(CutoverStatus current) {
        if (current == null) throw new IllegalArgumentException("Cutover status is required");
        java.util.Map<String, Boolean> gates = new java.util.LinkedHashMap<>();
        gates.put("legacyReadiness", current.readinessPassed());
        gates.put("canonicalParity", current.parityPassed());
        gates.put("masterShadowWrite", current.shadowWriteEnabled());
        gates.put("productShadowWrite", current.productReferenceShadowEnabled());
        gates.put("productPrimaryRead", current.productReferencePrimaryReadEnabled());
        gates.put("productBackfill", current.productReferencesReady());
        gates.put("productParity", current.productReferenceParityPassed());
        gates.put("legacyProductAssociationsRetired", current.legacyProductAssociationsRetired());
        gates.put("canonicalRead", current.canonicalReadEnabled());

        var legacyReadiness = current.readiness();
        var legacyParity = current.parity();
        var productReadiness = current.productReferences();
        var productParity = current.productReferenceParity();
        var issues = legacyReadiness == null || legacyReadiness.issues() == null
            ? java.util.List.<MasterDataMigrationService.ReadinessIssue>of()
            : java.util.List.copyOf(legacyReadiness.issues());
        var mismatches = legacyParity == null || legacyParity.mismatches() == null
            ? java.util.List.<MasterDataParityService.ParityMismatch>of()
            : java.util.List.copyOf(legacyParity.mismatches());
        var productMismatches = productParity == null || productParity.mismatches() == null
            ? java.util.List.<ProductMasterDataReferenceParityService.Mismatch>of()
            : java.util.List.copyOf(productParity.mismatches());
        var incomplete = productReadiness == null || productReadiness.incompleteByEntity() == null
            ? java.util.Map.<String, Long>of()
            : java.util.Map.copyOf(productReadiness.incompleteByEntity());

        var blockers = retirementBlockers(current);
        return new CutoverEvidence(
            current.readyToRetireLegacyPersistence() && blockers.isEmpty(),
            java.util.Collections.unmodifiableMap(gates),
            blockers,
            legacyParity == null ? 0L : legacyParity.checked(),
            mismatches.size(),
            productReadiness == null ? 0L : productReadiness.checked(),
            productReadiness == null ? 0L : productReadiness.incomplete(),
            incomplete,
            productParity == null ? 0L : productParity.checkedReferences(),
            productMismatches.size(),
            issues,
            mismatches,
            productMismatches
        );
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
