package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_ammunition_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class AmmunitionSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(nullable = false, length = 100) public String caliber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "caliber_id") public ArmamentParameter caliberRef;
    @Column(name = "ammunition_type", nullable = false, length = 100) public String ammunitionType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ammunition_type_id") public ArmamentParameter ammunitionTypeRef;
    @Column(name = "lethality_classification", nullable = false, length = 30) public String lethalityClassification;
    @Column(name = "projectile_type", nullable = false, length = 100) public String projectileType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "projectile_type_id") public ArmamentParameter projectileTypeRef;
    @Column(name = "case_type", nullable = false, length = 100) public String caseType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "case_type_id") public ArmamentParameter caseTypeRef;
    @Column(name = "primer_type", nullable = false, length = 100) public String primerType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "primer_type_id") public ArmamentParameter primerTypeRef;
}
