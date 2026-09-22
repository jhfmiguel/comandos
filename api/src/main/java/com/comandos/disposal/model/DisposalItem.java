package com.comandos.disposal.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.ItemModel;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.StockLot;
import com.comandos.inventory.model.StockMovement;
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
