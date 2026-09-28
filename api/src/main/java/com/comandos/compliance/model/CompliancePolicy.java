package com.comandos.compliance.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "erp_compliance_policy", uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "unit_id"}))
public class CompliancePolicy extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    public Organization organization;

    @ManyToOne
    @JoinColumn(name = "unit_id")
    public OrganizationalUnit unit;

    @Column(name = "expiration_warning_days", nullable = false)
    public Integer expirationWarningDays = 30;

    @Column(name = "maintenance_warning_days", nullable = false)
    public Integer maintenanceWarningDays = 30;

    @Column(name = "regulatory_warning_days", nullable = false)
    public Integer regulatoryWarningDays = 30;

    @Column(name = "inspection_interval_days", nullable = false)
    public Integer inspectionIntervalDays = 180;

    @Column(nullable = false)
    public Boolean active = true;
}
