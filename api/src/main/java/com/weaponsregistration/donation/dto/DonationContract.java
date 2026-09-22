package com.weaponsregistration.donation.dto;
import java.math.BigDecimal;
import java.util.List;
public final class DonationContract {
 private DonationContract() {}
 public record LineRequest(Long assetId, Long balanceId, BigDecimal quantity) {}
 public record FinalizeRequest(String requestId, Long organizationId, Long unitId, Long donorId, Long doneeId, String term, List<LineRequest> items, String direction, String documentReference) {}
 public record StockOption(String kind,long stockId,String code,String modelName,String sku,String locationName,String unitOfMeasure,String available) {}
 public record LineView(long id,long modelId,Long assetId,Long lotId,long locationId,long movementId,String modelName,String sku,String stockCode,String locationName,String unitOfMeasure,String quantity) {}
 public record DonationView(long id,long organizationId,String organizationName,Long unitId,String unitName,long donorId,String donorName,long doneeId,String doneeName,String term,String status,String finalizedAt,Long finalizedById,String finalizedByLogin,List<LineView> items) {}
 public record Page<T>(List<T> content,long totalElements,int page,int size) {}
}
