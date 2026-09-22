package com.weaponsregistration.purchase.model;

import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_procurement_document")
public class ProcurementDocument extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "procurement_process_id", nullable = false)
    public ProcurementProcess procurementProcess;

    @Column(name = "document_type", nullable = false, length = 100)
    public String documentType;

    @Column(name = "document_number", length = 100)
    public String documentNumber;

    @Column(name = "title", nullable = false, length = 255)
    public String title;

    @Column(name = "storage_reference", length = 1000)
    public String storageReference;

    @Column(name = "legal_requirement", nullable = false)
    public Boolean legalRequirement = false;

    @Column(name = "active", nullable = false)
    public Boolean active = true;
}