package com.comandos.lifecycle.dto;
import java.math.BigDecimal;import java.util.List;
public final class LifecycleContract{private LifecycleContract(){}
 public record InspectionItemRequest(Integer order,String code,String label,Boolean required,String result,String observation){}
 public record InspectionItemView(Long id,Integer order,String code,String label,Boolean required,String result,String observation,long photoCount){}
 public record InspectionRequest(Long organizationId,Long unitId,Long assetId,String checklist,String result,String damages,Boolean generateMaintenance,List<InspectionItemRequest> items){}
 public record InspectionView(Long id,Long assetId,String assetCode,String checklist,String result,String damages,String inspectedAt,String responsibleLogin,String approvedByLogin,Long generatedWorkOrderId,List<InspectionItemView> items,long photoCount){}
 public record OccurrenceRequest(Long organizationId,Long unitId,Long assetId,Long lotId,Long balanceId,String type,BigDecimal quantity,String description,String investigation,String documentReference){}
 public record OccurrenceView(Long id,String type,String status,Long assetId,Long lotId,String reference,BigDecimal quantity,String description,String investigation,String documentReference,String occurredAt,String responsibleLogin,String resolvedAt,String resolvedByLogin){}
 public record ResolveRequest(String investigation,String documentReference){}
 public record AttachmentRequest(String resource,Long recordId,String fileName,String contentType,String base64,String description,Long inspectionItemId){}
 public record AttachmentView(Long id,String resource,Long recordId,String fileName,String contentType,String description,String uploadedAt,String uploadedByLogin,Long inspectionItemId){}
 public record Page<T>(List<T> content,long totalElements,int page,int size){}
}
