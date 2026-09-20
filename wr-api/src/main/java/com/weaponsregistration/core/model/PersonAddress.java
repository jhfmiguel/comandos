package com.weaponsregistration.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "erp_person_address")
public class PersonAddress extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false, updatable = false)
    public Person person;
    @Column(nullable = false) public String type;
    @Column(nullable = false) public String postalCode;
    @Column(nullable = false) public String street;
    @Column(nullable = false) public String number;
    public String complement;
    public String district;
    @Column(nullable = false) public String city;
    @Column(nullable = false) public String state;
    @Column(nullable = false) public Boolean primaryAddress;
    @Column(nullable = false) public boolean archived = false;
}
