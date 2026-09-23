package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_ballistic_protection_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class BallisticProtectionSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "protection_type", nullable = false, length = 100) public String protectionType;
    @Column(name = "protection_level", nullable = false, length = 100) public String protectionLevel;
    @Column(nullable = false, length = 100) public String material;
    @Column(nullable = false, length = 150) public String certification;
    @Column(name = "size_label", nullable = false, length = 50) public String size;
    @Column(name = "service_life_months", nullable = false) public Integer serviceLifeMonths;
}
