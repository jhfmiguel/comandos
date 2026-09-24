package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_restraint_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class RestraintSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "restraint_type", nullable = false, length = 100) public String restraintType;
    @Column(nullable = false, length = 100) public String material;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "material_id") public ArmamentParameter materialRef;
    @Column(name = "locking_mechanism", length = 100) public String lockingMechanism;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "locking_mechanism_id") public ArmamentParameter lockingMechanismRef;
    @Column(name = "double_lock", nullable = false) public Boolean doubleLock = false;
}
