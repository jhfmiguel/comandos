package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_grenade_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class GrenadeSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "grenade_type", nullable = false, length = 100) public String grenadeType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "grenade_type_id") public ArmamentParameter grenadeTypeRef;
    @Column(nullable = false, length = 100) public String agent;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "agent_id") public ArmamentParameter agentRef;
    @Column(nullable = false, length = 500) public String composition;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "composition_id") public ArmamentParameter compositionRef;
    @Column(name = "shelf_life_months", nullable = false) public Integer shelfLifeMonths;
    @Column(name = "delay_seconds", nullable = false) public Integer delaySeconds;
    @Column(name = "safety_radius", nullable = false, precision = 19, scale = 4) public BigDecimal safetyRadius;
}
