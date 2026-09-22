package com.comandos.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_person_role_assignment")
public class PersonRoleAssignment extends CoreEntity {
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    public Person person;
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    public PersonRole role;
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    public Organization organization;
    
	@ManyToOne(optional = true)
    @JoinColumn(name = "unit_id", nullable = true)
    public OrganizationalUnit unit;
    
	@Column(name = "start_date", nullable = false)
    public LocalDate startDate;
    
	@Column(name = "end_date", nullable = true)
    public LocalDate endDate;
    
	@Column(name = "status", nullable = false, length = 255)
    public String status;

}
