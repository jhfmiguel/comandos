package com.comandos.custody.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "erp_custody_responsibility",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_custody_responsibility_custody",
        columnNames = "custody_id"
    )
)
public class CustodyResponsibility extends CoreEntity {

    @OneToOne(optional = false)
    @JoinColumn(
        name = "custody_id",
        nullable = false,
        unique = true,
        updatable = false
    )
    public Custody custody;

    @Column(name = "responsible_person_id", nullable = false, updatable = false)
    public Long responsiblePersonLegacyId;

    @Column(
        name = "responsible_person_canonical_id",
        length = 128,
        updatable = false
    )
    public String responsiblePersonCanonicalId;

    @Column(name = "role_assignment_id", nullable = false, updatable = false)
    public Long roleAssignmentLegacyId;

    @Column(name = "responsible_person_name", nullable = false, updatable = false)
    public String responsiblePersonName;

    @Column(name = "role_code", nullable = false, length = 255, updatable = false)
    public String roleCode;

    @Column(name = "role_name", nullable = false, length = 255, updatable = false)
    public String roleName;

    @Column(name = "organization_name", nullable = false, updatable = false)
    public String organizationName;

    @Column(name = "unit_name", nullable = false, updatable = false)
    public String unitName;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    public LocalDateTime recordedAt;
}
