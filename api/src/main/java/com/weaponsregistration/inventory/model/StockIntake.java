package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_stock_intake")
public class StockIntake extends CoreEntity {
    @Column(nullable = false, unique = true, length = 36) public String requestId;
    @Column(nullable = false, length = 64) public String fingerprint;
    @Column(nullable = false, length = 20) public String resource;
    @Column(nullable = false, columnDefinition = "text") public String recordIds;
    @Column(nullable = false, length = 30) public String quantity;
}
