package com.comandos.workflow.api;

import com.comandos.workflow.dto.WorkflowContract.Page;
import com.comandos.workflow.dto.WorkflowContract.Request;
import com.comandos.workflow.dto.WorkflowContract.Transition;
import com.comandos.workflow.dto.WorkflowContract.View;

public interface WorkflowGateway {

    View request(Request request);

    View analyze(long id, Transition transition);

    View authorize(long id, Transition transition);

    View execute(long id, Transition transition);

    View conclude(long id, Transition transition);

    View cancel(long id, Transition transition);

    View get(long id);

    Page<View> list(
        long organizationId,
        Long unitId,
        String status,
        int page
    );
}
