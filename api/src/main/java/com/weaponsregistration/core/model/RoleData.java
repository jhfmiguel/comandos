package com.weaponsregistration.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_role_data", uniqueConstraints = {@UniqueConstraint(columnNames = {"person_role_id", "detail_key"})})
public class RoleData extends CoreEntity {
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "person_role_id", nullable = false)
    public PersonRoleAssignment personRole;
    
	@Column(name = "detail_key", nullable = false, length = 255)
    public String key;
    
	@Column(name = "detail_value", nullable = false, length = 255)
    public String value;

}
