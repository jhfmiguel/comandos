package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_stock_movement")
public class StockMovement extends CoreEntity {
    @ManyToOne(optional = true)
    @JoinColumn(name = "asset_id", nullable = true)
    public AssetItem asset;
    @ManyToOne(optional = true)
    @JoinColumn(name = "lot_id", nullable = true)
    public StockLot lot;
    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    public StockLocation location;
    @Column(name = "nature", nullable = false, length = 255)
    public String nature;
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal quantity = BigDecimal.ZERO;
    @Column(name = "moved_at", nullable = false)
    public LocalDateTime movedAt;
    @Column(name = "operator_id") public Long operatorId;
    @Column(name = "operator_login", length = 255) public String operatorLogin;
    @Column(name = "reference_type", length = 100) public String referenceType;
    @Column(name = "reference_id") public Long referenceId;
    @Column(name = "notes", length = 1000) public String notes;
    @PreUpdate
    private void preventUpdate() {
        throw new IllegalStateException("Stock movement history is immutable.");
    }

    @PreRemove
    private void preventDelete() {
        throw new IllegalStateException("Stock movement history is immutable.");
    }
}
