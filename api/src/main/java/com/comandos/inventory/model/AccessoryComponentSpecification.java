package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_accessory_component_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class AccessoryComponentSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "component_type", nullable = false, length = 100) public String componentType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "component_type_id") public ArmamentParameter componentTypeRef;
    @Column(name = "compatible_with", length = 255) public String compatibleWith;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "compatibility_id") public ArmamentParameter compatibilityRef;
    @Column(name = "mounting_interface", length = 150) public String mountingInterface;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "interface_id") public ArmamentParameter interfaceRef;
    @Column(name = "controlled_component", nullable = false) public Boolean controlledComponent = false;
}
