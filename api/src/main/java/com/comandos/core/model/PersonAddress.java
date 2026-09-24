package com.comandos.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "erp_person_address")
public class PersonAddress extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false, updatable = false)
    public Person person;
    @Column(name = "address_type", nullable = false) public String type;
    @Column public String postalCode;
    @Column(nullable = false) public String street;
    @Column(name = "address_number", nullable = false) public String number;
    public String complement;
    public String district;
    @Column(nullable = false) public String city;
    @Column(nullable = false) public String state;
    @Column(name = "country", length = 120) public String country = "Brasil";
    @Column(name = "foreign_address") public Boolean foreignAddress = false;
    @Column(nullable = false) public Boolean primaryAddress;
    @Column(nullable = false) public boolean archived = false;
}
