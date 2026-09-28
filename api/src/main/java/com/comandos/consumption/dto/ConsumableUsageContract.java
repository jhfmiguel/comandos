package com.comandos.consumption.dto;

import java.math.BigDecimal;
import java.util.List;

public final class ConsumableUsageContract {
    private ConsumableUsageContract() {}
    public record LineRequest(Long balanceId, BigDecimal deliveredQuantity, BigDecimal usedQuantity,
        BigDecimal returnedQuantity, String result) {}
    public record FinalizeRequest(String requestId, Long organizationId, Long unitId, Long responsibleId,
        Long authorizerId, String purpose, String activityType, String operationTraining, List<LineRequest> items) {}
    public record StockOption(long balanceId, long lotId, String family, String sku, String modelName,
        String lotNumber, String locationName, String unitOfMeasure, BigDecimal available, String validUntil) {}
    public record ItemView(long id, long lotId, long balanceId, String family, String sku, String modelName,
        String lotNumber, String locationName, String unitOfMeasure, BigDecimal deliveredQuantity,
        BigDecimal usedQuantity, BigDecimal returnedQuantity, long deliveryMovementId, Long returnMovementId,
        String result) {}
    public record UsageView(long id, long organizationId, String organizationName, Long unitId, String unitName,
        long responsibleId, String responsibleName, long authorizerId, String authorizerName, String purpose,
        String activityType, String operationTraining, String status, String deliveredAt, String closedAt,
        Long finalizedById, String finalizedByLogin, List<ItemView> items) {}
    public record Page<T>(List<T> content, long totalElements, int page, int size) {}
}
