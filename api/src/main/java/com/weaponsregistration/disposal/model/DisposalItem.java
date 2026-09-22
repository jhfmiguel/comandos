package com.weaponsregistration.disposal.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.inventory.model.AssetItem;
import com.weaponsregistration.inventory.model.ItemModel;
import com.weaponsregistration.inventory.model.StockLocation;
import com.weaponsregistration.inventory.model.StockLot;
import com.weaponsregistration.inventory.model.StockMovement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_disposal_item")
public class DisposalItem extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "process_id", nullable = false) public DisposalProcess process;
    @ManyToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @ManyToOne @JoinColumn(name = "asset_id") public AssetItem asset;
    @ManyToOne @JoinColumn(name = "lot_id") public StockLot lot;
    @ManyToOne(optional = false) @JoinColumn(name = "location_id", nullable = false) public StockLocation location;
    @OneToOne(optional = false) @JoinColumn(name = "movement_id", nullable = false, unique = true) public StockMovement movement;
    @Column(nullable = false) public String modelName;
    @Column(nullable = false) public String sku;
    @Column(nullable = false) public String stockCode;
    @Column(nullable = false) public String locationName;
    @Column(nullable = false) public String unitOfMeasure;
    @Column(nullable = false, precision = 19, scale = 4) public BigDecimal quantity;
}
