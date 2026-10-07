package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_asset_item", uniqueConstraints = {
    @UniqueConstraint(name = "uk_asset_item_asset_code", columnNames = {"asset_code"}),
    @UniqueConstraint(name = "uk_asset_item_internal_code", columnNames = {"internal_code"}),
    @UniqueConstraint(name = "uk_asset_item_serial_number", columnNames = {"serial_number"})
})
public class AssetItem extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    public ItemModel model;
    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    public StockLocation location;
    @Column(name = "asset_code", nullable = false, updatable = false, length = 255)
    public String assetCode;
    @Column(name = "serial_number", nullable = true, updatable = false, length = 255)
    public String serialNumber;
    @Column(name = "internal_code", nullable = true, updatable = false, length = 255)
    public String internalCode;
    @Column(name = "condition", nullable = false, length = 255)
    public String condition;
    @Column(name = "status", nullable = false, length = 255)
    public String status;
    @Column(name = "valid_until", nullable = true)
    public LocalDate validUntil;
    @Column(name = "current_value", nullable = false, precision = 19, scale = 4)
    public BigDecimal currentValue = BigDecimal.ZERO;
}
