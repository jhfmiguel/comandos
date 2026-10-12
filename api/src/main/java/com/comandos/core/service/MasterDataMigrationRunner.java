package com.comandos.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Opt-in operational runner for the canonical master-data persistence migration.
 *
 * <p>Disabled by default. Enable only in a controlled maintenance/homologation
 * run after reviewing the readiness report.</p>
 */
@Component
@Order(250)
public class MasterDataMigrationRunner implements ApplicationRunner {

    private final MasterDataMigrationService migration;
    private final MasterDataParityService parity;
    private final MasterDataCutoverStatusService cutoverStatus;
    private final ProductMasterDataReferenceBackfillService productReferenceBackfill;
    private final ProductMasterDataReferenceParityService productReferenceParity;
    private final boolean readinessOnStartup;
    private final boolean backfillOnStartup;
    private final boolean parityOnStartup;
    private final boolean productReferenceBackfillOnStartup;
    private final boolean retirementValidationOnStartup;

    public MasterDataMigrationRunner(
            MasterDataMigrationService migration,
            MasterDataParityService parity,
            MasterDataCutoverStatusService cutoverStatus,
            ProductMasterDataReferenceBackfillService productReferenceBackfill,
            ProductMasterDataReferenceParityService productReferenceParity,
            @Value("${comandos.master-data.readiness-on-startup:false}")
            boolean readinessOnStartup,
            @Value("${comandos.master-data.backfill-on-startup:false}")
            boolean backfillOnStartup,
            @Value("${comandos.master-data.parity-on-startup:false}")
            boolean parityOnStartup,
            @Value("${comandos.master-data.product-reference-backfill-on-startup:false}")
            boolean productReferenceBackfillOnStartup,
            @Value("${comandos.master-data.retirement-validation-on-startup:false}")
            boolean retirementValidationOnStartup) {
        this.migration = migration;
        this.parity = parity;
        this.cutoverStatus = cutoverStatus;
        this.productReferenceBackfill = productReferenceBackfill;
        this.productReferenceParity = productReferenceParity;
        this.readinessOnStartup = readinessOnStartup;
        this.backfillOnStartup = backfillOnStartup;
        this.parityOnStartup = parityOnStartup;
        this.productReferenceBackfillOnStartup = productReferenceBackfillOnStartup;
        this.retirementValidationOnStartup = retirementValidationOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Startup operations are permitted only by their explicit opt-in flags.
        // Canonical runtime reads never cause migration or retirement validation.
        if (retirementValidationOnStartup
                && (backfillOnStartup || productReferenceBackfillOnStartup)) {
            throw new IllegalStateException(
                "Retirement validation cannot run with mutating backfill startup flags."
            );
        }
        if (!readinessOnStartup
                && !backfillOnStartup
                && !parityOnStartup
                && !productReferenceBackfillOnStartup
                && !retirementValidationOnStartup) {
            return;
        }

        if (readinessOnStartup || backfillOnStartup || retirementValidationOnStartup) {
            var readiness = migration.readiness();
            System.out.println(
                "[master-data-migration] ready=" + readiness.ready()
                    + " issues=" + readiness.issues()
            );
            if (backfillOnStartup && !readiness.ready()) {
                throw new IllegalStateException(
                    "Master-data canonical backfill requested but readiness has blockers: "
                        + readiness.issues()
                );
            }
        }

        // A retirement check must not start with incomplete product references.
        // Backfill can legitimately be requested to repair that state.
        if (retirementValidationOnStartup && !productReferenceBackfillOnStartup) {
            requireProductReferenceReadiness();
        }

        if (backfillOnStartup) {
            var report = migration.backfill();

            System.out.println(
                "[master-data-migration] backfill completed: mirrored="
                    + report.mirroredTotal()
                    + ", skippedProductRoles="
                    + report.skippedProductRoles()
                    + ", details="
                    + report
            );

            requireParity();
        } else if (parityOnStartup) {
            requireParity();
        }

        // Product-table backfill resolves IDs through the canonical directory.
        // It is an explicit operation and runs after master-data backfill, if selected.
        if (productReferenceBackfillOnStartup) {
            var productReport = productReferenceBackfill.backfill();
            System.out.println(
                "[master-data-migration] product-reference backfill completed: scanned="
                    + productReport.scanned()
                    + ", synchronized="
                    + productReport.synchronizedCount()
                    + ", details="
                    + productReport.synchronizedByEntity()
            );
            requireProductReferenceReadiness();
        }

        if (productReferenceBackfillOnStartup || parityOnStartup
                || retirementValidationOnStartup) {
            var productParityReport = productReferenceParity.verify();
            System.out.println(
                "[master-data-migration] product-reference parity consistent="
                    + productParityReport.consistent()
                    + ", checked="
                    + productParityReport.checkedReferences()
                    + ", mismatches="
                    + productParityReport.mismatches()
            );
            if (!productParityReport.consistent()) {
                throw new IllegalStateException(
                    "Product canonical reference parity mismatch: "
                        + productParityReport.mismatches()
                );
            }
        }

        // Canonical reads are allowed during staged migration. Full retirement
        // validation is explicitly opt-in and must not be triggered solely
        // by enabling the canonical read feature flag.
        if (retirementValidationOnStartup) {
            cutoverStatus.requireReadyToRetireLegacyPersistence();
            System.out.println(
                "[master-data-migration] legacy persistence retirement gate=PASS"
            );
        }
    }

    private void requireProductReferenceReadiness() {
        var readiness = productReferenceBackfill.readiness();
        System.out.println(
            "[master-data-migration] product-reference-ready="
                + readiness.ready() + " incomplete=" + readiness.incompleteByEntity()
        );
        if (!readiness.ready() || readiness.incomplete() != 0
                || readiness.incompleteByEntity() == null
                || readiness.incompleteByEntity().values().stream()
                    .anyMatch(count -> count == null || count != 0)) {
            throw new IllegalStateException(
                "Product canonical references remain incomplete: " + readiness
            );
        }
    }

    private void requireParity() {
        var parityReport = parity.verify();

        System.out.println(
            "[master-data-migration] parity consistent="
                + parityReport.consistent()
                + ", checked="
                + parityReport.checked()
                + ", mismatches="
                + parityReport.mismatches()
        );

        if (!parityReport.consistent()) {
            throw new IllegalStateException(
                "Master-data canonical parity mismatch: "
                    + parityReport.mismatches()
            );
        }
    }
}
