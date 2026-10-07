package com.comandos.demo;

import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.custody.model.Custody;
import com.comandos.custody.model.CustodyResponsibility;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1880)
public class CustodyResponsibilityDemoVerifier implements ApplicationRunner {
    private static final Set<String> AUTHORIZED = Set.of(
        "CUSTODY_RESPONSIBLE", "MATERIAL_RESPONSIBLE", "ARMORY_MANAGER", "WAREHOUSE_MANAGER",
        "UNIT_HEAD", "UNIT_MANAGER", "TEAM_LEADER", "OPERATION_COMMANDER",
        "RESPONSAVEL_CAUTELA", "RESPONSAVEL_MATERIAL", "RESPONSAVEL_ARMAMENTO", "ALMOXARIFE",
        "CHEFE_UNIDADE", "GESTOR_UNIDADE", "CHEFE_EQUIPE", "COMANDANTE_OPERACAO"
    );

    private final EntityManager em;

    public CustodyResponsibilityDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        List<Custody> custodies = em.createQuery("select c from Custody c", Custody.class).getResultList();
        for (Custody custody : custodies) {
            verifyMatrix(custody);
            if (custody.recipientUnitLegacyId != null) verifyInstitutionalResponsibility(custody);
        }
    }

    private void verifyMatrix(Custody custody) {
        String scope = custody.custodyScope == null ? "INDIVIDUAL" : custody.custodyScope;
        String duration = custody.durationType == null ? (custody.dueAt == null ? "PERMANENT" : "TEMPORARY") : custody.durationType;
        if (custody.recipientUnitLegacyId == null) {
            require(custody.recipientLegacyId != null, "person custody without recipient");
            require("INDIVIDUAL".equals(scope), "person custody must be INDIVIDUAL");
        } else {
            require(custody.recipientLegacyId == null, "institutional custody cannot also have person recipient");
            require(Set.of("COLLECTIVE", "TEAM", "OPERATION").contains(scope), "invalid institutional custody scope");
        }
        require(Set.of("TEMPORARY", "PERMANENT").contains(duration), "invalid custody duration");
        if ("TEMPORARY".equals(duration)) require(custody.dueAt != null, "temporary custody without due date");
        if ("PERMANENT".equals(duration)) require(custody.dueAt == null, "permanent custody with due date");
        if (Set.of("TEAM", "OPERATION").contains(scope)) {
            require("TEMPORARY".equals(duration), "team/operation custody must be temporary");
            require(custody.teamOperation != null && !custody.teamOperation.isBlank(), "team/operation custody without identification");
        }
    }

    private void verifyInstitutionalResponsibility(Custody custody) {
        CustodyResponsibility responsibility = em.createQuery(
                "select r from CustodyResponsibility r where r.custody.id = :custody", CustodyResponsibility.class)
            .setParameter("custody", custody.id)
            .getResultStream().findFirst().orElse(null);
        require(responsibility != null, "institutional custody without responsibility: custody=" + custody.id);
        require(responsibility.responsiblePersonLegacyId != null && responsibility.roleAssignmentLegacyId != null,
            "institutional responsibility without person/assignment ids");
        PersonRoleAssignment assignment = em.find(PersonRoleAssignment.class, responsibility.roleAssignmentLegacyId);
        require(assignment != null, "institutional responsibility role assignment not found");
        require(Objects.equals(assignment.person.id, responsibility.responsiblePersonLegacyId), "responsibility assignment person mismatch");
        require(Objects.equals(assignment.organization.id, custody.organizationLegacyId), "responsibility organization mismatch");
        require(assignment.unit != null && Objects.equals(assignment.unit.id, custody.recipientUnitLegacyId), "responsibility unit mismatch");
        require(assignment.role != null && AUTHORIZED.contains(assignment.role.code.toUpperCase()), "responsibility role not authorized");
        require("ACTIVE".equalsIgnoreCase(assignment.status), "responsibility assignment not active");
        require(assignment.startDate != null && !assignment.startDate.isAfter(LocalDate.now()), "responsibility assignment not started");
        require(assignment.endDate == null || !assignment.endDate.isBefore(LocalDate.now()), "responsibility assignment expired");
        require(responsibility.recordedAt != null, "responsibility historical timestamp missing");
        require(responsibility.responsiblePersonName != null && !responsibility.responsiblePersonName.isBlank(), "responsible person snapshot missing");
        require(responsibility.roleCode != null && !responsibility.roleCode.isBlank(), "responsibility role snapshot missing");
        require(responsibility.organizationName != null && !responsibility.organizationName.isBlank(), "responsibility organization snapshot missing");
        require(responsibility.unitName != null && !responsibility.unitName.isBlank(), "responsibility unit snapshot missing");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Custody responsibility regression failed: " + message);
    }
}
