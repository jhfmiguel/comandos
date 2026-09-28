package com.comandos.workflow.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.comandos.workflow.api.SensitiveOperation;
import com.comandos.workflow.api.SensitiveOperationCommand;
import com.comandos.workflow.api.SensitiveOperationResult;
import com.comandos.workflow.api.WorkflowTransition;
import com.comandos.workflow.api.WorkflowView;
import com.comandos.workflow.model.ApprovalWorkflow;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

class SensitiveOperationExecutorTests {

    @Test
    void executesBusinessOperationOnlyAfterExecutedAndBindsLateRecordIdBeforeConclusion() {
        var calls = new ArrayList<String>();
        var guard = new GuardSpy(calls);
        var workflows = new WorkflowSpy(calls);
        var executor = new SensitiveOperationExecutor(guard, workflows);

        String result = executor.execute(command(), () -> {
            calls.add("BUSINESS");
            return SensitiveOperationResult.withRecord("ok", 777L);
        });

        assertEquals("ok", result);
        assertEquals(
            List.of("AUTHORIZED", "EXECUTED", "EXECUTED_GUARD", "BUSINESS", "BIND:777", "CONCLUDED"),
            calls
        );
    }

    @Test
    void supportsOperationsWhoseRecordDoesNotExistBeforeExecution() {
        var calls = new ArrayList<String>();
        var executor = new SensitiveOperationExecutor(new GuardSpy(calls), new WorkflowSpy(calls));

        String result = executor.execute(command(), () -> SensitiveOperationResult.of("no-record-yet"));

        assertEquals("no-record-yet", result);
        assertEquals(List.of("AUTHORIZED", "EXECUTED", "EXECUTED_GUARD", "CONCLUDED"), calls);
    }

    @Test
    void businessFailureStopsBeforeRecordBindingAndConclusion() {
        var calls = new ArrayList<String>();
        var executor = new SensitiveOperationExecutor(new GuardSpy(calls), new WorkflowSpy(calls));

        var error = assertThrows(IllegalStateException.class, () -> executor.execute(command(), () -> {
            calls.add("BUSINESS");
            throw new IllegalStateException("business failure");
        }));

        assertEquals("business failure", error.getMessage());
        assertEquals(List.of("AUTHORIZED", "EXECUTED", "EXECUTED_GUARD", "BUSINESS"), calls);
    }

    @Test
    void nullBusinessResultStopsBeforeRecordBindingAndConclusion() {
        var calls = new ArrayList<String>();
        var executor = new SensitiveOperationExecutor(new GuardSpy(calls), new WorkflowSpy(calls));

        var error = assertThrows(IllegalStateException.class, () -> executor.execute(command(), () -> {
            calls.add("BUSINESS");
            return null;
        }));

        assertEquals("Sensitive operation must return an execution result.", error.getMessage());
        assertEquals(List.of("AUTHORIZED", "EXECUTED", "EXECUTED_GUARD", "BUSINESS"), calls);
    }

    @Test
    void invalidCommandIsRejectedBeforeWorkflowMutation() {
        var calls = new ArrayList<String>();
        var executor = new SensitiveOperationExecutor(new GuardSpy(calls), new WorkflowSpy(calls));
        var invalid = new SensitiveOperationCommand(
            100L,
            "TRANSFER",
            "inventory/assets",
            10L,
            0L,
            "Execute authorized transfer",
            "Transfer completed successfully"
        );

        assertThrows(ResponseStatusException.class,
            () -> executor.execute(invalid, () -> SensitiveOperationResult.of("should-not-run")));
        assertTrue(calls.isEmpty());
    }

    @Test
    void executorDeclaresRollbackForAnyFailure() throws Exception {
        var method = SensitiveOperationExecutor.class.getMethod(
            "execute", SensitiveOperationCommand.class, SensitiveOperation.class);
        var transaction = method.getAnnotation(Transactional.class);

        assertTrue(transaction != null);
        assertArrayEquals(new Class<?>[] { Throwable.class }, transaction.rollbackFor());
    }

    private static SensitiveOperationCommand command() {
        return new SensitiveOperationCommand(
            100L,
            "TRANSFER",
            "inventory/assets",
            10L,
            20L,
            "Execute authorized transfer",
            "Transfer completed successfully"
        );
    }

    private static final class GuardSpy extends WorkflowApprovalGuard {
        private final List<String> calls;

        GuardSpy(List<String> calls) {
            super(null, new WorkflowPolicy());
            this.calls = calls;
        }

        @Override
        public ApprovalWorkflow requireAuthorized(long workflowId, String operationType, String resource,
                long organizationId, Long unitId) {
            calls.add("AUTHORIZED");
            return new ApprovalWorkflow();
        }

        @Override
        public ApprovalWorkflow requireExecuted(long workflowId, String operationType, String resource,
                long organizationId, Long unitId) {
            calls.add("EXECUTED_GUARD");
            return new ApprovalWorkflow();
        }
    }

    private static final class WorkflowSpy extends WorkflowService {
        private final List<String> calls;

        WorkflowSpy(List<String> calls) {
            super(null, null, null, null, new WorkflowPolicy());
            this.calls = calls;
        }

        @Override
        public WorkflowView execute(long id, WorkflowTransition transition) {
            calls.add("EXECUTED");
            return null;
        }

        @Override
        public WorkflowView bindRecordId(long id, long recordId) {
            calls.add("BIND:" + recordId);
            return null;
        }

        @Override
        public WorkflowView conclude(long id, WorkflowTransition transition) {
            calls.add("CONCLUDED");
            return null;
        }
    }
}
