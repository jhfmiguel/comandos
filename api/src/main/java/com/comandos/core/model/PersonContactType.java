package com.comandos.core.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "erp_person_contact_type",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"code"})}
)
public class PersonContactType extends CoreEntity {

    @Column(name = "code", nullable = false, length = 80)
    public String code;

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "description", length = 255)
    public String description;

    @Column(name = "address_enabled", nullable = false)
    public Boolean addressEnabled = false;

    @Column(name = "phone_enabled", nullable = false)
    public Boolean phoneEnabled = false;

    @Column(name = "email_enabled", nullable = false)
    public Boolean emailEnabled = false;

    @Column(name = "active", nullable = false)
    public Boolean active = true;
}
