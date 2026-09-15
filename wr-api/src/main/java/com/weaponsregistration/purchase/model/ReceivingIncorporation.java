package com.weaponsregistration.purchase.model;

import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "receiving_incorporation",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_receiving_incorporation_serial",
            columnNames = {"receiving_serial_id"}
        )
    }
)
public class ReceivingIncorporation extends CoreEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiving_id", nullable = false)
    public EquipmentReceiving receiving;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiving_item_id", nullable = false)
    public EquipmentReceivingItem receivingItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_serial_id")
    public ReceivingSerial receivingSerial;

    @Column(name = "stock_location_id", nullable = false)
    public Long stockLocationId;

    @Column(name = "asset_code", length = 100)
    public String assetCode;

    @Column(name = "lot_number", length = 120)
    public String lotNumber;

    @Column(name = "incorporated_by", length = 255)
    public String incorporatedBy;

    @Column(name = "incorporated_at", nullable = false)
    public LocalDateTime incorporatedAt;

    @Column(name = "notes", length = 2000)
    public String notes;
}