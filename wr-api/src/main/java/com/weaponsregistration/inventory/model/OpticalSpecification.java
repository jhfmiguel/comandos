package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_optical_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class OpticalSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "optical_type", nullable = false, length = 100) public String opticalType;
    @Column(name = "maximum_magnification", nullable = false, precision = 19, scale = 4) public BigDecimal maximumMagnification;
    @Column(name = "night_vision", nullable = false) public Boolean nightVision = false;
    @Column(name = "thermal_vision", nullable = false) public Boolean thermalVision = false;
}
