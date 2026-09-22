package com.weaponsregistration.transfer.dto;
import java.math.BigDecimal;
import java.util.List;

public final class TransferContract {
    private TransferContract() {}

    public record LineRequest(Long assetId, Long balanceId, BigDecimal quantity) {}
    public record AcceptRequest(String requestId) {}
    public record RejectRequest(String requestId, String reason) {}
    public record FinalizeRequest(
        String requestId,
        Long organizationId,
        Long sourceUnitId,
        Long destinationUnitId,
        Long destinationLocationId,
        String purpose,
        List<LineRequest> items,
        String transferType,
        String legalInstrument,
        String documentReference
    ) {}
    public record StockOption(
        String kind, long stockId, String code, String modelName, String sku,
        String locationName, String unitOfMeasure, String available
    ) {}
    public record LineView(
        long id, Long assetId, Long lotId, String modelName, String sku, String stockCode,
        String sourceLocationName, String destinationLocationName, String unitOfMeasure,
        String quantity, long outMovementId, long inMovementId
    ) {}
    public record TransferView(
        long id, long organizationId, String organizationName,
        long sourceUnitId, String sourceUnitName,
        long destinationUnitId, String destinationUnitName,
        long destinationLocationId, String destinationLocationName,
        String purpose, String status, String sentAt,
        Long finalizedById, String finalizedByLogin,
        Long approvedById, String approvedByLogin, String approvedAt,
        Long rejectedById, String rejectedByLogin, String rejectedAt, String rejectionReason,
        List<LineView> items
    ) {}
    public record Page<T>(List<T> content, long totalElements, int page, int size) {}
}
