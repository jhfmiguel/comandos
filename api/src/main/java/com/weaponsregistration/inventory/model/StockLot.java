package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_stock_lot", uniqueConstraints = {@UniqueConstraint(columnNames = {"model_id", "opening_location_id", "lot_number"})})
public class StockLot extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    public ItemModel model;
    @ManyToOne(optional = false)
    @JoinColumn(name = "opening_location_id", nullable = false)
    public StockLocation openingLocation;
    @Column(name = "lot_number", nullable = false, length = 255)
    public String lotNumber;
    @Column(name = "initial_quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal initialQuantity = BigDecimal.ZERO;
    @Column(name = "available_quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal availableQuantity = BigDecimal.ZERO;
    @Column(columnDefinition = "text") public String openingPackaging;
    @Column(name = "valid_until", nullable = true)
    public LocalDate validUntil;
    @Column(name = "condition", nullable = false, length = 100)
    public String condition = "GOOD";
    @Column(name = "status", nullable = false, length = 100)
    public String status = "AVAILABLE";
}
