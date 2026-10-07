package com.comandos.custody.service;

import com.comandos.core.service.CanonicalMasterDataDirectory;
import com.comandos.core.service.CanonicalPartyRoleDirectory;
import com.comandos.core.service.ProductCanonicalScopeResolver;
import com.comandos.custody.dto.CustodyContract.*;
import com.comandos.custody.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;

@Service
public class CustodyIssueFacade {
    private static final Set<String> INSTITUTIONAL_RESPONSIBILITY_ROLES = Set.of(
        "CUSTODY_RESPONSIBLE", "MATERIAL_RESPONSIBLE", "ARMORY_MANAGER", "WAREHOUSE_MANAGER",
        "UNIT_HEAD", "UNIT_MANAGER", "TEAM_LEADER", "OPERATION_COMMANDER",
        "RESPONSAVEL_CAUTELA", "RESPONSAVEL_MATERIAL", "RESPONSAVEL_ARMAMENTO", "ALMOXARIFE",
        "CHEFE_UNIDADE", "GESTOR_UNIDADE", "CHEFE_EQUIPE", "COMANDANTE_OPERACAO"
    );

    private static final Set<String> INSTITUTIONAL_SCOPES = Set.of("COLLECTIVE", "TEAM", "OPERATION");
    private static final Set<String> DURATION_TYPES = Set.of("TEMPORARY", "PERMANENT");

    private static final TenantId TENANT = TenantId.of("comandos");

    private final EntityManager em;
    private final CustodyService custodyService;
    private final CanonicalMasterDataDirectory masterData;
    private final CanonicalPartyRoleDirectory partyRoles;
    private final ProductCanonicalScopeResolver canonicalScope;

    public CustodyIssueFacade(
            EntityManager em,
            CustodyService custodyService,
            CanonicalMasterDataDirectory masterData,
            CanonicalPartyRoleDirectory partyRoles,
            ProductCanonicalScopeResolver canonicalScope) {
        this.em = em;
        this.custodyService = custodyService;
        this.masterData = masterData;
        this.partyRoles = partyRoles;
        this.canonicalScope = canonicalScope;
    }

    @Transactional
    public CustodyView issueIndividual(IssueRequest request) {
        if (request == null) bad("Custody data is required.");
        String recipientType = normalized(request.recipientType(), request.recipientUnitId() == null ? "PERSON" : "UNIT");
        String scope = normalized(request.custodyScope(), "INDIVIDUAL");
        String duration = normalized(request.durationType(), blank(request.dueAt()) ? "PERMANENT" : "TEMPORARY");
        if (!"PERSON".equals(recipientType) || request.recipientId() == null || request.recipientUnitId() != null)
            bad("Individual custody must be issued to exactly one person.");
        if (!"INDIVIDUAL".equals(scope))
            bad("Collective, team and operation custody must use the institutional custody endpoint.");
        validateDuration(duration, request.dueAt(), scope);
        return custodyService.issue(request);
    }

    @Transactional
    public CustodyView issueInstitutional(InstitutionalIssueRequest request) {
        if (request == null || request.organizationId() == null || request.recipientUnitId() == null
                || request.responsiblePersonId() == null || request.responsibilityRoleAssignmentId() == null)
            bad("Organization, receiving unit, responsible person and responsibility assignment are required.");

        String scope = normalized(request.custodyScope(), "COLLECTIVE");
        String duration = normalized(request.durationType(), blank(request.dueAt()) ? "PERMANENT" : "TEMPORARY");
        if (!INSTITUTIONAL_SCOPES.contains(scope))
            bad("Institutional custody scope must be COLLECTIVE, TEAM or OPERATION.");
        validateDuration(duration, request.dueAt(), scope);
        if (("TEAM".equals(scope) || "OPERATION".equals(scope)) && blank(request.teamOperation()))
            bad("Team or operation custody requires its team/operation identification.");
        if ("COLLECTIVE".equals(scope) && !blank(request.teamOperation()))
            bad("Collective unit custody must not use a team/operation identification.");

        OrganizationSnapshot organization = organization(request.organizationId());
        UnitSnapshot recipientUnit = unit(organization.id(), request.recipientUnitId());
        PersonSnapshot responsible = person(request.responsiblePersonId());
        CanonicalPartyRoleDirectory.ProductSpecificRole assignment =
            responsibilityAssignment(
                responsible.id(),
                request.responsibilityRoleAssignmentId()
            );
        validateResponsibility(organization, recipientUnit, responsible, assignment);

        IssueRequest delegated = new IssueRequest(
            request.requestId(), request.organizationId(), request.unitId(), null, request.authorizerId(), request.purpose(),
            request.dueAt(), request.assetIds(), request.equipmentSetIds(), request.recipientUnitId(), "UNIT", scope, duration,
            request.teamOperation(), request.responsibilityTerm(), request.deliveryCondition(), request.accessories()
        );
        CustodyView issued = custodyService.issue(delegated);
        bindResponsibility(issued.id(), organization, recipientUnit, responsible, assignment);
        return issued;
    }

    @Transactional(readOnly = true)
    public CustodyResponsibilityView responsibility(long custodyId) {
        CustodyResponsibility value = em.createQuery(
                "select r from CustodyResponsibility r where r.custody.id = :custody", CustodyResponsibility.class)
            .setParameter("custody", custodyId)
            .getResultStream().findFirst().orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Institutional custody responsibility not found."));
        return view(value);
    }

