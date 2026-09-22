package com.weaponsregistration.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_person")
public class Person extends CoreEntity {
    
	@Column(name = "person_type", nullable = false, length = 255)
    public String personType;
    
	@Column(name = "full_name", nullable = false, length = 255)
    public String fullName;
    
	@Column(name = "tax_id", nullable = true, length = 255)
    public String taxId;
    
	@Column(name = "birth_date", nullable = true)
    public LocalDate birthDate;
    
	@Column(name = "address", nullable = true, length = 255)
    public String address;
   
	@Column(name = "phone", nullable = true, length = 255)
    public String phone;
    
	@Column(name = "email", nullable = true, length = 255)
    public String email;
   
	@Column(name = "active", nullable = false)
    public Boolean active = true;
	
}
