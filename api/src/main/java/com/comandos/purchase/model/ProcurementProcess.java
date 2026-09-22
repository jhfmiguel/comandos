package com.comandos.purchase.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.model.Organization;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "erp_procurement_process",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "process_number"}))
public class ProcurementProcess extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    public Organization organization;

    @Column(name = "process_number", nullable = false, length = 100)
    public String processNumber;

    @Column(name = "object_description", nullable = false, length = 4000)
    public String objectDescription;

    @Column(name = "justification", length = 4000)
    public String justification;

    @Enumerated(EnumType.STRING)
    @Column(name = "procurement_method", nullable = false, length = 50)
    public ProcurementMethod procurementMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "bidding_modality", length = 50)
    public BiddingModality biddingModality;

    @Enumerated(EnumType.STRING)
    @Column(name = "direct_contracting_type", length = 50)
    public DirectContractingType directContractingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    public ProcurementStatus status = ProcurementStatus.DRAFT;

    @Column(name = "estimated_value", nullable = false, precision = 19, scale = 4)
    public BigDecimal estimatedValue = BigDecimal.ZERO;

    @Column(name = "legal_basis", length = 1000)
    public String legalBasis;

    @Column(name = "supplier_choice_reason", length = 4000)
    public String supplierChoiceReason;

    @Column(name = "price_justification", length = 4000)
    public String priceJustification;

    @Column(name = "opening_date")
    public LocalDate openingDate;

    @Column(name = "publication_date")
    public LocalDate publicationDate;

    @Column(name = "award_date")
    public LocalDate awardDate;

    @Column(name = "homologation_date")
    public LocalDate homologationDate;
}