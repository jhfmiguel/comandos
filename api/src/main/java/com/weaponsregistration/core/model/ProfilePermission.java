package com.weaponsregistration.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_profile_permission", uniqueConstraints = {@UniqueConstraint(columnNames = {"profile_id", "permission_id"})})
public class ProfilePermission extends CoreEntity {
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    public AccessProfile profile;
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    public Permission permission;

}
