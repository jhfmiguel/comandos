package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_equipment_set", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"organization_id", "code"})
})
public class EquipmentSet extends CoreEntity {
    @Column(name = "organization_id", nullable = false)
    public Long organizationLegacyId;

    @Column(name = "unit_id")
    public Long unitLegacyId;

    @Column(name = "organization_canonical_id", length = 128)
    public String organizationCanonicalId;

    @Column(name = "unit_canonical_id", length = 128)
    public String unitCanonicalId;

    @Column(name = "code", nullable = false, length = 255)
    public String code;

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "description", length = 255)
    public String description;

    @Column(name = "active", nullable = false)
    public boolean active;
}
