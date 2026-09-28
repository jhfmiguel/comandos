package com.comandos.workflow.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class WorkflowPolicyTests {
    private final WorkflowPolicy policy = new WorkflowPolicy();

    @Test
    void acceptsOnlyTheLinearMainPath() {
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.REQUESTED, WorkflowPolicy.ANALYZED));
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.ANALYZED, WorkflowPolicy.AUTHORIZED));
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.EXECUTED));
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.EXECUTED, WorkflowPolicy.CONCLUDED));
    }

    @Test
    void rejectsStateSkippingAndBackwardsTransitions() {
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.REQUESTED, WorkflowPolicy.AUTHORIZED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.ANALYZED, WorkflowPolicy.EXECUTED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.CONCLUDED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.ANALYZED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.EXECUTED, WorkflowPolicy.AUTHORIZED));
    }

    @Test
    void cancellationIsAllowedOnlyBeforeTerminalState() {
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.REQUESTED, WorkflowPolicy.CANCELLED));
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.ANALYZED, WorkflowPolicy.CANCELLED));
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.CANCELLED));
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.EXECUTED, WorkflowPolicy.CANCELLED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.CONCLUDED, WorkflowPolicy.CANCELLED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.CANCELLED, WorkflowPolicy.CANCELLED));
    }

    private void assertConflict(Runnable operation) {
        var error = assertThrows(ResponseStatusException.class, operation::run);
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}
