package com.weaponsregistration.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_access_profile", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class AccessProfile extends CoreEntity {
    
	@Column(name = "name", nullable = false, length = 255)
    public String name;
    
	@Column(name = "level", nullable = false, length = 255)
    public String level;
	
}
