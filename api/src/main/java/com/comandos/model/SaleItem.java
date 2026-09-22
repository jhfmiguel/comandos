package com.comandos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sale_item")
public class SaleItem {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "id_sale")
	private Sale sale;
	
	@ManyToOne
	@JoinColumn(name = "id_weapon")
	private Weapon weapon;
	
	@Column
	private Integer quantity;
	
	
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Sale getSale() { return sale; }
	public void setSale(Sale sale) { this.sale = sale; }
	public Weapon getWeapon() { return weapon; }
	public void setWeapon(Weapon weapon) { this.weapon = weapon; }
	public Integer getQuantity() { return quantity; }
	public void setQuantity(Integer quantity) { this.quantity = quantity; }
	
	
	@Override
	public String toString() {
		return "SaleItem [id=" + id + ", sale=" + sale + ", weapon=" + weapon + ", quantity=" + quantity + "]";
	}
	
	
	

}
