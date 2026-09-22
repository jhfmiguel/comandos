package com.comandos.rest.weapons;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.comandos.model.Weapon;

public class WeaponFormRequest {
   
    private Long id;
	
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
    private LocalDateTime creationDate; 
	
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
	
    private String priceFormatted;
	
    
    //-- CREATES THE WEAPON ENTITY
    
    public Weapon toModel() {
        return new Weapon( id, sku, name, description, price );
    }
	
    
    //-- TRANSFORMS INTO THE WEAPON ENTITY
    
    public static WeaponFormRequest fromModel( Weapon weapon ) {
        String formatted = "R$ 0,00";
        if (weapon.getPrice() != null) {
            java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.of("pt", "BR"));
            formatted = formatter.format(weapon.getPrice());
        }

        return new WeaponFormRequest(
            weapon.getId(),
            weapon.getCreationDate(),
            weapon.getSku(), 
            weapon.getName(), 
            weapon.getDescription(), 
            weapon.getPrice(),
            formatted
        );
    }

    //--- CONSTRUCTORS
    
    public WeaponFormRequest() {
        super();
    }
	
    public WeaponFormRequest(Long id, LocalDateTime creationDate, String sku, String name, String description, BigDecimal price, String priceFormatted) {
        super();
        this.id = id;
        this.creationDate = creationDate;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.priceFormatted = priceFormatted;
    }
	
    
    //--- GETTERS AND SETTERS
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; } 
	
    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; } 
	
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
	
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
	
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
	
    public String getPriceFormatted() { return priceFormatted; }
    public void setPriceFormatted(String priceFormatted) { this.priceFormatted = priceFormatted; }
	
    
    //--- TOSTRING
    @Override
    public String toString() {
        return "WeaponFormRequest [id=" + id + ", sku=" + sku + ", name=" + name + ", description=" + description + ", price=" + price
                + ", priceFormatted=" + priceFormatted + "]";
    }
}
