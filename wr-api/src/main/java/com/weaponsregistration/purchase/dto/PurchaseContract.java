package com.weaponsregistration.purchase.dto;

import com.weaponsregistration.purchase.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseContract {
    private PurchaseContract() {}

    public record CreatePurchaseRequest(
        Long buyerOrganizationId, Long supplierOrganizationId,
        String purchaseNumber, LocalDate purchaseDate, String notes,
        List<CreatePurchaseItemRequest> items) {}

    public record CreatePurchaseItemRequest(
        Long itemModelId, BigDecimal quantity, BigDecimal unitPrice,
        BigDecimal discount, String notes) {}

    public record CreateProcurementRequest(
        String processNumber, String objectDescription, String justification,
        ProcurementMethod procurementMethod, BiddingModality biddingModality,
        DirectContractingType directContractingType, BigDecimal estimatedValue,
        String legalBasis, String supplierChoiceReason, String priceJustification) {}

    public record PurchaseItemView(
        Long id, Long itemModelId, String itemName, BigDecimal quantity,
        BigDecimal receivedQuantity, BigDecimal unitPrice,
        BigDecimal discount, BigDecimal total) {}

    public record ProcurementView(
        Long id, String processNumber, ProcurementMethod procurementMethod,
        BiddingModality biddingModality, DirectContractingType directContractingType,
        ProcurementStatus status, BigDecimal estimatedValue, String legalBasis) {}

    public record PurchaseView(
        Long id, Long buyerOrganizationId, String buyerName,
        Long supplierOrganizationId, String supplierName,
        String purchaseNumber, LocalDate purchaseDate, PurchaseStatus status,
        BigDecimal subtotal, BigDecimal discount, BigDecimal freight,
        BigDecimal taxes, BigDecimal total, ProcurementView procurement,
        List<PurchaseItemView> items) {}
}