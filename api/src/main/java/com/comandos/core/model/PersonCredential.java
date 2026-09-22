package com.comandos.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_person_credential")
public class PersonCredential extends CoreEntity {
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    public Person person;
    
	@Column(name = "type", nullable = false, length = 255)
    public String type;
    
	@Column(name = "number", nullable = false, length = 255)
    public String number;
   
	@Column(name = "valid_until", nullable = false)
    public LocalDate validUntil;
	
}
