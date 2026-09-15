package com.weaponsregistration.purchase.model;
import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="erp_receiving_inspection")
public class ReceivingInspection extends CoreEntity {
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="receiving_id",nullable=false) public EquipmentReceiving receiving;
 @Column(name="inspected_at") public LocalDateTime inspectedAt;
 @Column(name="inspector",length=255) public String inspector;
 @Column(name="provisional_receipt",nullable=false) public Boolean provisionalReceipt=false;
 @Column(name="definitive_receipt",nullable=false) public Boolean definitiveReceipt=false;
 @Column(name="approved") public Boolean approved;
 @Column(name="non_conformity",length=4000) public String nonConformity;
 @Column(name="decision_notes",length=4000) public String decisionNotes;
}