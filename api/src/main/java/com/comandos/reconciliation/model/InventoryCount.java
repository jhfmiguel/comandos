package com.comandos.reconciliation.model;

import com.comandos.core.model.*;
import com.comandos.inventory.model.StockLocation;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_inventory_count")
public class InventoryCount extends CoreEntity {
    @ManyToOne(optional=false) @JoinColumn(name="organization_id",nullable=false) public Organization organization;
    @ManyToOne @JoinColumn(name="unit_id") public OrganizationalUnit unit;
    @ManyToOne(optional=false) @JoinColumn(name="location_id",nullable=false) public StockLocation location;
    @ManyToOne(optional=false) @JoinColumn(name="status_type_id",nullable=false) public InventoryCountStatusType status;
    @Column(nullable=false) public String organizationName;
    @Column public String unitName;
    @Column(nullable=false) public String locationName;
    @Column(nullable=false,length=255) public String purpose;
    @Column(nullable=false) public LocalDateTime openedAt;
    @Column public LocalDateTime countedAt;
    @Column public LocalDateTime approvedAt;
    @Column public Long openedById;
    @Column public String openedByLogin;
    @Column public Long approvedById;
    @Column public String approvedByLogin;
    @Column(nullable=false,unique=true,length=36) public String requestId;
    @Column(nullable=false,length=64) public String requestFingerprint;
}
