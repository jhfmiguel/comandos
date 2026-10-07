package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_equipment_set_operation", uniqueConstraints = {
    @UniqueConstraint(name = "uk_equipment_set_operation_request", columnNames = {"request_id"})
})
public class EquipmentSetOperation extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "equipment_set_id", nullable = false)
    public EquipmentSet equipmentSet;

    @Column(name = "organization_id", nullable = false)
    public Long organizationLegacyId;

    @Column(name = "unit_id")
    public Long unitLegacyId;

    @Column(name = "organization_canonical_id", length = 128)
    public String organizationCanonicalId;

    @Column(name = "unit_canonical_id", length = 128)
    public String unitCanonicalId;

    @Column(name = "set_code", nullable = false, length = 255)
    public String setCode;

    @Column(name = "set_name", nullable = false, length = 255)
    public String setName;

    @Column(name = "operation_type", nullable = false, length = 40)
    public String operationType;

    @Column(name = "aggregate_resource", nullable = false, length = 80)
    public String aggregateResource;

    @Column(name = "aggregate_record_ids", nullable = false, length = 2000)
    public String aggregateRecordIds;

    @Column(name = "component_count", nullable = false)
    public int componentCount;

    @Column(nullable = false, length = 30)
    public String status = "COMPLETED";

    @Column(name = "executed_at", nullable = false)
    public LocalDateTime executedAt;

    @Column(name = "operator_id")
    public Long operatorId;

    @Column(name = "operator_login", length = 255)
    public String operatorLogin;

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    public String requestId;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    public String requestFingerprint;
}
