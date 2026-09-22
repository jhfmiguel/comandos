package com.comandos.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_person_qualification")
public class PersonQualification extends CoreEntity {
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    public Person person;
    
	@Column(name = "category", nullable = false, length = 255)
    public String category;
    
	@Column(name = "valid_until", nullable = false)
    public LocalDate validUntil;
    
	@Column(name = "status", nullable = false, length = 255)
    public String status;
	
}
