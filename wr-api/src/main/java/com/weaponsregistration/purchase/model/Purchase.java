package com.weaponsregistration.purchase.model;
import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name="erp_purchase",uniqueConstraints=@UniqueConstraint(columnNames={"buyer_organization_id","purchase_number"}))
public class Purchase extends CoreEntity {
 @ManyToOne(optional=false) @JoinColumn(name="buyer_organization_id",nullable=false) public Organization buyerOrganization;
 @ManyToOne @JoinColumn(name="supplier_organization_id") public Organization supplierOrganization;
 @OneToOne @JoinColumn(name="procurement_process_id") public ProcurementProcess procurementProcess;
 @Enumerated(EnumType.STRING) @Column(name="acquisition_type",nullable=false,length=30) public AcquisitionType acquisitionType=AcquisitionType.ONEROUS;
 @Column(name="origin_description",length=1000) public String originDescription;
 @Column(name="purchase_number",nullable=false,length=100) public String purchaseNumber;
 @Column(name="purchase_date",nullable=false) public LocalDate purchaseDate;
 @Enumerated(EnumType.STRING) @Column(name="status",nullable=false,length=50) public PurchaseStatus status=PurchaseStatus.DRAFT;
 @Column(name="subtotal",nullable=false,precision=19,scale=4) public BigDecimal subtotal=BigDecimal.ZERO;
 @Column(name="discount",nullable=false,precision=19,scale=4) public BigDecimal discount=BigDecimal.ZERO;
 @Column(name="freight",nullable=false,precision=19,scale=4) public BigDecimal freight=BigDecimal.ZERO;
 @Column(name="taxes",nullable=false,precision=19,scale=4) public BigDecimal taxes=BigDecimal.ZERO;
 @Column(name="other_costs",nullable=false,precision=19,scale=4) public BigDecimal otherCosts=BigDecimal.ZERO;
 @Column(name="total",nullable=false,precision=19,scale=4) public BigDecimal total=BigDecimal.ZERO;
 @Column(name="payment_conditions",length=2000) public String paymentConditions;
 @Column(name="delivery_conditions",length=2000) public String deliveryConditions;
 @Column(name="warranty_conditions",length=2000) public String warrantyConditions;
 @Column(name="notes",length=4000) public String notes;
 @OneToMany(mappedBy="purchase",cascade=CascadeType.ALL,orphanRemoval=true) public List<PurchaseItem> items=new ArrayList<>();
 @OneToMany(mappedBy="purchase",cascade=CascadeType.ALL,orphanRemoval=true) public List<AcquisitionDocument> documents=new ArrayList<>();
 public void recalculateTotals(){
  subtotal=items.stream().map(PurchaseItem::calculateTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
  total=subtotal.subtract(n(discount)).add(n(freight)).add(n(taxes)).add(n(otherCosts));
 }
 private BigDecimal n(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
}