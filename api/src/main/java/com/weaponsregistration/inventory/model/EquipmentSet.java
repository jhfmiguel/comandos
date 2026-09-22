package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_equipment_set", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"organization_id", "code"})
})
public class EquipmentSet extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    public Organization organization;
    @ManyToOne
    @JoinColumn(name = "unit_id")
    public OrganizationalUnit unit;
    @Column(name = "code", nullable = false, length = 255)
    public String code;
    @Column(name = "name", nullable = false, length = 255)
    public String name;
    @Column(name = "description", length = 255)
    public String description;
    @Column(name = "active", nullable = false)
    public boolean active;
}
