package com.comandos.core.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "erp_economic_activity",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"code"})}
)
public class EconomicActivity extends CoreEntity {

    @Column(name = "code", nullable = false, length = 40)
    public String code;

    @Column(name = "description", nullable = false, length = 255)
    public String description;

    @Column(name = "active", nullable = false)
    public Boolean active = true;
}
