package com.comandos.maintenance.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "erp_maintenance_plan")
public class MaintenancePlan extends CoreEntity {
    @Column(name = "organization_id", nullable = false)
    public Long organizationLegacyId;

    @Column(name = "unit_id")
    public Long unitLegacyId;

    @Column(name = "organization_canonical_id", length = 128)
    public String organizationCanonicalId;

    @Column(name = "unit_canonical_id", length = 128)
    public String unitCanonicalId;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public Integer periodicityDays;

    @Column(nullable = false)
    public Boolean active = true;
}
