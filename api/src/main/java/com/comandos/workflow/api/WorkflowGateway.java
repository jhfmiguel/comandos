package com.comandos.workflow.api;

import com.comandos.core.api.PlatformPage;

public interface WorkflowGateway {

    WorkflowView request(WorkflowRequest request);

    WorkflowView analyze(long id, WorkflowTransition transition);

    WorkflowView authorize(long id, WorkflowTransition transition);

    WorkflowView execute(long id, WorkflowTransition transition);

    WorkflowView conclude(long id, WorkflowTransition transition);

    WorkflowView cancel(long id, WorkflowTransition transition);

    WorkflowView get(long id);

    PlatformPage<WorkflowView> list(
        long organizationId,
        Long unitId,
        String status,
        int page
    );
}
