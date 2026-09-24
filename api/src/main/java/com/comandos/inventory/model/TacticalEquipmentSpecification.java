package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_tactical_equipment_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class TacticalEquipmentSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "equipment_type", nullable = false, length = 100) public String equipmentType;
    @Column(length = 100) public String material;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "material_id") public ArmamentParameter materialRef;
    @Column(name = "size_label", length = 50) public String size;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "size_id") public ArmamentParameter sizeRef;
    @Column(name = "weight_grams", precision = 19, scale = 4) public BigDecimal weightGrams;
    @Column(name = "operational_notes", length = 500) public String operationalNotes;
}
