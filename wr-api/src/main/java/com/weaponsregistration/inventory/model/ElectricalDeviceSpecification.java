package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_electrical_device_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class ElectricalDeviceSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(nullable = false, precision = 19, scale = 4) public BigDecimal voltage;
    @Column(nullable = false) public Integer cycles;
    @Column(name = "cartridge_type", nullable = false, length = 100) public String cartridgeType;
}
