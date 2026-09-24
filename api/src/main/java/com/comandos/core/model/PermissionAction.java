package com.comandos.core.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "erp_permission_action",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"code"})}
)
public class PermissionAction extends CoreEntity {

    @Column(name = "code", nullable = false, length = 80)
    public String code;

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "description", length = 255)
    public String description;

    @Column(name = "active", nullable = false)
    public Boolean active = true;
}
