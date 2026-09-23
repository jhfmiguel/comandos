package com.comandos.workflow.api;

public interface WorkflowGateway {

    WorkflowView request(WorkflowRequest request);

    WorkflowView analyze(long id, WorkflowTransition transition);

    WorkflowView authorize(long id, WorkflowTransition transition);

    WorkflowView execute(long id, WorkflowTransition transition);

    WorkflowView conclude(long id, WorkflowTransition transition);

    WorkflowView cancel(long id, WorkflowTransition transition);

    WorkflowView get(long id);

    WorkflowPage<WorkflowView> list(
        long organizationId,
        Long unitId,
        String status,
        int page
    );
}
