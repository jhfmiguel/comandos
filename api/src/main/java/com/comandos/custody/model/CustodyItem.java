package com.comandos.custody.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_custody_item", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"custody_id", "asset_id"}),
    @UniqueConstraint(columnNames = {"custody_id", "balance_id", "equipment_set_id"})
})
public class CustodyItem extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "custody_id", nullable = false)
    public Custody custody;
    @ManyToOne @JoinColumn(name = "asset_id")
    public AssetItem asset;
    @ManyToOne @JoinColumn(name = "balance_id")
    public StockBalance balance;
    @ManyToOne @JoinColumn(name = "equipment_set_id")
    public EquipmentSet equipmentSet;
    @ManyToOne(optional = false) @JoinColumn(name = "location_id", nullable = false)
    public StockLocation location;
    @OneToOne(optional = false) @JoinColumn(name = "issue_movement_id", nullable = false, unique = true)
    public StockMovement issueMovement;
    @Column(nullable = false) public String modelName;
    @Column(nullable = false) public String assetCode;
    @Column public String serialNumber;
    @Column public String equipmentSetCode;
    @Column public String equipmentSetName;
    @Column public String componentRole;
    @Column(nullable = false, precision = 19, scale = 4) public java.math.BigDecimal quantity = java.math.BigDecimal.ONE;
    @Column(nullable = false) public String locationName;
    @Column(name = "delivery_condition", length = 500) public String deliveryCondition;
    @Column(name = "accessories", length = 1000) public String accessories;
    @Column public LocalDateTime returnedAt;
}
