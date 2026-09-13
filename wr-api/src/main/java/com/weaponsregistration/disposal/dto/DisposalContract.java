package com.weaponsregistration.disposal.dto;

import java.math.BigDecimal;
import java.util.List;

public final class DisposalContract {
    private DisposalContract() {}
    public record LineRequest(Long assetId, Long balanceId, BigDecimal quantity) {}
    public record FinalizeRequest(String requestId, Long organizationId, Long unitId, String processNumber, String reason,
        String destructionMethod, String destroyedAt, String destructionCertificate, List<LineRequest> items) {}
    public record StockOption(String kind, long stockId, String code, String modelName, String sku, String locationName,
        String unitOfMeasure, String available) {}
    public record LineView(long id, Long assetId, Long lotId, String modelName, String sku, String stockCode,
        String locationName, String unitOfMeasure, String quantity, long movementId) {}
    public record DisposalView(long id, long organizationId, String organizationName, Long unitId, String unitName,
        String processNumber, String reason, String status, String finalizedAt, Long finalizedById, String finalizedByLogin,
        String destructionMethod, String destroyedAt, String destructionCertificate, List<LineView> items) {}
    public record Page<T>(List<T> content, long totalElements, int page, int size) {}
}
