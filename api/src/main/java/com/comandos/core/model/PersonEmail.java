package com.comandos.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "erp_person_email")
public class PersonEmail extends CoreEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false, updatable = false)
    public Person person;

    @Column(name = "email_type", nullable = false, length = 40)
    public String type;

    @Column(name = "email_address", nullable = false, length = 255)
    public String email;

    @Column(name = "primary_email", nullable = false)
    public Boolean primaryEmail = false;
}
