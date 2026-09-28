package com.comandos.workflow.service;

import com.comandos.workflow.model.ApprovalWorkflow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class WorkflowApprovalGuard {
    private final EntityManager em;
    private final WorkflowPolicy policy;

    public WorkflowApprovalGuard(EntityManager em, WorkflowPolicy policy) {
        this.em = em;
        this.policy = policy;
    }

    @Transactional
    public ApprovalWorkflow requireAuthorized(long workflowId, String operationType, String resource,
            long organizationId, Long unitId) {
        return requireStateByWorkflowId(workflowId, operationType, resource, organizationId, unitId,
            WorkflowPolicy.AUTHORIZED);
    }

    @Transactional
    public ApprovalWorkflow requireExecuted(long workflowId, String operationType, String resource,
            long organizationId, Long unitId) {
        return requireStateByWorkflowId(workflowId, operationType, resource, organizationId, unitId,
            WorkflowPolicy.EXECUTED);
    }

    @Transactional
    public ApprovalWorkflow requireConcluded(long workflowId, String operationType, String resource,
            long organizationId, Long unitId) {
        return requireStateByWorkflowId(workflowId, operationType, resource, organizationId, unitId,
            WorkflowPolicy.CONCLUDED);
    }

    @Transactional(readOnly = true)
    public ApprovalWorkflow requireAuthorized(String operationType, String resource, long recordId,
            long organizationId, Long unitId) {
        return requireStateByRecord(operationType, resource, recordId, organizationId, unitId,
            List.of(WorkflowPolicy.AUTHORIZED));
    }

    @Transactional(readOnly = true)
    public ApprovalWorkflow requireExecuted(String operationType, String resource, long recordId,
            long organizationId, Long unitId) {
        return requireStateByRecord(operationType, resource, recordId, organizationId, unitId,
            List.of(WorkflowPolicy.EXECUTED));
    }

    @Transactional(readOnly = true)
    public ApprovalWorkflow requireConcluded(String operationType, String resource, long recordId,
            long organizationId, Long unitId) {
        return requireStateByRecord(operationType, resource, recordId, organizationId, unitId,
            List.of(WorkflowPolicy.CONCLUDED));
    }

    private ApprovalWorkflow requireStateByWorkflowId(long workflowId, String operationType, String resource,
            long organizationId, Long unitId, String acceptedState) {
        if (workflowId <= 0 || organizationId <= 0) {
            bad("Workflow and organization are required for workflow enforcement.");
        }
        String type = WorkflowPolicy.normalize(operationType);
        policy.rule(type);
        String normalizedResource = WorkflowPolicy.normalizeResource(resource);

        ApprovalWorkflow workflow = em.find(ApprovalWorkflow.class, workflowId, LockModeType.PESSIMISTIC_WRITE);
        if (workflow == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found.");
        }
        validateContext(workflow, type, normalizedResource, organizationId, unitId);
        if (!acceptedState.equals(workflow.status)) {
            conflict("Sensitive operation requires workflow state " + acceptedState
                + ", but workflow " + workflowId + " is " + workflow.status + ".");
        }
        return workflow;
    }

    private ApprovalWorkflow requireStateByRecord(String operationType, String resource, long recordId,
            long organizationId, Long unitId, List<String> acceptedStates) {
        String type = WorkflowPolicy.normalize(operationType);
        policy.rule(type);
        String normalizedResource = WorkflowPolicy.normalizeResource(resource);
        if (recordId <= 0 || organizationId <= 0) {
            bad("Record and organization are required for record-based workflow enforcement.");
        }

        String jpql = "select w from ApprovalWorkflow w where w.operationType=:operation and w.resource=:resource"
            + " and w.recordId=:record and w.organization.id=:organization"
            + (unitId == null ? " and w.unit is null" : " and w.unit.id=:unit")
            + " and w.status in :states order by w.id desc";
        var query = em.createQuery(jpql, ApprovalWorkflow.class)
            .setParameter("operation", type)
            .setParameter("resource", normalizedResource)
            .setParameter("record", recordId)
            .setParameter("organization", organizationId)
            .setParameter("states", acceptedStates)
            .setMaxResults(1);
        if (unitId != null) {
            query.setParameter("unit", unitId);
        }
        var result = query.getResultList();
        if (result.isEmpty()) {
            conflict("Sensitive operation requires workflow state " + String.join(" or ", acceptedStates) + ".");
        }
        return result.getFirst();
    }

    private static void validateContext(ApprovalWorkflow workflow, String operationType, String resource,
            long organizationId, Long unitId) {
        Long workflowUnitId = workflow.unit == null ? null : workflow.unit.id;
        if (!operationType.equals(workflow.operationType)
                || !resource.equals(workflow.resource)
                || workflow.organization == null
                || workflow.organization.id == null
                || workflow.organization.id != organizationId
                || !Objects.equals(workflowUnitId, unitId)) {
            conflict("Workflow does not match the protected operation context.");
        }
    }

    private static void bad(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static void conflict(String message) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
