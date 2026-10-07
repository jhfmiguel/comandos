package com.comandos.workflow.service;

import com.comandos.audit.service.AuditService;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.service.ProductMasterDataReferenceSynchronizer;
import com.comandos.core.service.ProductCanonicalScopeResolver;
import com.comandos.security.service.AccessPolicy;
import com.fariamiguel.security.api.CurrentActor;
import com.fariamiguel.security.api.CurrentActorProvider;
import com.comandos.workflow.api.*;
import com.comandos.workflow.model.ApprovalWorkflow;
import com.comandos.workflow.model.ApprovalWorkflowEvent;
import com.fariamiguel.core.api.PlatformPage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class WorkflowService implements SensitiveWorkflowGateway {
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;
    private final CurrentActorProvider actors;
    private final WorkflowPolicy policy;
    private final ProductMasterDataReferenceSynchronizer masterDataReferences;
    private final ProductCanonicalScopeResolver canonicalScope;

    @org.springframework.beans.factory.annotation.Autowired
    public WorkflowService(EntityManager em, AccessPolicy access, AuditService audit,
            CurrentActorProvider actors, WorkflowPolicy policy,
            ProductMasterDataReferenceSynchronizer masterDataReferences,
            ProductCanonicalScopeResolver canonicalScope) {
        this.em = em;
        this.access = access;
        this.audit = audit;
        this.actors = actors;
        this.policy = policy;
        this.masterDataReferences = masterDataReferences;
        this.canonicalScope = canonicalScope;
    }


    @Transactional
    public WorkflowView request(WorkflowRequest r) {
        if (r == null || r.organizationId() == null || r.organizationId() <= 0 || blank(r.operationType())
                || blank(r.resource()) || blank(r.justification())) {
            bad("Organization, operation, resource and justification are required.");
        }
        String operationType = WorkflowPolicy.normalize(r.operationType());
        policy.rule(operationType);
        String resource = WorkflowPolicy.normalizeResource(r.resource());
        access.requireScope(resource, policy.permissionForTransition(WorkflowPolicy.REQUESTED), r.organizationId(), r.unitId());

        Organization organization = find(Organization.class, r.organizationId());
        OrganizationalUnit unit = r.unitId() == null
            ? null
            : find(OrganizationalUnit.class, r.unitId());
        if (unit != null && !unit.organization.id.equals(organization.id)) {
            bad("Unit does not belong to organization.");
        }

        var w = new ApprovalWorkflow();
        w.organizationLegacyId = organization.id;
        w.unitLegacyId = unit == null ? null : unit.id;
        w.organizationCanonicalId = canonicalScope.organization(organization.id);
        w.unitCanonicalId = canonicalScope.unit(unit == null ? null : unit.id);
        if (r.recordId() != null && r.recordId() <= 0) {
            bad("Record id must be positive when informed.");
        }

        w.operationType = operationType;
        w.resource = resource;
        w.recordId = r.recordId();
        w.status = WorkflowPolicy.REQUESTED;
        w.justification = requiredJustification(r.justification());
        w.requestedAt = LocalDateTime.now();
        CurrentActor actor = requiredActor();
        w.requestedById = actorId(actor);
        w.requestedByLogin = actor.displayName();
        if (masterDataReferences != null) {
            if (canonicalScope != null && canonicalScope.enabled()) {
                masterDataReferences.synchronizeForBackfill(w);
            } else {
                masterDataReferences.synchronize(w);
            }
        }
        em.persist(w);
        em.flush();

        event(w, "NONE", WorkflowPolicy.REQUESTED, w.justification, actor, w.requestedAt);
        em.flush();
        var view = view(w);
        audit.record("approval-workflows", w.id, "CREATE", null, view);
        return view;
    }

    @Transactional
    public WorkflowView analyze(long id, WorkflowTransition r) {
        return move(id, WorkflowPolicy.ANALYZED, r);
    }

    @Transactional
    public WorkflowView authorize(long id, WorkflowTransition r) {
        return move(id, WorkflowPolicy.AUTHORIZED, r);
    }

    @Transactional
    public WorkflowView execute(long id, WorkflowTransition r) {
        return move(id, WorkflowPolicy.EXECUTED, r);
    }

    @Transactional
    public WorkflowView conclude(long id, WorkflowTransition r) {
        return move(id, WorkflowPolicy.CONCLUDED, r);
    }

    @Transactional
    public WorkflowView cancel(long id, WorkflowTransition r) {
        return move(id, WorkflowPolicy.CANCELLED, r);
    }

    @Transactional
    public WorkflowView bindRecordId(long id, long recordId) {
        if (recordId <= 0) bad("Record id must be positive.");
        var w = locked(id);
        if (!WorkflowPolicy.EXECUTED.equals(w.status)) {
            conflict("Record can only be linked while workflow is EXECUTED.");
        }
        if (w.recordId != null) {
            if (w.recordId == recordId) return view(w);
            conflict("Workflow is already linked to another record.");
        }

        access.requireScope(w.resource, policy.permissionForTransition(WorkflowPolicy.EXECUTED),
            w.organizationLegacyId, w.unitLegacyId);
        requiredActor();
        var before = view(w);
        w.recordId = recordId;
        em.flush();
        var after = view(w);
        audit.record("approval-workflows", w.id, "BIND_RECORD", before, after);
        return after;
    }

    public WorkflowView get(long id) {
        var w = find(ApprovalWorkflow.class, id);
        access.requireScope(w.resource, "READ", w.organizationLegacyId, w.unitLegacyId);
        return view(w);
    }

    public PlatformPage<WorkflowView> list(long org, Long unit, String status, int page) {
        access.requireScope("inventory/assets", "READ", org, unit);
        boolean canonical = canonicalScope != null && canonicalScope.enabled();
        var scope = canonical ? canonicalScope.scope(org, unit) : null;
        String where = " where "
            + (canonical ? "w.organizationCanonicalId" : "w.organizationLegacyId")
            + "=:o"
            + (unit == null ? "" : " and "
                + (canonical ? "w.unitCanonicalId" : "w.unitLegacyId")
                + "=:u")
            + (blank(status) ? "" : " and w.status=:s");
        var q = em.createQuery("select w from ApprovalWorkflow w" + where + " order by w.id desc", ApprovalWorkflow.class);
        var c = em.createQuery("select count(w) from ApprovalWorkflow w" + where, Long.class);
        q.setParameter("o", canonical ? scope.organizationId() : org);
        c.setParameter("o", canonical ? scope.organizationId() : org);
        if (unit != null) {
            q.setParameter("u", canonical ? scope.unitId() : unit);
            c.setParameter("u", canonical ? scope.unitId() : unit);
        }
        if (!blank(status)) {
            String normalizedStatus = WorkflowPolicy.normalizeStatus(status);
            q.setParameter("s", normalizedStatus);
            c.setParameter("s", normalizedStatus);
        }
        return new PlatformPage<>(q.setFirstResult(Math.max(page, 0) * 20).setMaxResults(20).getResultList().stream()
            .map(this::view).toList(), c.getSingleResult(), Math.max(page, 0), 20);
    }

    private WorkflowView move(long id, String target, WorkflowTransition transition) {
        var w = locked(id);
        policy.rule(w.operationType);
        policy.requireTransition(w.status, target);
        access.requireScope(w.resource, policy.permissionForTransition(target),
            w.organizationLegacyId, w.unitLegacyId);

        String reason = requiredTransitionJustification(transition);
        CurrentActor actor = requiredActor();
        var before = view(w);
        String from = w.status;
        LocalDateTime now = LocalDateTime.now();
        w.status = target;

        if (WorkflowPolicy.AUTHORIZED.equals(target)) {
            w.authorityId = actorId(actor);
            w.authorityLogin = actor.displayName();
            w.authorizedAt = now;
        }
        if (WorkflowPolicy.EXECUTED.equals(target)) w.executedAt = now;
        if (WorkflowPolicy.CONCLUDED.equals(target)) w.concludedAt = now;
        if (WorkflowPolicy.CANCELLED.equals(target)) w.cancelledAt = now;

        event(w, from, target, reason, actor, now);
        em.flush();
        var after = view(w);
        audit.record("approval-workflows", w.id, target, before, after);
        return after;
    }

    private void event(ApprovalWorkflow w, String from, String to, String reason,
            CurrentActor actor, LocalDateTime occurredAt) {
        var e = new ApprovalWorkflowEvent();
        e.workflow = w;
        e.fromStatus = from;
        e.toStatus = to;
        e.justification = requiredJustification(reason);
        e.occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
        e.actorId = actorId(actor);
        e.actorLogin = actor.displayName();
        em.persist(e);
    }

    private WorkflowView view(ApprovalWorkflow w) {
        var events = em.createQuery(
            "select e from ApprovalWorkflowEvent e where e.workflow.id=:id order by e.id",
            ApprovalWorkflowEvent.class)
            .setParameter("id", w.id)
            .getResultList().stream()
            .map(e -> new WorkflowEventView(e.id, e.fromStatus, e.toStatus, e.justification,
                e.occurredAt.toString(), e.actorLogin))
            .toList();
        return new WorkflowView(w.id, w.organizationLegacyId, w.unitLegacyId,
            w.operationType, w.resource, w.recordId, w.status, w.justification, w.requestedAt.toString(),
            w.requestedByLogin, w.authorityLogin, s(w.authorizedAt), s(w.executedAt),
            s(w.concludedAt), s(w.cancelledAt), events);
    }

    private ApprovalWorkflow locked(long id) {
        if (id <= 0) bad("Workflow id must be positive.");
        var w = em.find(ApprovalWorkflow.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (w == null) notFound();
        return w;
    }

    private <T> T find(Class<T> type, long id) {
        var value = em.find(type, id);
        if (value == null) notFound();
        return value;
    }

    private CurrentActor requiredActor() {
        CurrentActor actor = actors.currentActor();
        if (actor == null || !actor.authenticated() || blank(actor.displayName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "An authenticated and identified actor is required for workflow transitions.");
        }
        return actor;
    }

    private static Long actorId(CurrentActor actor) {
        if (actor == null || actor.id() == null || actor.id().isBlank()
                || "anonymous".equalsIgnoreCase(actor.id())) {
            return null;
        }
        try {
            return Long.valueOf(actor.id());
        } catch (NumberFormatException ignored) {
            return Integer.toUnsignedLong(actor.id().hashCode());
        }
    }

    private static String requiredTransitionJustification(WorkflowTransition transition) {
        if (transition == null || blank(transition.justification())) {
            bad("Justification is required for every workflow transition.");
        }
        return requiredJustification(transition.justification());
    }

    private static String requiredJustification(String value) {
        if (blank(value)) bad("Justification is required.");
        String justification = value.trim();
        if (justification.length() > 2000) bad("Justification must contain at most 2000 characters.");
        return justification;
    }

    private static String s(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void bad(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static void conflict(String message) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static void notFound() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found.");
    }
}
