package com.comandos.demo;

import com.comandos.workflow.model.ApprovalWorkflow;
import com.comandos.workflow.model.ApprovalWorkflowEvent;
import com.comandos.workflow.service.WorkflowPolicy;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1960)
public class WorkflowPolicyDemoVerifier implements ApplicationRunner {
    private final EntityManager em;
    private final WorkflowPolicy policy;

    public WorkflowPolicyDemoVerifier(EntityManager em, WorkflowPolicy policy) {
        this.em = em;
        this.policy = policy;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        verifyPolicyMatrix();
        verifyPersistedWorkflows();
    }

    private void verifyPolicyMatrix() {
        if (policy.rules().size() < 15)
            fail("sensitive operation matrix is incomplete");
        policy.rules().forEach((type, rule) -> {
            if (!type.equals(rule.operationType())) fail("operation key mismatch for " + type);
            if (!rule.approvalRequired()) fail("approval must be required for " + type);
            if (!rule.analysisRequired()) fail("analysis must be required for " + type);
        });
        if (!"CREATE".equals(policy.permissionForTransition(WorkflowPolicy.REQUESTED))) fail("request permission");
        if (!"UPDATE".equals(policy.permissionForTransition(WorkflowPolicy.ANALYZED))) fail("analysis permission");
        if (!"APPROVE".equals(policy.permissionForTransition(WorkflowPolicy.AUTHORIZED))) fail("authorization permission");
        if (!"UPDATE".equals(policy.permissionForTransition(WorkflowPolicy.EXECUTED))) fail("execution permission");
        if (!"UPDATE".equals(policy.permissionForTransition(WorkflowPolicy.CONCLUDED))) fail("conclusion permission");
    }

    private void verifyPersistedWorkflows() {
        var workflows = em.createQuery("select w from ApprovalWorkflow w order by w.id", ApprovalWorkflow.class).getResultList();
        for (var workflow : workflows) {
            policy.rule(workflow.operationType);
            if (workflow.organizationLegacyId == null) fail("workflow without organization: " + workflow.id);
            if (blank(workflow.resource)) fail("workflow without permission resource: " + workflow.id);
            if (blank(workflow.justification)) fail("workflow without justification: " + workflow.id);
            if (workflow.requestedAt == null) fail("workflow without request timestamp: " + workflow.id);
            if (blank(workflow.requestedByLogin)) fail("workflow without requester actor: " + workflow.id);
            verifyEvents(workflow);
            verifyMilestones(workflow);
        }
    }

    private void verifyEvents(ApprovalWorkflow workflow) {
        var events = em.createQuery(
            "select e from ApprovalWorkflowEvent e where e.workflow.id=:id order by e.id",
            ApprovalWorkflowEvent.class).setParameter("id", workflow.id).getResultList();
        if (events.isEmpty()) fail("workflow without event history: " + workflow.id);

        var first = events.getFirst();
        if (!"NONE".equals(first.fromStatus) || !WorkflowPolicy.REQUESTED.equals(first.toStatus))
            fail("workflow must start at REQUESTED: " + workflow.id);
        if (blank(first.justification)) fail("request event without justification for workflow " + workflow.id);
        if (first.occurredAt == null) fail("request event without timestamp for workflow " + workflow.id);
        if (blank(first.actorLogin)) fail("request event without actor for workflow " + workflow.id);
        if (first.occurredAt.isBefore(workflow.requestedAt)) fail("request event before workflow request: " + workflow.id);

        String previousStatus = WorkflowPolicy.REQUESTED;
        LocalDateTime previousTimestamp = first.occurredAt;
        for (int i = 1; i < events.size(); i++) {
            var event = events.get(i);
            if (!previousStatus.equals(event.fromStatus)) fail("broken event chain for workflow " + workflow.id);
            if (blank(event.justification)) fail("event without justification for workflow " + workflow.id);
            if (event.occurredAt == null) fail("event without timestamp for workflow " + workflow.id);
            if (blank(event.actorLogin)) fail("event without actor for workflow " + workflow.id);
            if (event.occurredAt.isBefore(previousTimestamp)) fail("non-monotonic event timestamp for workflow " + workflow.id);

            try {
                policy.requireTransition(event.fromStatus, event.toStatus);
            } catch (RuntimeException exception) {
                fail("invalid workflow transition for " + workflow.id + ": " + exception.getMessage());
            }
            previousStatus = event.toStatus;
            previousTimestamp = event.occurredAt;
        }
        if (!workflow.status.equals(previousStatus)) fail("workflow status diverges from event history: " + workflow.id);
    }

    private void verifyMilestones(ApprovalWorkflow workflow) {
        Set<String> statuses = Set.of(WorkflowPolicy.REQUESTED, WorkflowPolicy.ANALYZED, WorkflowPolicy.AUTHORIZED,
            WorkflowPolicy.EXECUTED, WorkflowPolicy.CONCLUDED, WorkflowPolicy.CANCELLED);
        if (!statuses.contains(workflow.status)) fail("unknown workflow status: " + workflow.status);
        if (Set.of(WorkflowPolicy.AUTHORIZED, WorkflowPolicy.EXECUTED, WorkflowPolicy.CONCLUDED).contains(workflow.status)) {
            if (workflow.authorizedAt == null || blank(workflow.authorityLogin))
                fail("authorized workflow without authority evidence: " + workflow.id);
        }
        if (Set.of(WorkflowPolicy.EXECUTED, WorkflowPolicy.CONCLUDED).contains(workflow.status) && workflow.executedAt == null)
            fail("executed workflow without timestamp: " + workflow.id);
        if (WorkflowPolicy.CONCLUDED.equals(workflow.status) && workflow.concludedAt == null)
            fail("concluded workflow without timestamp: " + workflow.id);
        if (WorkflowPolicy.CANCELLED.equals(workflow.status) && workflow.cancelledAt == null)
            fail("cancelled workflow without timestamp: " + workflow.id);
        if (workflow.authorizedAt != null && workflow.authorizedAt.isBefore(workflow.requestedAt)) fail("authorization before request");
        if (workflow.executedAt != null && workflow.authorizedAt != null && workflow.executedAt.isBefore(workflow.authorizedAt)) fail("execution before authorization");
        if (workflow.concludedAt != null && workflow.executedAt != null && workflow.concludedAt.isBefore(workflow.executedAt)) fail("conclusion before execution");
        if (workflow.cancelledAt != null && workflow.cancelledAt.isBefore(workflow.requestedAt)) fail("cancellation before request");
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void fail(String detail) { throw new IllegalStateException("Oracle workflow regression failed: " + detail); }
}
