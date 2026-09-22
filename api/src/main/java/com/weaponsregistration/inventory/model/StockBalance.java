package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_stock_balance", uniqueConstraints = {@UniqueConstraint(columnNames = {"lot_id", "location_id"})})
public class StockBalance extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    public StockLot lot;
    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    public StockLocation location;
    @Column(name = "available", nullable = false, precision = 19, scale = 4)
    public BigDecimal available = BigDecimal.ZERO;
    @Column(name = "reserved", nullable = false, precision = 19, scale = 4)
    public BigDecimal reserved = BigDecimal.ZERO;
    @Column(name = "blocked", nullable = false, precision = 19, scale = 4)
    public BigDecimal blocked = BigDecimal.ZERO;
}
