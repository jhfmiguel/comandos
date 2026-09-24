package com.comandos.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "erp_permission", uniqueConstraints = {@UniqueConstraint(columnNames = {"resource_name", "action"})})
public class Permission extends CoreEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_type_id")
    public PermissionResource resourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_type_id")
    public PermissionAction actionType;

    /**
     * Legacy/security projections kept for compatibility with authorization queries.
     * They are synchronized from the selected parameter records.
     */
    @Column(name = "resource_name", nullable = false, length = 255)
    public String resource;

    @Column(name = "action", nullable = false, length = 255)
    public String action;
}
