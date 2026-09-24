package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_spray_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class SpraySpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(nullable = false, length = 100) public String agent;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "agent_id") public ArmamentParameter agentRef;
    @Column(nullable = false, length = 500) public String composition;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "composition_id") public ArmamentParameter compositionRef;
    @Column(name = "shelf_life_months", nullable = false) public Integer shelfLifeMonths;
    @Column(nullable = false, precision = 19, scale = 4) public BigDecimal concentration;
    @Column(name = "volume_ml", nullable = false, precision = 19, scale = 4) public BigDecimal volumeMl;
    @Column(name = "range_meters", nullable = false, precision = 19, scale = 4) public BigDecimal rangeMeters;
}
