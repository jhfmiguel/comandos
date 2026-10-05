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
    private final boolean readinessOnStartup;
    private final boolean backfillOnStartup;

    public MasterDataMigrationRunner(
            MasterDataMigrationService migration,
            MasterDataParityService parity,
            @Value("${comandos.master-data.readiness-on-startup:false}")
            boolean readinessOnStartup,
            @Value("${comandos.master-data.backfill-on-startup:false}")
            boolean backfillOnStartup) {
        this.migration = migration;
        this.parity = parity;
        this.readinessOnStartup = readinessOnStartup;
        this.backfillOnStartup = backfillOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!readinessOnStartup && !backfillOnStartup) return;

        var readiness = migration.readiness();

        System.out.println(
            "[master-data-migration] ready=" + readiness.ready()
                + " issues=" + readiness.issues()
        );

        if (!backfillOnStartup) return;

        if (!readiness.ready()) {
            throw new IllegalStateException(
                "Master-data canonical backfill requested but readiness has blockers: "
                    + readiness.issues()
            );
        }

        var report = migration.backfill();

        System.out.println(
            "[master-data-migration] backfill completed: mirrored="
                + report.mirroredTotal()
                + ", skippedProductRoles="
                + report.skippedProductRoles()
                + ", details="
                + report
        );

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
                "Master-data backfill completed with canonical parity mismatches: "
                    + parityReport.mismatches()
            );
        }
    }
}
