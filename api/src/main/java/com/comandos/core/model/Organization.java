package com.comandos.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_organization")
public class Organization extends CoreEntity {
   
	@Column(name = "nature", nullable = false, length = 255)
    public String nature;
    
	@Column(name = "name", nullable = false, length = 255)
    public String name;
    
	@Column(name = "acronym", nullable = true, length = 255)
    public String acronym;
    
	@Column(name = "tax_id", nullable = true, length = 255)
    public String taxId;
   
	@Column(name = "public_organization", nullable = false)
    public Boolean publicOrganization = false;
    
	@Column(name = "active", nullable = false)
    public Boolean active = true;
	
}
