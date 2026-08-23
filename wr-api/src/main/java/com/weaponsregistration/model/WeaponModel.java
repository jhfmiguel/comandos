package com.weaponsregistration.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="cliente")
public class WeaponModel {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // --- LOG AND AUDIT ATTRIBUTES ---

    @CreatedDate
    @Column(name="creation_date", nullable = false, updatable = false)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
    private LocalDateTime creationDate;

    //@LastModifiedDate
    //@Column(nullable = false)
    //@JsonFormat(pattern = "dd/MM/yyyy")
    //private LocalDateTime updateDate;
    
    //@CreatedBy
    //@Column(updatable = false)
    //private String createdBy;

    //@LastModifiedBy
    //private String updatedBy;
    
    // --- SECURITY AND INTEGRITY ATTRIBUTES ---

    //@Version
    //private Long version;

    //@Column(nullable = false)
    //private Boolean active = true;
    
    
    // --- BUSINESS ATTRIBUTES ---
    
    private LocalDate birth;
    private String cpf;
    private String name;
    private String address;
    private String phone;
    private String email;
    

}
