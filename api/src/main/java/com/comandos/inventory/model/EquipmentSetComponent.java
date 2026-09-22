package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_equipment_set_component", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"equipment_set_id", "asset_id"}),
    @UniqueConstraint(columnNames = {"equipment_set_id", "balance_id"})
})
public class EquipmentSetComponent extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "equipment_set_id", nullable = false)
    public EquipmentSet equipmentSet;
    @ManyToOne
    @JoinColumn(name = "asset_id")
    public AssetItem asset;
    @ManyToOne
    @JoinColumn(name = "balance_id")
    public StockBalance balance;
    @Column(name = "role", nullable = false, length = 255)
    public String role;
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal quantity;
}
