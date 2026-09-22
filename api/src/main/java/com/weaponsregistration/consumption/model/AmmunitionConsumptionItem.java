package com.weaponsregistration.consumption.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.inventory.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_ammunition_consumption_item")
public class AmmunitionConsumptionItem extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "consumption_id", nullable = false) public AmmunitionConsumption consumption;
    @ManyToOne(optional = false) @JoinColumn(name = "lot_id", nullable = false) public StockLot lot;
    @ManyToOne(optional = false) @JoinColumn(name = "balance_id", nullable = false) public StockBalance balance;
    @ManyToOne(optional = false) @JoinColumn(name = "location_id", nullable = false) public StockLocation location;
    @OneToOne(optional = false) @JoinColumn(name = "movement_id", nullable = false, unique = true) public StockMovement movement;
    @Column(nullable = false) public String modelName;
    @Column(nullable = false) public String sku;
    @Column(nullable = false) public String lotNumber;
    @Column(nullable = false) public String locationName;
    @Column(nullable = false) public String unitOfMeasure;
    @Column(nullable = false, precision = 19, scale = 4) public BigDecimal quantity;
    @Column(name = "delivered_quantity", nullable = false, precision = 19, scale = 4) public BigDecimal deliveredQuantity;
    @Column(name = "used_quantity", nullable = false, precision = 19, scale = 4) public BigDecimal usedQuantity;
    @Column(name = "returned_quantity", nullable = false, precision = 19, scale = 4) public BigDecimal returnedQuantity;
    @Column(nullable = false, length = 255) public String result;
}
