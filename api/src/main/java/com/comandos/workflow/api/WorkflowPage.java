package com.comandos.workflow.api;

import java.util.List;

public record WorkflowPage<T>(
    List<T> content,
    long totalElements,
    int page,
    int size
) {}
