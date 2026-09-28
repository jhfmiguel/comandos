package com.comandos.inventory.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class EquipmentSetOperationContract {
    private EquipmentSetOperationContract() {}

    public record DonationSetRequest(String requestId, Long equipmentSetId, Long organizationId, Long unitId,
        Long donorId, Long doneeId, String term, String documentReference) {}

    public record DisposalSetRequest(String requestId, Long equipmentSetId, Long organizationId, Long unitId,
        String processNumber, String reason, String destructionMethod, String destroyedAt,
        String destructionCertificate, Boolean confirmed) {}

    public record ConsumableSetRequest(String requestId, Long equipmentSetId, Long organizationId, Long unitId,
        Long responsibleId, Long authorizerId, String purpose, String activityType, String operationTraining,
        Map<Long, BigDecimal> returnedByComponentId, String result) {}

    public record MaintenanceSetRequest(String requestId, Long equipmentSetId, Long organizationId, Long unitId,
        Long planId, String reason, String maintenanceType, String workshop, String gunsmith) {}

    public record AggregateOperationView(long id, long equipmentSetId, String setCode, String setName,
        String operationType, String aggregateResource, List<Long> aggregateRecordIds,
        int componentCount, String status, String executedAt, Long operatorId, String operatorLogin) {}
}
