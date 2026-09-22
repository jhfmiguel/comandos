package com.comandos.purchase.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_purchase_planning_document")
public class PurchasePlanningDocument extends CoreEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "planning_id", nullable = false)
    public PurchasePlanning planning;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 60)
    public PurchasePlanningDocumentType documentType;

    @Column(name = "title", nullable = false, length = 255)
    public String title;

    @Column(name = "document_number", length = 120)
    public String documentNumber;

    @Column(name = "document_url", length = 1000)
    public String documentUrl;

    @Column(name = "legal_basis", length = 2000)
    public String legalBasis;

    @Column(name = "issued_at")
    public LocalDateTime issuedAt;

    @Column(name = "approved", nullable = false)
    public Boolean approved = false;

    @Column(name = "notes", length = 4000)
    public String notes;
}