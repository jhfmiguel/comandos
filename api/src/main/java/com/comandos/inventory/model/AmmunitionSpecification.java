package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_ammunition_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class AmmunitionSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(nullable = false, length = 100) public String caliber;
    @Column(name = "ammunition_type", nullable = false, length = 100) public String ammunitionType;
    @Column(name = "lethality_classification", nullable = false, length = 30) public String lethalityClassification;
    @Column(name = "projectile_type", nullable = false, length = 100) public String projectileType;
    @Column(name = "case_type", nullable = false, length = 100) public String caseType;
    @Column(name = "primer_type", nullable = false, length = 100) public String primerType;
}