    private void validateResponsibility(
            OrganizationSnapshot organization,
            UnitSnapshot unit,
            PersonSnapshot responsible,
            CanonicalPartyRoleDirectory.ProductSpecificRole assignment) {
        if (!organization.active() || !unit.active() || !responsible.active())
            bad("Organization, receiving unit and responsible person must be active.");
        if (assignment.personId() != responsible.id())
            bad("The responsibility assignment must belong to the selected responsible person.");
        if (assignment.organizationId() != organization.id())
            bad("The responsibility assignment must belong to the selected organization.");
        if (!Objects.equals(assignment.unitId(), unit.id()))
            bad("Institutional custody responsibility requires a role assignment in the receiving unit.");
        if (blank(assignment.code())
                || !INSTITUTIONAL_RESPONSIBILITY_ROLES.contains(
                    assignment.code().trim().toUpperCase(Locale.ROOT)))
            bad("The selected role does not authorize responsibility for institutional custody.");
        if (!"ACTIVE".equalsIgnoreCase(assignment.status()))
            bad("The responsibility role assignment must be active.");
        LocalDate today = LocalDate.now();
        if (assignment.validFrom() == null || assignment.validFrom().isAfter(today)
                || assignment.validUntil() != null && assignment.validUntil().isBefore(today))
            bad("The responsibility role assignment must be currently valid.");
    }

    private void bindResponsibility(
            long custodyId,
            OrganizationSnapshot organization,
            UnitSnapshot unit,
            PersonSnapshot responsible,
            CanonicalPartyRoleDirectory.ProductSpecificRole assignment) {
        Custody custody = em.find(Custody.class, custodyId, LockModeType.PESSIMISTIC_WRITE);
        if (custody == null) throw new IllegalStateException("Issued custody not found.");
        CustodyResponsibility existing = em.createQuery(
                "select r from CustodyResponsibility r where r.custody.id = :custody", CustodyResponsibility.class)
            .setParameter("custody", custodyId).setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultStream().findFirst().orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.responsiblePersonLegacyId, responsible.id())
                    || !Objects.equals(existing.roleAssignmentLegacyId, assignment.assignmentId()))
                conflict("This custody request is already bound to a different institutional responsibility.");
            return;
        }
        CustodyResponsibility value = new CustodyResponsibility();
        value.custody = custody;
        value.responsiblePersonLegacyId = responsible.id();
        value.responsiblePersonCanonicalId =
            canonicalScope == null ? null : canonicalScope.person(responsible.id());
        value.roleAssignmentLegacyId = assignment.assignmentId();
        value.responsiblePersonName = responsible.name();
        value.roleCode = assignment.code();
        value.roleName = assignment.name();
        value.organizationName = organization.name();
        value.unitName = unit.name();
        value.recordedAt = LocalDateTime.now();
        em.persist(value);
    }

    private static void validateDuration(String duration, String dueAt, String scope) {
        if (!DURATION_TYPES.contains(duration)) bad("Custody duration must be TEMPORARY or PERMANENT.");
        if ("TEMPORARY".equals(duration) && blank(dueAt))
            bad("Temporary custody requires a due date.");
        if ("PERMANENT".equals(duration) && !blank(dueAt))
            bad("Permanent custody must not have a due date.");
        if (("TEAM".equals(scope) || "OPERATION".equals(scope)) && !"TEMPORARY".equals(duration))
            bad("Team and operation custody must be temporary.");
    }

    private CustodyResponsibilityView view(CustodyResponsibility value) {
        return new CustodyResponsibilityView(
            value.custody.id,
            value.responsiblePersonLegacyId,
            value.responsiblePersonName,
            value.roleAssignmentLegacyId,
            value.roleCode,
            value.roleName,
            value.organizationName,
            value.unitName,
            value.recordedAt.toString());
    }

    private CanonicalPartyRoleDirectory.ProductSpecificRole responsibilityAssignment(
            long personId,
            Long assignmentId) {
        if (assignmentId == null || assignmentId <= 0) {
            bad("A valid responsibility assignment is required.");
        }
        return partyRoles.productSpecificRoles(personId).stream()
            .filter(role -> role.assignmentId() == assignmentId)
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Responsibility role assignment not found."
            ));
    }

    private OrganizationSnapshot organization(Long id) {
        if (id == null || id <= 0) bad("A valid organization is required.");
        var value = masterData.findOrganization(id, TENANT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Organization not found."
            ));
        return new OrganizationSnapshot(
            id,
            value.legalName(),
            value.status() == LifecycleStatus.ACTIVE
        );
    }

    private UnitSnapshot unit(long organizationId, Long id) {
        if (id == null || id <= 0) bad("A valid receiving unit is required.");
        var value = masterData.findUnit(id, TENANT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Receiving unit not found."
            ));
        if (!CompanyId.of("comandos:organization:" + organizationId)
                .equals(value.companyId())) {
            bad("The receiving unit must belong to the selected organization.");
        }
        return new UnitSnapshot(id, value.name(), value.active());
    }

    private PersonSnapshot person(Long id) {
        if (id == null || id <= 0) bad("A valid responsible person is required.");
        var value = masterData.findPerson(id, TENANT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Responsible person not found."
            ));
        return new PersonSnapshot(
            id,
            value.name(),
            value.status() == LifecycleStatus.ACTIVE
        );
    }

    private record OrganizationSnapshot(Long id, String name, boolean active) {}
    private record UnitSnapshot(Long id, String name, boolean active) {}
    private record PersonSnapshot(Long id, String name, boolean active) {}

    private <T> T locked(Class<T> type, Long id) {
        if (id == null || id <= 0) bad("A valid record ID is required.");
        T value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) bad(type.getSimpleName() + " not found.");
        return value;
    }

    private static String normalized(String value, String fallback) {
        return blank(value) ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
