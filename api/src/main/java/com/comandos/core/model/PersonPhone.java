package com.comandos.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "erp_person_phone")
public class PersonPhone extends CoreEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false, updatable = false)
    public Person person;

    @Column(name = "phone_type", nullable = false, length = 40)
    public String type;

    @Column(name = "country_code", length = 8)
    public String countryCode = "+55";

    @Column(name = "phone_number", nullable = false, length = 40)
    public String number;

    @Column(name = "whatsapp", nullable = false)
    public Boolean whatsapp = false;

    @Column(name = "primary_phone", nullable = false)
    public Boolean primaryPhone = false;
}
