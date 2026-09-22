

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
    
    
package com.comandos.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name="\"user\"")
public class User {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // --- LOG AND AUDIT ATTRIBUTES ---

    @CreatedDate
    @Column(name="creation_date", nullable = false, updatable = false)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime creationDate;
    
    // --- BUSINESS ATTRIBUTES ---
    
    private LocalDate birth;
    
    private String cpf;
    
    private String name;
    
    private String address;
    
    private String phone;
    
    private String email;
    
    // --- PRE PERSIST ---
    
    @PrePersist
    public void prePersist() {
    	setCreationDate( LocalDateTime.now() );
    }
    
    // --- CONSTRUCTORS ---
    
	public User() { 
        super(); 
    }
	
	public User(LocalDate birth, String cpf, String name, String address, String phone, String email) {
		super();
		this.birth = birth;
		this.cpf = cpf;
		this.name = name;
		this.address = address;
		this.phone = phone;
		this.email = email;
	}
	
	public User(Long id, LocalDateTime creationDate, LocalDate birth, String cpf, 
			String name, String address, String phone, String email) {
		super();
		this.id = id;
		this.creationDate = creationDate;
		this.birth = birth;
		this.cpf = cpf;
		this.name = name;
		this.address = address;
		this.phone = phone;
		this.email = email;
	}
    
    // --- GETTERS AND SETTERS ---
    
    public Long getId() { return id; }
	public void setId( Long id ) { this.id = id; }
	
	public LocalDateTime getCreationDate() { return creationDate; }
	public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate;}
	
	public LocalDate getBirth() { return birth; }
	public void setBirth( LocalDate birth ) { this.birth = birth; }
	
	public String getCpf() { return cpf; }
	public void setCpf( String cpf ) { this.cpf = cpf; }
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }
	
	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }
	
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	// --- TOSTRING ---
	
	@Override
	public String toString() {
		return "User [id=" + id + ", creationDate=" + creationDate + ", birth=" + birth + ", cpf=" + cpf + ", name="
				+ name + ", address=" + address + ", phone=" + phone + ", email=" + email + "]";
	}
}
