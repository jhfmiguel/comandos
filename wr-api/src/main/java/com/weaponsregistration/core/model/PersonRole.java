package com.weaponsregistration.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_person_role", uniqueConstraints = {@UniqueConstraint(columnNames = {"code"})})
public class PersonRole extends CoreEntity {
    
	@Column(name = "code", nullable = false, length = 255)
    public String code;
    
	@Column(name = "name", nullable = false, length = 255)
    public String name;
	
}
