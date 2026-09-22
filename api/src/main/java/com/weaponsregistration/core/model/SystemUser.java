package com.weaponsregistration.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_system_user", uniqueConstraints = {@UniqueConstraint(columnNames = {"login"})})
public class SystemUser extends CoreEntity {
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    public Person person;
    
	@Column(name = "login", nullable = false, length = 255)
    public String login;
    
	@Column(name = "password_hash", nullable = false, length = 255)
    public String passwordHash;
    
	@Column(nullable = false)
    public boolean mfaEnabled = false;
    
	@Column(name = "blocked", nullable = false)
    public Boolean blocked = false;

}
