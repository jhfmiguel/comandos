package com.comandos.consumption.dto;

import java.math.BigDecimal;
import java.util.List;

public final class AmmunitionConsumptionContract {
    private AmmunitionConsumptionContract() {}
    public record LineRequest(Long balanceId, BigDecimal quantity, String result,
        BigDecimal deliveredQuantity, BigDecimal usedQuantity, BigDecimal returnedQuantity) {}
    public record FinalizeRequest(String requestId, Long organizationId, Long unitId, Long responsibleId,
        Long authorizerId, String purpose, List<LineRequest> items, String activityType, String operationTraining) {}
    public record StockOption(long balanceId, long lotId, String sku, String modelName, String lotNumber,
        String locationName, String unitOfMeasure, BigDecimal available, String validUntil) {}
    public record ItemView(long id, long lotId, long balanceId, String sku, String modelName, String lotNumber,
        String locationName, String unitOfMeasure, BigDecimal quantity, String result, long movementId,
        BigDecimal deliveredQuantity, BigDecimal usedQuantity, BigDecimal returnedQuantity) {}
    public record ConsumptionView(long id, long organizationId, String organizationName, Long unitId, String unitName,
        long responsibleId, String responsibleName, long authorizerId, String authorizerName, String purpose,
        String status, String consumedAt, Long finalizedById, String finalizedByLogin, List<ItemView> items,
        String activityType, String operationTraining) {}
    public record Page<T>(List<T> content, long totalElements, int page, int size) {}
}
