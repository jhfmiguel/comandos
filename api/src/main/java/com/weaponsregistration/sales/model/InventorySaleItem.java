package com.weaponsregistration.sales.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.inventory.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_sale_item")
public class InventorySaleItem extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "sale_id", nullable = false)
    public InventorySale sale;
    @ManyToOne(optional = false) @JoinColumn(name = "model_id", nullable = false)
    public ItemModel model;
    @ManyToOne @JoinColumn(name = "asset_id") public AssetItem asset;
    @ManyToOne @JoinColumn(name = "lot_id") public StockLot lot;
    @ManyToOne(optional = false) @JoinColumn(name = "location_id", nullable = false)
    public StockLocation location;
    @OneToOne(optional = false) @JoinColumn(name = "movement_id", nullable = false, unique = true)
    public StockMovement movement;
    @Column(nullable = false) public String modelName;
    @Column(nullable = false) public String sku;
    @Column(nullable = false) public String stockCode;
    @Column(nullable = false) public String locationName;
    @Column(nullable = false) public String unitOfMeasure;
    @Column(nullable = false, precision = 19, scale = 4) public BigDecimal quantity;
    @Column(nullable = false, precision = 19, scale = 4) public BigDecimal unitPrice;
    @Column(nullable = false, precision = 19, scale = 4) public BigDecimal subtotal;
}
