package com.comandos.reconciliation.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="erp_inventory_count_item")
public class InventoryCountItem extends CoreEntity {
    @ManyToOne(optional=false) @JoinColumn(name="inventory_count_id",nullable=false) public InventoryCount inventoryCount;
    @ManyToOne(optional=false) @JoinColumn(name="model_id",nullable=false) public ItemModel model;
    @ManyToOne @JoinColumn(name="asset_id") public AssetItem asset;
    @ManyToOne @JoinColumn(name="lot_id") public StockLot lot;
    @ManyToOne @JoinColumn(name="balance_id") public StockBalance balance;
    @ManyToOne @JoinColumn(name="result_type_id") public InventoryCountResultType result;
    @ManyToOne @JoinColumn(name="adjustment_movement_id") public StockMovement adjustmentMovement;
    @Column(nullable=false) public String modelName;
    @Column(nullable=false) public String sku;
    @Column(nullable=false) public String stockCode;
    @Column(nullable=false) public String unitOfMeasure;
    @Column(nullable=false,precision=19,scale=4) public BigDecimal systemQuantity;
    @Column(precision=19,scale=4) public BigDecimal countedQuantity;
    @Column(precision=19,scale=4) public BigDecimal differenceQuantity;
    @Column(length=500) public String notes;
}
