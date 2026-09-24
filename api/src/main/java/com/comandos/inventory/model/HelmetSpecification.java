package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_helmet_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class HelmetSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "protection_level", nullable = false, length = 100) public String protectionLevel;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "protection_level_id") public ArmamentParameter protectionLevelRef;
    @Column(nullable = false, length = 100) public String material;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "material_id") public ArmamentParameter materialRef;
    @Column(name = "size_label", nullable = false, length = 50) public String size;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "size_id") public ArmamentParameter sizeRef;
    @Column(name = "weight_grams", precision = 19, scale = 4) public BigDecimal weightGrams;
    @Column(length = 150) public String certification;
}
