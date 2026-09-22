package com.weaponsregistration.purchase.dto;

import com.weaponsregistration.purchase.model.ReceivingSourceType;
import com.weaponsregistration.purchase.model.ReceivingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class EquipmentReceivingContract {
    private EquipmentReceivingContract() {}

    public record CreateRequest(
        ReceivingSourceType sourceType,
        Long acquisitionId,
        Long receivingOrganizationId,
        String receivingUnit,
        String receivingLocation,
        String deliveryDocumentNumber,
        String invoiceNumber,
        LocalDateTime receivedAt,
        String receivedBy,
        Boolean physicalChecked,
        Boolean documentsChecked,
        String notes,
        List<ItemRequest> items
    ) {}

    public record ItemRequest(
        Long acquisitionItemId,
        Long itemModelId,
        BigDecimal expectedQuantity,
        BigDecimal receivedQuantity,
        String lotNumber,
        LocalDate manufactureDate,
        LocalDate expirationDate,
        String conditionDescription,
        String notes,
        List<String> serialNumbers
    ) {}

    public record StatusRequest(ReceivingStatus status, String notes) {}

    public record SerialView(
        Long id,
        String serialNumber,
        String manufacturerCode,
        String assetCode,
        Boolean accepted,
        String rejectionReason
    ) {}

    public record ItemView(
        Long id,
        Long acquisitionItemId,
        Long itemModelId,
        String itemName,
        BigDecimal acquiredQuantity,
        BigDecimal expectedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal acceptedQuantity,
        BigDecimal rejectedQuantity,
        BigDecimal acquisitionPendingQuantity,
        String lotNumber,
        LocalDate manufactureDate,
        LocalDate expirationDate,
        String conditionDescription,
        String divergenceDescription,
        String notes,
        List<SerialView> serials
    ) {}

    public record View(
        Long id,
        ReceivingSourceType sourceType,
        Long acquisitionId,
        String acquisitionNumber,
        Long receivingOrganizationId,
        String receivingOrganizationName,
        String receivingUnit,
        String receivingLocation,
        String deliveryDocumentNumber,
        String invoiceNumber,
        LocalDateTime receivedAt,
        String receivedBy,
        Boolean physicalChecked,
        Boolean documentsChecked,
        ReceivingStatus status,
        String notes,
        List<ItemView> items
    ) {}
}
