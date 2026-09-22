package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_helmet_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class HelmetSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "protection_level", nullable = false, length = 100) public String protectionLevel;
    @Column(nullable = false, length = 100) public String material;
    @Column(nullable = false, length = 50) public String size;
    @Column(name = "weight_grams", precision = 19, scale = 4) public BigDecimal weightGrams;
    @Column(length = 150) public String certification;
}
