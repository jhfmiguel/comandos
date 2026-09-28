package com.comandos.workflow.service;

import com.comandos.workflow.api.SensitiveOperation;
import com.comandos.workflow.api.SensitiveOperationCommand;
import com.comandos.workflow.api.SensitiveOperationResult;
import com.comandos.workflow.api.WorkflowTransition;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SensitiveOperationExecutor {
    private final WorkflowApprovalGuard guard;
    private final WorkflowService workflows;

    public SensitiveOperationExecutor(WorkflowApprovalGuard guard, WorkflowService workflows) {
        this.guard = guard;
        this.workflows = workflows;
    }

    @Transactional(rollbackFor = Throwable.class)
    public <T> T execute(SensitiveOperationCommand command, SensitiveOperation<T> operation) {
        validate(command, operation);

        guard.requireAuthorized(
            command.workflowId(),
            command.operationType(),
            command.resource(),
            command.organizationId(),
            command.unitId()
        );

        workflows.execute(
            command.workflowId(),
            new WorkflowTransition(command.executionJustification().trim())
        );

        // The business callback is never allowed to run merely because the
        // transition method was invoked. Re-check the protected context and
        // the exact state inside the same transaction before releasing control.
        guard.requireExecuted(
            command.workflowId(),
            command.operationType(),
            command.resource(),
            command.organizationId(),
            command.unitId()
        );

        SensitiveOperationResult<T> result = operation.execute();
        if (result == null) {
            throw new IllegalStateException("Sensitive operation must return an execution result.");
        }

        if (result.recordId() != null) {
            workflows.bindRecordId(command.workflowId(), result.recordId());
        }

        workflows.conclude(
            command.workflowId(),
            new WorkflowTransition(command.conclusionJustification().trim())
        );
        return result.value();
    }

    private static <T> void validate(SensitiveOperationCommand command, SensitiveOperation<T> operation) {
        if (command == null) {
            bad("Sensitive operation command is required.");
        }
        if (operation == null) {
            bad("Sensitive business operation is required.");
        }
        if (command.workflowId() <= 0 || command.organizationId() <= 0) {
            bad("Workflow and organization are required for sensitive operation execution.");
        }
        if (command.unitId() != null && command.unitId() <= 0) {
            bad("Unit must be a positive identifier when supplied.");
        }
        WorkflowPolicy.normalize(command.operationType());
        WorkflowPolicy.normalizeResource(command.resource());
        if (blank(command.executionJustification()) || blank(command.conclusionJustification())) {
            bad("Execution and conclusion justifications are required.");
        }
        if (command.executionJustification().trim().length() > 2000
                || command.conclusionJustification().trim().length() > 2000) {
            bad("Workflow justifications must contain at most 2000 characters.");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void bad(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
