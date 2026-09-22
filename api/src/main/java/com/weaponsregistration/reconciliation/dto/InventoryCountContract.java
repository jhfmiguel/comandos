package com.weaponsregistration.reconciliation.dto;

import java.math.BigDecimal;
import java.util.List;

public final class InventoryCountContract {
    private InventoryCountContract() {}
    public record OpenRequest(String requestId, Long organizationId, Long unitId, Long locationId, String purpose) {}
    public record CountLine(Long itemId, BigDecimal countedQuantity, String notes) {}
    public record SubmitRequest(List<CountLine> items) {}
    public record ItemView(long id, String kind, Long assetId, Long lotId, String modelName, String sku,
        String stockCode, String unitOfMeasure, String systemQuantity, String countedQuantity,
        String differenceQuantity, String resultCode, String resultName, String notes) {}
    public record CountView(long id, long organizationId, String organizationName, Long unitId, String unitName,
        long locationId, String locationName, String purpose, String statusCode, String statusName,
        String openedAt, String countedAt, String approvedAt, String openedByLogin, String approvedByLogin,
        List<ItemView> items) {}
    public record Page<T>(List<T> content, long totalElements, int page, int size) {}
}
