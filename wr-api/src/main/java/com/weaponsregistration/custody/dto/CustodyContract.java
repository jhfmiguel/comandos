package com.weaponsregistration.custody.dto;

import java.util.List;

public final class CustodyContract {
    private CustodyContract() {}
    public record IssueRequest(String requestId, Long organizationId, Long unitId, Long recipientId,
        Long authorizerId, String purpose, String dueAt, List<Long> assetIds, List<Long> equipmentSetIds, Long recipientUnitId) {}
    public record ReturnRequest(String requestId, List<Long> itemIds, Long conditionTypeId, String inspectionNotes) {}
    public record StockOption(long assetId, String assetCode, String serialNumber, String modelName, String locationName) {}
    public record EquipmentSetOption(long equipmentSetId, String code, String name, int componentCount) {}
    public record ItemView(long id, Long assetId, Long balanceId, Long equipmentSetId, String equipmentSetCode,
        String equipmentSetName, String componentRole, String assetCode, String serialNumber, String modelName,
        String locationName, String quantity, long issueMovementId, String returnedAt,
        Long returnConditionTypeId, String returnConditionName, Boolean returnBlocksAvailability,
        String inspectionNotes, Long returnInspectionId, Long maintenanceWorkOrderId) {}
    public record ReturnView(long id, String returnedAt, Long returnedById, String returnedByLogin, List<Long> itemIds) {}
    public record CustodyView(long id, long organizationId, String organizationName, Long unitId, String unitName,
        Long recipientId, Long recipientUnitId, String recipientType, String recipientName, long authorizerId, String authorizerName, String purpose,
        String status, String deliveredAt, String dueAt, String completedAt, Long issuedById, String issuedByLogin,
        List<ItemView> items, List<ReturnView> returns) {}
    public record Page<T>(List<T> content, long totalElements, int page, int size) {}
}
