package com.comandos.workflow.api;

public record WorkflowRequest(
    Long organizationId,
    Long unitId,
    String operationType,
    String resource,
    Long recordId,
    String justification
) {}
