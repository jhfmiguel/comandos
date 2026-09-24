package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_optical_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class OpticalSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "optical_type", nullable = false, length = 100) public String opticalType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "optical_type_id") public ArmamentParameter opticalTypeRef;
    @Column(name = "minimum_magnification", nullable = false, precision = 19, scale = 4) public BigDecimal minimumMagnification;
    @Column(name = "maximum_magnification", nullable = false, precision = 19, scale = 4) public BigDecimal maximumMagnification;
    @Column(name = "objective_diameter_mm", precision = 19, scale = 4) public BigDecimal objectiveDiameterMm;
    @Column(nullable = false, length = 150) public String reticle;
    @Column(name = "field_of_view", length = 150) public String fieldOfView;
    @Column(name = "night_vision", nullable = false) public Boolean nightVision = false;
    @Column(name = "thermal_vision", nullable = false) public Boolean thermalVision = false;
}
