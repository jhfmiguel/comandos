package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_shield_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class ShieldSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "shield_type", nullable = false, length = 100) public String shieldType;
    @Column(name = "protection_level", length = 100) public String protectionLevel;
    @Column(nullable = false, length = 100) public String material;
    @Column(name = "height_mm", precision = 19, scale = 4) public BigDecimal heightMm;
    @Column(name = "width_mm", precision = 19, scale = 4) public BigDecimal widthMm;
    @Column(name = "weight_grams", precision = 19, scale = 4) public BigDecimal weightGrams;
}
