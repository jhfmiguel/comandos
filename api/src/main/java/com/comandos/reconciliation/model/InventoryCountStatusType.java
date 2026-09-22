package com.comandos.reconciliation.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_inventory_count_status_type", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class InventoryCountStatusType extends CoreEntity {
    @Column(nullable = false, length = 50) public String code;
    @Column(nullable = false) public String name;
    @Column(length = 500) public String description;
    @Column(nullable = false) public boolean active = true;
    @Column(nullable = false) public boolean terminal;
    @Column(nullable = false) public Integer displayOrder = 0;
    @Column(nullable = false) public boolean systemProtected;
}
