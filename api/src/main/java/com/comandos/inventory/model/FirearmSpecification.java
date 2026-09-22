package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_firearm_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class FirearmSpecification extends CoreEntity {
    @OneToOne(optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    public ItemModel model;

    @Column(nullable = false, length = 100)
    public String caliber;

    @Column(name = "operating_mechanism", nullable = false, length = 100)
    public String operatingMechanism;

    @Column(nullable = false)
    public Integer capacity;

    @Column(name = "barrel_length", nullable = false, precision = 19, scale = 4)
    public BigDecimal barrelLength;
}
