package com.comandos.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_organizational_unit", uniqueConstraints = {@UniqueConstraint(columnNames = {"organization_id", "code"})})
public class OrganizationalUnit extends CoreEntity {
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
   	public Organization organization;
    
	@ManyToOne(optional = true)
    @JoinColumn(name = "parent_unit_id", nullable = true)
    public OrganizationalUnit parentUnit;
    
    @Column(name = "code", nullable = false, length = 255)
    public String code;
    
    @Column(name = "name", nullable = false, length = 255)
    public String name;
    
    @Column(name = "type", nullable = false, length = 255)
    public String type;
    
}
