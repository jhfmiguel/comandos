package com.weaponsregistration.purchase.model;
import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="erp_equipment_receiving")
public class EquipmentReceiving extends CoreEntity {
 @Enumerated(EnumType.STRING) @Column(name="source_type",nullable=false,length=40) public ReceivingSourceType sourceType=ReceivingSourceType.ACQUISITION;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="purchase_id") public Purchase acquisition;
 @Column(name="external_source_id") public Long externalSourceId;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="receiving_organization_id") public Organization receivingOrganization;
 @Column(name="receiving_unit",length=255) public String receivingUnit;
 @Column(name="receiving_location",length=500) public String receivingLocation;
 @Column(name="delivery_document_number",length=160) public String deliveryDocumentNumber;
 @Column(name="invoice_number",length=160) public String invoiceNumber;
 @Column(name="received_at") public LocalDateTime receivedAt;
 @Column(name="received_by",length=255) public String receivedBy;
 @Enumerated(EnumType.STRING) @Column(name="status",nullable=false,length=40) public ReceivingStatus status=ReceivingStatus.EXPECTED;
 @Column(name="notes",length=4000) public String notes;
}