package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_asset_item", uniqueConstraints = {@UniqueConstraint(columnNames = {"asset_code"}), @UniqueConstraint(columnNames = {"model_id", "serial_number"})})
public class AssetItem extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    public ItemModel model;
    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    public StockLocation location;
    @Column(name = "asset_code", nullable = false, length = 255)
    public String assetCode;
    @Column(name = "serial_number", nullable = true, length = 255)
    public String serialNumber;
    @Column(name = "condition", nullable = false, length = 255)
    public String condition;
    @Column(name = "status", nullable = false, length = 255)
    public String status;
    @Column(name = "valid_until", nullable = true)
    public LocalDate validUntil;
    @Column(name = "current_value", nullable = false, precision = 19, scale = 4)
    public BigDecimal currentValue = BigDecimal.ZERO;
}
