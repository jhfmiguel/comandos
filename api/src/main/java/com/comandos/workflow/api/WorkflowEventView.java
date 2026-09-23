package com.comandos.workflow.api;

public record WorkflowEventView(
    Long id,
    String fromStatus,
    String toStatus,
    String justification,
    String occurredAt,
    String actorLogin
) {}
