package com.comandos.sales.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.*;
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
    @Column(name="previous_owner_type", nullable=false, length=30) public String previousOwnerType = "ORGANIZATION";
    @Column(name="previous_owner_name", nullable=false, length=255) public String previousOwnerName;
    @Column(name="new_owner_type", nullable=false, length=30) public String newOwnerType = "BUYER";
    @Column(name="new_owner_name", nullable=false, length=255) public String newOwnerName;

    @PrePersist
    @PreUpdate
    void snapshotOwnership() {
        if (sale != null) {
            if (previousOwnerName == null || previousOwnerName.isBlank()) previousOwnerName = sale.organizationName;
            if (newOwnerName == null || newOwnerName.isBlank()) newOwnerName = sale.buyerName;
        }
        if (asset != null && lot != null) throw new IllegalStateException("Sale item cannot reference asset and lot simultaneously.");
        if (asset == null && lot == null) throw new IllegalStateException("Sale item must reference an asset or lot.");
        if (movement == null || movement.quantity == null || movement.quantity.signum() >= 0) {
            throw new IllegalStateException("Sale item requires a negative stock movement.");
        }
        if ("SALE_OUT".equals(movement.nature)) movement.nature = StockMovementNature.SALE.name();
        if (!StockMovementNature.SALE.name().equals(movement.nature)) {
            throw new IllegalStateException("Sale item requires SALE stock movement.");
        }
    }
}
