package com.weaponsregistration.purchase.dto;
import com.weaponsregistration.purchase.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public final class PurchaseContract {
 private PurchaseContract(){}
 public record CreatePurchaseRequest(
  Long buyerOrganizationId,Long supplierOrganizationId,AcquisitionType acquisitionType,String originDescription,
  String purchaseNumber,LocalDate purchaseDate,BigDecimal discount,BigDecimal freight,BigDecimal taxes,BigDecimal otherCosts,
  String paymentConditions,String deliveryConditions,String warrantyConditions,String notes,
  List<CreatePurchaseItemRequest> items,List<DocumentRequest> documents){}
 public record CreatePurchaseItemRequest(Long itemModelId,BigDecimal quantity,BigDecimal unitPrice,BigDecimal discount,String conditionDescription,String notes){}
 public record DocumentRequest(AcquisitionDocumentType documentType,String documentNumber,LocalDate issueDate,String issuer,BigDecimal amount,String storageReference,String notes){}
 public record CreateProcurementRequest(String processNumber,String objectDescription,String justification,ProcurementMethod procurementMethod,
  BiddingModality biddingModality,DirectContractingType directContractingType,BigDecimal estimatedValue,String legalBasis,String supplierChoiceReason,String priceJustification){}
 public record PurchaseItemView(Long id,Long itemModelId,String itemName,BigDecimal quantity,BigDecimal receivedQuantity,BigDecimal unitPrice,BigDecimal discount,BigDecimal total,String conditionDescription){}
 public record DocumentView(Long id,AcquisitionDocumentType documentType,String documentNumber,LocalDate issueDate,String issuer,BigDecimal amount,String storageReference,String notes){}
 public record ProcurementView(Long id,String processNumber,ProcurementMethod procurementMethod,BiddingModality biddingModality,DirectContractingType directContractingType,ProcurementStatus status,BigDecimal estimatedValue,String legalBasis){}
 public record PurchaseView(Long id,Long buyerOrganizationId,String buyerName,Boolean publicBuyer,Long supplierOrganizationId,String supplierName,
  AcquisitionType acquisitionType,String originDescription,String purchaseNumber,LocalDate purchaseDate,PurchaseStatus status,BigDecimal subtotal,
  BigDecimal discount,BigDecimal freight,BigDecimal taxes,BigDecimal otherCosts,BigDecimal total,String paymentConditions,String deliveryConditions,
  String warrantyConditions,String notes,ProcurementView procurement,List<PurchaseItemView> items,List<DocumentView> documents){}
}