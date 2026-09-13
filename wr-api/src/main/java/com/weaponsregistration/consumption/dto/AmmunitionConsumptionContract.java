package com.weaponsregistration.consumption.dto;

import java.math.BigDecimal;
import java.util.List;

public final class AmmunitionConsumptionContract {
    private AmmunitionConsumptionContract() {}
    public record LineRequest(Long balanceId, BigDecimal quantity, String result) {}
    public record FinalizeRequest(String requestId, Long organizationId, Long unitId, Long responsibleId,
        Long authorizerId, String purpose, List<LineRequest> items) {}
    public record StockOption(long balanceId, long lotId, String sku, String modelName, String lotNumber,
        String locationName, String unitOfMeasure, BigDecimal available, String validUntil) {}
    public record ItemView(long id, long lotId, long balanceId, String sku, String modelName, String lotNumber,
        String locationName, String unitOfMeasure, BigDecimal quantity, String result, long movementId) {}
    public record ConsumptionView(long id, long organizationId, String organizationName, Long unitId, String unitName,
        long responsibleId, String responsibleName, long authorizerId, String authorizerName, String purpose,
        String status, String consumedAt, Long finalizedById, String finalizedByLogin, List<ItemView> items) {}
    public record Page<T>(List<T> content, long totalElements, int page, int size) {}
}
