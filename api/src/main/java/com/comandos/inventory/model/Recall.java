package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "erp_recall",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "recall_number"})
)
public class Recall extends CoreEntity {
    @Column(name = "organization_id", nullable = false)
    public Long organizationLegacyId;

    @Column(name = "unit_id")
    public Long unitLegacyId;

    @Column(name = "organization_canonical_id", length = 128)
    public String organizationCanonicalId;

    @Column(name = "unit_canonical_id", length = 128)
    public String unitCanonicalId;

    @Column(name = "recall_number", nullable = false)
    public String number;

    @Column(nullable = false)
    public String reason;

    @Column(nullable = false)
    public String status;

    @Column(length = 255)
    public String description;
}
