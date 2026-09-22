package com.weaponsregistration.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_permission", uniqueConstraints = {@UniqueConstraint(columnNames = {"resource", "action"})})
public class Permission extends CoreEntity {
    
	@Column(name = "resource", nullable = false, length = 255)
    public String resource;
    
	@Column(name = "action", nullable = false, length = 255)
    public String action;
	
}
