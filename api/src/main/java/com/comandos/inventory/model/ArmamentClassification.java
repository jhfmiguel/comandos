package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_armament_classification", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class ArmamentClassification extends CoreEntity {
    @Column(nullable = false, length = 50)
    public String code;
    @Column(nullable = false)
    public String name;
    public String description;
    @Column(nullable = false)
    public Boolean active = true;
    @ManyToOne(optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    public ArmamentType type;
}

