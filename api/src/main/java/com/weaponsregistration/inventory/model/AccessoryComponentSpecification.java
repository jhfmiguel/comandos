package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_accessory_component_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class AccessoryComponentSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "component_type", nullable = false, length = 100) public String componentType;
    @Column(name = "compatible_with", length = 255) public String compatibleWith;
    @Column(name = "mounting_interface", length = 150) public String mountingInterface;
    @Column(name = "controlled_component", nullable = false) public Boolean controlledComponent = false;
}
