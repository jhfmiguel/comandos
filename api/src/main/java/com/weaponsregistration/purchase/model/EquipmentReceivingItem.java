package com.weaponsregistration.purchase.model;
import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="erp_equipment_receiving_item")
public class EquipmentReceivingItem extends CoreEntity {
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="receiving_id",nullable=false) public EquipmentReceiving receiving;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="purchase_item_id") public PurchaseItem acquisitionItem;
 @Column(name="item_model_id") public Long itemModelId;
 @Column(name="expected_quantity",precision=19,scale=4) public BigDecimal expectedQuantity;
 @Column(name="received_quantity",nullable=false,precision=19,scale=4) public BigDecimal receivedQuantity;
 @Column(name="accepted_quantity",precision=19,scale=4) public BigDecimal acceptedQuantity;
 @Column(name="rejected_quantity",precision=19,scale=4) public BigDecimal rejectedQuantity;
 @Column(name="lot_number",length=160) public String lotNumber;
 @Column(name="manufacture_date") public LocalDate manufactureDate;
 @Column(name="expiration_date") public LocalDate expirationDate;
 @Column(name="condition_description",length=1000) public String conditionDescription;
 @Column(name="divergence_description",length=3000) public String divergenceDescription;
 @Column(name="notes",length=3000) public String notes;
}