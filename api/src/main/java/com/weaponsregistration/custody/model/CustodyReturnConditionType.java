package com.weaponsregistration.custody.model;

import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_custody_return_condition_type", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class CustodyReturnConditionType extends CoreEntity {
    @Column(nullable = false, length = 50) public String code;
    @Column(nullable = false) public String name;
    @Column(length = 500) public String description;
    @Column(nullable = false) public boolean active = true;
    @Column(nullable = false) public boolean blocksAvailability;
    @Column(nullable = false) public Integer displayOrder = 0;
    @Column(nullable = false) public boolean systemProtected;
}
