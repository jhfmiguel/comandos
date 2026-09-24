package com.comandos.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "erp_access_profile", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class AccessProfile extends CoreEntity {

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_level_id")
    public AccessProfileLevel levelType;

    /**
     * Legacy/security projection kept for compatibility with authorization queries.
     * Always synchronized from levelType.code for new/updated records.
     */
    @Column(name = "profile_level", nullable = false, length = 255)
    public String level;
}
