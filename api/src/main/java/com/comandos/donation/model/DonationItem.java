package com.comandos.donation.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="erp_donation_item")
public class DonationItem extends CoreEntity {
    @ManyToOne(optional=false) @JoinColumn(name="donation_id", nullable=false) public Donation donation;
    @ManyToOne(optional=false) @JoinColumn(name="model_id", nullable=false) public ItemModel model;
    @ManyToOne @JoinColumn(name="asset_id") public AssetItem asset;
    @ManyToOne @JoinColumn(name="lot_id") public StockLot lot;
    @ManyToOne(optional=false) @JoinColumn(name="location_id", nullable=false) public StockLocation location;
    @OneToOne(optional=false) @JoinColumn(name="movement_id", nullable=false, unique=true) public StockMovement movement;
    @Column(nullable=false) public String modelName;
    @Column(nullable=false) public String sku;
    @Column(nullable=false) public String stockCode;
    @Column(nullable=false) public String locationName;
    @Column(nullable=false) public String unitOfMeasure;
    @Column(nullable=false, precision=19, scale=4) public BigDecimal quantity;
}
