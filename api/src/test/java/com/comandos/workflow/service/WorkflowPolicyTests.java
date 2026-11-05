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
    private final ComandosWorkflowPolicy policy = new ComandosWorkflowPolicy(new DefaultWorkflowEngine());

    @Test
    void exposesTheCanonicalWorkflowPathInOrder() {
        assertEquals(
            List.of(
                ComandosWorkflowPolicy.REQUESTED,
                ComandosWorkflowPolicy.ANALYZED,
                ComandosWorkflowPolicy.AUTHORIZED,
                ComandosWorkflowPolicy.EXECUTED,
                ComandosWorkflowPolicy.CONCLUDED
            ),
            ComandosWorkflowPolicy.MAIN_PATH
        );
        assertFalse(policy.terminal(ComandosWorkflowPolicy.REQUESTED));
        assertFalse(policy.terminal(ComandosWorkflowPolicy.ANALYZED));
        assertFalse(policy.terminal(ComandosWorkflowPolicy.AUTHORIZED));
        assertFalse(policy.terminal(ComandosWorkflowPolicy.EXECUTED));
        assertTrue(policy.terminal(ComandosWorkflowPolicy.CONCLUDED));
        assertTrue(policy.terminal(ComandosWorkflowPolicy.CANCELLED));
    }

    @Test
    void acceptsOnlyTheLinearMainPathThroughCanonicalEngine() {
        assertDoesNotThrow(() -> policy.requireTransition(ComandosWorkflowPolicy.REQUESTED, ComandosWorkflowPolicy.ANALYZED));
        assertDoesNotThrow(() -> policy.requireTransition(ComandosWorkflowPolicy.ANALYZED, ComandosWorkflowPolicy.AUTHORIZED));
        assertDoesNotThrow(() -> policy.requireTransition(ComandosWorkflowPolicy.AUTHORIZED, ComandosWorkflowPolicy.EXECUTED));
        assertDoesNotThrow(() -> policy.requireTransition(ComandosWorkflowPolicy.EXECUTED, ComandosWorkflowPolicy.CONCLUDED));
    }

    @Test
    void rejectsStateSkippingBackwardsAndRepeatedTransitions() {
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.REQUESTED, ComandosWorkflowPolicy.AUTHORIZED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.REQUESTED, ComandosWorkflowPolicy.EXECUTED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.REQUESTED, ComandosWorkflowPolicy.CONCLUDED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.ANALYZED, ComandosWorkflowPolicy.EXECUTED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.AUTHORIZED, ComandosWorkflowPolicy.CONCLUDED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.AUTHORIZED, ComandosWorkflowPolicy.ANALYZED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.EXECUTED, ComandosWorkflowPolicy.AUTHORIZED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.ANALYZED, ComandosWorkflowPolicy.ANALYZED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.AUTHORIZED, ComandosWorkflowPolicy.AUTHORIZED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.EXECUTED, ComandosWorkflowPolicy.EXECUTED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.CONCLUDED, ComandosWorkflowPolicy.CONCLUDED));
    }

    @Test
    void cancellationIsAllowedOnlyBeforeTerminalState() {
        assertDoesNotThrow(() -> policy.requireTransition(ComandosWorkflowPolicy.REQUESTED, ComandosWorkflowPolicy.CANCELLED));
        assertDoesNotThrow(() -> policy.requireTransition(ComandosWorkflowPolicy.ANALYZED, ComandosWorkflowPolicy.CANCELLED));
        assertDoesNotThrow(() -> policy.requireTransition(ComandosWorkflowPolicy.AUTHORIZED, ComandosWorkflowPolicy.CANCELLED));
        assertDoesNotThrow(() -> policy.requireTransition(ComandosWorkflowPolicy.EXECUTED, ComandosWorkflowPolicy.CANCELLED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.CONCLUDED, ComandosWorkflowPolicy.CANCELLED));
        assertConflict(() -> policy.requireTransition(ComandosWorkflowPolicy.CANCELLED, ComandosWorkflowPolicy.CANCELLED));
    }

    @Test
    void normalizesOperationStatusAndPermissionResource() {
        assertEquals("STOCK_ADJUSTMENT", ComandosWorkflowPolicy.normalize(" stock-adjustment "));
        assertEquals(ComandosWorkflowPolicy.AUTHORIZED, ComandosWorkflowPolicy.normalizeStatus(" authorized "));
        assertEquals("inventory/assets", ComandosWorkflowPolicy.normalizeResource(" inventory/assets "));
    }

    @Test
    void rejectsUnknownOperationsStatesAndUnsafeResources() {
        assertBadRequest(() -> policy.rule("UNKNOWN_OPERATION"));
        assertBadRequest(() -> ComandosWorkflowPolicy.normalizeStatus("APPROVED"));
        assertBadRequest(() -> ComandosWorkflowPolicy.normalizeResource("/inventory/assets"));
        assertBadRequest(() -> ComandosWorkflowPolicy.normalizeResource("inventory/../assets"));
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
        assertEquals("CREATE", policy.permissionForTransition(ComandosWorkflowPolicy.REQUESTED));
        assertEquals("UPDATE", policy.permissionForTransition(ComandosWorkflowPolicy.ANALYZED));
        assertEquals("APPROVE", policy.permissionForTransition(ComandosWorkflowPolicy.AUTHORIZED));
        assertEquals("UPDATE", policy.permissionForTransition(ComandosWorkflowPolicy.EXECUTED));
        assertEquals("UPDATE", policy.permissionForTransition(ComandosWorkflowPolicy.CONCLUDED));
        assertEquals("UPDATE", policy.permissionForTransition(ComandosWorkflowPolicy.CANCELLED));
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
