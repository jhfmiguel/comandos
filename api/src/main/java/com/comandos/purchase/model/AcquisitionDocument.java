package com.comandos.purchase.model;
import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Table(name="erp_acquisition_document")
public class AcquisitionDocument extends CoreEntity {
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="purchase_id",nullable=false) public Purchase purchase;
 @Enumerated(EnumType.STRING) @Column(name="document_type",nullable=false,length=60) public AcquisitionDocumentType documentType;
 @Column(name="document_number",length=160) public String documentNumber;
 @Column(name="issue_date") public LocalDate issueDate;
 @Column(name="issuer",length=255) public String issuer;
 @Column(name="amount",precision=19,scale=4) public BigDecimal amount;
 @Column(name="storage_reference",length=1000) public String storageReference;
 @Column(name="notes",length=2000) public String notes;
}