package com.comandos.workflow.service;

import com.comandos.workflow.model.ApprovalWorkflow;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@Transactional(readOnly = true)
public class WorkflowApprovalGuard {
    private final EntityManager em;
    private final WorkflowPolicy policy;

    public WorkflowApprovalGuard(EntityManager em, WorkflowPolicy policy) {
        this.em = em;
        this.policy = policy;
    }

    public ApprovalWorkflow requireAuthorized(String operationType, String resource, long recordId,
            long organizationId, Long unitId) {
        return requireState(operationType, resource, recordId, organizationId, unitId,
            List.of(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.EXECUTED));
    }

    public ApprovalWorkflow requireExecuted(String operationType, String resource, long recordId,
            long organizationId, Long unitId) {
        return requireState(operationType, resource, recordId, organizationId, unitId,
            List.of(WorkflowPolicy.EXECUTED));
    }

    public ApprovalWorkflow requireConcluded(String operationType, String resource, long recordId,
            long organizationId, Long unitId) {
        return requireState(operationType, resource, recordId, organizationId, unitId,
            List.of(WorkflowPolicy.CONCLUDED));
    }

    private ApprovalWorkflow requireState(String operationType, String resource, long recordId,
            long organizationId, Long unitId, List<String> acceptedStates) {
        String type = WorkflowPolicy.normalize(operationType);
        policy.rule(type);
        if (resource == null || resource.isBlank() || recordId <= 0 || organizationId <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Operation, resource, record and organization are required for workflow enforcement.");
        String jpql = "select w from ApprovalWorkflow w where w.operationType=:operation and w.resource=:resource"
            + " and w.recordId=:record and w.organization.id=:organization"
            + (unitId == null ? " and w.unit is null" : " and w.unit.id=:unit")
            + " and w.status in :states order by w.id desc";
        var query = em.createQuery(jpql, ApprovalWorkflow.class)
            .setParameter("operation", type)
            .setParameter("resource", resource.trim())
            .setParameter("record", recordId)
            .setParameter("organization", organizationId)
            .setParameter("states", acceptedStates)
            .setMaxResults(1);
        if (unitId != null) query.setParameter("unit", unitId);
        var result = query.getResultList();
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Sensitive operation requires workflow state " + String.join(" or ", acceptedStates) + ".");
        }
        return result.getFirst();
    }
}
