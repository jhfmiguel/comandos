package com.comandos.purchase.dto;

import com.comandos.purchase.model.PurchasePlanningStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class PurchasePlanningContract {
    private PurchasePlanningContract() {}

    public record CreateRequest(
        Long organizationId,
        String processNumber,
        String requestingUnit,
        String objectDescription,
        String needJustification,
        BigDecimal estimatedValue,
        LocalDate expectedContractDate,
        String budgetSource,
        Boolean budgetAvailable,
        Boolean technicalPreliminaryStudyRequired,
        Boolean termsOfReferenceRequired,
        Boolean riskAnalysisRequired,
        Boolean priceResearchRequired,
        String notes
    ) {}

    public record StatusRequest(PurchasePlanningStatus status) {}

    public record View(
        Long id,
        Long organizationId,
        String processNumber,
        String requestingUnit,
        String objectDescription,
        String needJustification,
        BigDecimal estimatedValue,
        LocalDate expectedContractDate,
        String budgetSource,
        Boolean budgetAvailable,
        Boolean technicalPreliminaryStudyRequired,
        Boolean termsOfReferenceRequired,
        Boolean riskAnalysisRequired,
        Boolean priceResearchRequired,
        PurchasePlanningStatus status,
        String notes,
        String organizationCanonicalId
    ) {}
}
