package com.weaponsregistration.purchase.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "erp_purchase_planning")
public class PurchasePlanning extends CoreEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    public Organization organization;

    @Column(name = "process_number", length = 120)
    public String processNumber;

    @Column(name = "requesting_unit", length = 255)
    public String requestingUnit;

    @Column(name = "object_description", nullable = false, length = 2000)
    public String objectDescription;

    @Column(name = "need_justification", nullable = false, length = 4000)
    public String needJustification;

    @Column(name = "estimated_value", precision = 19, scale = 2)
    public BigDecimal estimatedValue;

    @Column(name = "expected_contract_date")
    public LocalDate expectedContractDate;

    @Column(name = "budget_source", length = 255)
    public String budgetSource;

    @Column(name = "budget_available")
    public Boolean budgetAvailable;

    @Column(name = "technical_preliminary_study_required", nullable = false)
    public Boolean technicalPreliminaryStudyRequired = true;

    @Column(name = "terms_of_reference_required", nullable = false)
    public Boolean termsOfReferenceRequired = true;

    @Column(name = "risk_analysis_required", nullable = false)
    public Boolean riskAnalysisRequired = true;

    @Column(name = "price_research_required", nullable = false)
    public Boolean priceResearchRequired = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    public PurchasePlanningStatus status = PurchasePlanningStatus.DRAFT;

    @Column(name = "notes", length = 4000)
    public String notes;
}