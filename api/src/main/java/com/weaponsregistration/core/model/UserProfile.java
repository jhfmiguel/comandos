package com.weaponsregistration.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_user_profile")
public class UserProfile extends CoreEntity {
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public SystemUser user;
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    public AccessProfile profile;
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    public Organization organization;
    
	@ManyToOne(optional = true)
    @JoinColumn(name = "unit_id", nullable = true)
    public OrganizationalUnit unit;

}
