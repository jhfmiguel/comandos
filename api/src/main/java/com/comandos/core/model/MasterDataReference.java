package com.comandos.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Durable crosswalk between legacy numeric COMANDOS master-data identifiers
 * and canonical Faria Miguel identifiers.
 *
 * <p>This table is the migration seam for product-domain foreign keys while
 * erp_* references are progressively replaced by canonical string IDs.</p>
 */
@Entity
@Table(
    name = "erp_master_data_reference",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_master_data_reference_legacy",
            columnNames = {"resource_type", "legacy_id"}
        ),
        @UniqueConstraint(
            name = "uk_master_data_reference_canonical",
            columnNames = {"resource_type", "canonical_id"}
        )
    }
)
public class MasterDataReference extends CoreEntity {

    @Column(name = "resource_type", length = 40, nullable = false)
    public String resourceType;

    @Column(name = "legacy_id", nullable = false)
    public Long legacyId;

    @Column(name = "canonical_id", length = 128, nullable = false)
    public String canonicalId;

    @Column(name = "canonical_tenant_id", length = 128, nullable = false)
    public String canonicalTenantId = "comandos";

    @Column(name = "active", nullable = false)
    public Boolean active = true;

    @Column(name = "last_synchronized_at", nullable = false)
    public LocalDateTime lastSynchronizedAt;

    @Column(name = "migration_note", length = 500)
    public String migrationNote;
}
