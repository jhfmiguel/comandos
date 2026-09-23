package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_stock_intake")
public class StockIntake extends CoreEntity {
    @Column(nullable = false, unique = true, length = 36) public String requestId;
    @Column(nullable = false, length = 64) public String fingerprint;
    @Column(name = "resource_name", nullable = false, length = 20) public String resource;
    @Lob @Column(nullable = false) public String recordIds;
    @Column(nullable = false, length = 30) public String quantity;
}
