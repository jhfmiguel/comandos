package com.comandos.report.dto;
import java.math.BigDecimal;import java.util.List;public final class ArmamentReportContract{private ArmamentReportContract(){}
 public record Dashboard(long totalAssets,long availableAssets,long custodyAssets,long maintenanceAssets,long blockedAssets,BigDecimal availableStock,long overdueCustodies,long openOccurrences,long expiredCompliance,long pendingWorkflows,long inventoryDivergences){}
 public record Alert(String severity,String type,String message,String reference,Long recordId){}
 public record History(String source,Long id,String occurredAt,String action,String reference,String actor){}
 public record Position(Long assetId,String assetCode,String serialNumber,String model,String status,String condition,String location,String unit){}
 public record LotPosition(Long lotId,String lotNumber,String model,String location,BigDecimal available,BigDecimal reserved,BigDecimal blocked,String validUntil){}
 public record ConsumptionSummary(String period,String unit,BigDecimal quantity){}
 public record Bundle(Dashboard dashboard,List<Alert>alerts,List<Position>assets,List<LotPosition>lots,List<History>recentHistory){}
}