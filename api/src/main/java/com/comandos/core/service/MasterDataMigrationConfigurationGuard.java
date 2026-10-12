package com.comandos.core.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Prevents unsafe combinations of master-data migration feature flags.
 */
@Component
public class MasterDataMigrationConfigurationGuard {

    private final boolean canonicalReadEnabled;
    private final boolean shadowWriteEnabled;

    public MasterDataMigrationConfigurationGuard(
            @Value("${comandos.master-data.shadow-write.enabled:false}")
            boolean shadowWriteEnabled,
            @Value("${comandos.master-data.canonical-read.enabled:false}")
            boolean canonicalReadEnabled) {
        this.shadowWriteEnabled = shadowWriteEnabled;
        this.canonicalReadEnabled = canonicalReadEnabled;
    }

    @PostConstruct
    void validate() {
        if (canonicalReadEnabled && !shadowWriteEnabled) {
            throw new IllegalStateException(
                "Canonical master-data reads require shadow-write to be enabled "
                    + "so legacy writes cannot drift from fm_* persistence."
            );
        }
    }
}
