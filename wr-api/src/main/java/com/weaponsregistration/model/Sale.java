package com.weaponsregistration.model;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sale")
public class Sale {
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "id_user")
	private User user;
	
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "payment_method")
	private PaymentMethod paymentMethod;
	
	@OneToMany(mappedBy = "sale")
	private List<SaleItem> weapons;
	
	@Column
	private BigDecimal total;

	
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public PaymentMethod getPaymentMethod() { return paymentMethod; }
	public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
	public List<SaleItem> getWeapons() { return weapons; }
	public void setItens(List<SaleItem> weapons) { this.weapons = weapons; }
	public BigDecimal getTotal() { return total; }
	public void setTotal(BigDecimal total) { this.total = total; }
	
	
	

}
