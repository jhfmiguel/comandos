package com.comandos.workflow.api;

import java.util.List;

public record WorkflowView(
    Long id,
    Long organizationId,
    Long unitId,
    String operationType,
    String resource,
    Long recordId,
    String status,
    String justification,
    String requestedAt,
    String requestedByLogin,
    String authorityLogin,
    String authorizedAt,
    String executedAt,
    String concludedAt,
    String cancelledAt,
    List<WorkflowEventView> events
) {}
