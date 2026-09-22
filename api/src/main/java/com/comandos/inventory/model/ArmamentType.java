package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_armament_type", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class ArmamentType extends CoreEntity {
    @Column(nullable = false, length = 50)
    public String code;
    @Column(nullable = false)
    public String name;
    public String description;
    @Column(nullable = false)
    public Boolean active = true;
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    public ItemCategory category;
}

