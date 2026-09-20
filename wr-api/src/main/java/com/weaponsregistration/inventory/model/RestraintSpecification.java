package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_restraint_specification", uniqueConstraints = @UniqueConstraint(columnNames = "model_id"))
public class RestraintSpecification extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "model_id", nullable = false) public ItemModel model;
    @Column(name = "restraint_type", nullable = false, length = 100) public String restraintType;
    @Column(nullable = false, length = 100) public String material;
    @Column(name = "locking_mechanism", length = 100) public String lockingMechanism;
    @Column(name = "double_lock", nullable = false) public Boolean doubleLock = false;
}
