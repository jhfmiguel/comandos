package com.comandos.compliance.dto;

import java.util.List;

public final class ComplianceContract {
    private ComplianceContract() {}

    public record PolicyRequest(Integer expirationWarningDays, Integer maintenanceWarningDays,
                                Integer regulatoryWarningDays, Integer inspectionIntervalDays, Boolean active) {}

    public record PolicyView(Long id, long organizationId, Long unitId, Integer expirationWarningDays,
                             Integer maintenanceWarningDays, Integer regulatoryWarningDays,
                             Integer inspectionIntervalDays, Boolean active) {}

    public record AlertView(String severity, String type, String resource, Long recordId, String code,
                            String message, String dueDate, Integer daysRemaining) {}

    public record ItemComplianceView(String kind, Long id, String code, String modelName, String status,
                                     boolean compliant, List<String> reasons, String validUntil,
                                     String nextMaintenanceAt, String lastInspectionAt,
                                     String lastInspectionResult, long activeRecalls, long activeAlerts) {}

    public record SummaryView(long organizationId, Long unitId, PolicyView policy,
                              long totalItems, long compliantItems, long nonCompliantItems,
                              long criticalAlerts, long warningAlerts, long informationalAlerts,
                              List<AlertView> alerts, List<ItemComplianceView> items) {}
}
