package com.comandos.workflow.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fariamiguel.workflow.service.DefaultWorkflowEngine;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class WorkflowPolicyTests {
    private final WorkflowPolicy policy = new WorkflowPolicy(new DefaultWorkflowEngine());

    @Test
    void exposesTheCanonicalWorkflowPathInOrder() {
        assertEquals(
            List.of(
                WorkflowPolicy.REQUESTED,
                WorkflowPolicy.ANALYZED,
                WorkflowPolicy.AUTHORIZED,
                WorkflowPolicy.EXECUTED,
                WorkflowPolicy.CONCLUDED
            ),
            WorkflowPolicy.MAIN_PATH
        );
        assertFalse(policy.terminal(WorkflowPolicy.REQUESTED));
        assertFalse(policy.terminal(WorkflowPolicy.ANALYZED));
        assertFalse(policy.terminal(WorkflowPolicy.AUTHORIZED));
        assertFalse(policy.terminal(WorkflowPolicy.EXECUTED));
        assertTrue(policy.terminal(WorkflowPolicy.CONCLUDED));
        assertTrue(policy.terminal(WorkflowPolicy.CANCELLED));
    }

    @Test
    void acceptsOnlyTheLinearMainPathThroughCanonicalEngine() {
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.REQUESTED, WorkflowPolicy.ANALYZED));
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.ANALYZED, WorkflowPolicy.AUTHORIZED));
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.EXECUTED));
        assertDoesNotThrow(() -> policy.requireTransition(WorkflowPolicy.EXECUTED, WorkflowPolicy.CONCLUDED));
    }

    @Test
    void rejectsStateSkippingBackwardsAndRepeatedTransitions() {
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.REQUESTED, WorkflowPolicy.AUTHORIZED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.REQUESTED, WorkflowPolicy.EXECUTED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.REQUESTED, WorkflowPolicy.CONCLUDED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.ANALYZED, WorkflowPolicy.EXECUTED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.CONCLUDED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.ANALYZED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.EXECUTED, WorkflowPolicy.AUTHORIZED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.ANALYZED, WorkflowPolicy.ANALYZED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.AUTHORIZED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.EXECUTED, WorkflowPolicy.EXECUTED));
        assertConflict(() -> policy.requireTransition(WorkflowPolicy.CONCLUDED, WorkflowPolicy.CONCLUDED));
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

    @Test
    void normalizesOperationStatusAndPermissionResource() {
        assertEquals("STOCK_ADJUSTMENT", WorkflowPolicy.normalize(" stock-adjustment "));
        assertEquals(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.normalizeStatus(" authorized "));
        assertEquals("inventory/assets", WorkflowPolicy.normalizeResource(" inventory/assets "));
    }

    @Test
    void rejectsUnknownOperationsStatesAndUnsafeResources() {
        assertBadRequest(() -> policy.rule("UNKNOWN_OPERATION"));
        assertBadRequest(() -> WorkflowPolicy.normalizeStatus("APPROVED"));
        assertBadRequest(() -> WorkflowPolicy.normalizeResource("/inventory/assets"));
        assertBadRequest(() -> WorkflowPolicy.normalizeResource("inventory/../assets"));
    }

    @Test
    void allSensitiveOperationsRequireAnalysisAndApproval() {
        assertEquals(15, policy.rules().size());
        policy.rules().values().forEach(rule -> {
            assertTrue(rule.analysisRequired(), rule.operationType());
            assertTrue(rule.approvalRequired(), rule.operationType());
        });
    }

    @Test
    void authorizationTransitionUsesExplicitApprovePermission() {
        assertEquals("CREATE", policy.permissionForTransition(WorkflowPolicy.REQUESTED));
        assertEquals("UPDATE", policy.permissionForTransition(WorkflowPolicy.ANALYZED));
        assertEquals("APPROVE", policy.permissionForTransition(WorkflowPolicy.AUTHORIZED));
        assertEquals("UPDATE", policy.permissionForTransition(WorkflowPolicy.EXECUTED));
        assertEquals("UPDATE", policy.permissionForTransition(WorkflowPolicy.CONCLUDED));
        assertEquals("UPDATE", policy.permissionForTransition(WorkflowPolicy.CANCELLED));
    }

    private void assertConflict(Runnable operation) {
        var error = assertThrows(ResponseStatusException.class, operation::run);
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    private void assertBadRequest(Runnable operation) {
        var error = assertThrows(ResponseStatusException.class, operation::run);
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }
}
