package com.weaponsregistration.purchase.dto;
import com.weaponsregistration.purchase.model.ReceivingSourceType;
import com.weaponsregistration.purchase.model.ReceivingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
public final class EquipmentReceivingContract {
 private EquipmentReceivingContract() {}
 public record CreateRequest(ReceivingSourceType sourceType,Long acquisitionId,Long receivingOrganizationId,String receivingUnit,String receivingLocation,String deliveryDocumentNumber,String invoiceNumber,LocalDateTime receivedAt,String receivedBy,Boolean physicalChecked,Boolean documentsChecked,String notes,List<ItemRequest> items) {}
 public record ItemRequest(Long acquisitionItemId,Long itemModelId,BigDecimal expectedQuantity,BigDecimal receivedQuantity,String lotNumber,LocalDate manufactureDate,LocalDate expirationDate,String conditionDescription,String notes,List<String> serialNumbers) {}
 public record StatusRequest(ReceivingStatus status,String notes) {}
}
