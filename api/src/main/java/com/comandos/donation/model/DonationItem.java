package com.comandos.donation.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Locale;

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
    @Column(name="previous_owner_type",nullable=false,length=30) public String previousOwnerType;
    @Column(name="previous_owner_name",nullable=false,length=255) public String previousOwnerName;
    @Column(name="new_owner_type",nullable=false,length=30) public String newOwnerType;
    @Column(name="new_owner_name",nullable=false,length=255) public String newOwnerName;

    @PrePersist
    void ownershipDefaults() {
        if (donation == null) return;
        String direction = donation.direction == null ? "OUTGOING" : donation.direction.trim().toUpperCase(Locale.ROOT);
        if ("INCOMING".equals(direction)) {
            if (previousOwnerType == null) previousOwnerType = "DONOR";
            if (previousOwnerName == null) previousOwnerName = donation.donorName;
            if (newOwnerType == null) newOwnerType = "ORGANIZATION";
            if (newOwnerName == null) newOwnerName = donation.organizationName;
        } else {
            if (previousOwnerType == null) previousOwnerType = "ORGANIZATION";
            if (previousOwnerName == null) previousOwnerName = donation.organizationName;
            if (newOwnerType == null) newOwnerType = "DONEE";
            if (newOwnerName == null) newOwnerName = donation.doneeName;
        }
    }
}
