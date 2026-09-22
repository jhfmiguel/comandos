package com.comandos.model;

import java.math.BigDecimal;
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
@Table( name = "weapon" )
public class Weapon {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // --- LOG AND AUDIT ATTRIBUTES ---

    @CreatedDate
    @Column(name="creation_date", nullable = false, updatable = false)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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
    
    @Column(name = "sku")
	private String sku;
    
    @Column(name = "name", length = 100)
    private String name;
    
    @Column(name = "description", length = 255)
	private String description;
    
    @Column(name = "price", precision = 16, scale = 2)
    private BigDecimal price;
    
    
    //--- PRE PRESIST ---
    
    @PrePersist
    public void prePresist() {
    	setCreationDate( LocalDateTime.now() );
    }
    
    
    //--- CONSTRUCTORS ---
    
	public Weapon() { super(); }
	
	public Weapon(LocalDateTime creationDate, String sku, String name, String description, 
			BigDecimal price) {
		
		super();
		this.creationDate = creationDate;
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.price = price;
		
	}
	
	public Weapon(Long id, String sku, String name, String description, 
			BigDecimal price) {
		
		super();
		this.id = id;
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.price = price;
		
	}

    
    // --- GETTERS AND SETTERS ---
    
	public Long getId() { return id; }
	public void setId( Long id ) { this.id = id; }
	
	public LocalDateTime getCreationDate() { return creationDate; }
	public void setCreationDate( LocalDateTime creationDate ) { this.creationDate = creationDate; }
	
	public String getSku() { return sku; }
	public void setSku( String sku ) { this.sku = sku; }
	
	public String getName() { return name; }
	public void setName( String name ) { this.name = name;}
	
	public String getDescription() { return description; }
	public void setDescription( String description ) { this.description = description; }
	
	public BigDecimal getPrice() { return price; }
	public void setPrice( BigDecimal price ) { this.price = price; }

	
	 // --- TOSTRING ---

	@Override
	public String toString() {
		return "Weapon [id=" + id + ",sku=" + sku + " name=" + name + ",  description=" + description + ", price=" + price + "]";
	}
    

}
