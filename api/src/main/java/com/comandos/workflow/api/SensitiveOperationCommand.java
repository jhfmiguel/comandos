package com.comandos.workflow.api;

public record SensitiveOperationCommand(
    long workflowId,
    String operationType,
    String resource,
    long organizationId,
    Long unitId,
    String executionJustification,
    String conclusionJustification
) {}
