package com.comandos.demo;

import com.comandos.core.model.*;
import com.comandos.inventory.model.*;
import com.comandos.lifecycle.model.*;
import com.comandos.maintenance.model.MaintenancePlan;
import com.comandos.workflow.model.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(55)
public class GovernanceLifecycleDemoSeeder implements ApplicationRunner {

    private final EntityManager em;

    public GovernanceLifecycleDemoSeeder(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization org = one(Organization.class, "acronym", "SSP-DEMO");
        OrganizationalUnit unit = one(OrganizationalUnit.class, "code", "ARM-CENTRAL");
        AssetItem asset = one(AssetItem.class, "assetCode", "PAT-DEMO-0002");
        StockLot lot = one(StockLot.class, "lotNumber", "CBC-DEMO-2026-001");
        StockLocation location = one(StockLocation.class, "code", "ARM-COFRE-01");
        StockBalance balance = lot == null || location == null ? null : balance(lot, location);
        if (org == null || unit == null || asset == null || lot == null || location == null || balance == null) return;

        seedMaintenancePlan(org, unit);
        ApprovalWorkflow workflow = seedWorkflow(org, unit);
        seedInspection(org, unit, asset);
        seedOccurrence(org, unit, lot, balance);
        seedAttachment(workflow);
        em.flush();
    }

    private void seedMaintenancePlan(Organization org, OrganizationalUnit unit) {
        if (count(MaintenancePlan.class) > 0) return;
        MaintenancePlan plan = new MaintenancePlan();
        plan.organization = org;
        plan.unit = unit;
        plan.name = "Plano semestral de manutenção preventiva - demonstração";
        plan.type = "PREVENTIVE";
        plan.periodicityDays = 180;
        plan.active = true;
        em.persist(plan);
    }

    private ApprovalWorkflow seedWorkflow(Organization org, OrganizationalUnit unit) {
        ApprovalWorkflow existing = first(ApprovalWorkflow.class);
        if (existing != null) return existing;

        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.organization = org;
        workflow.unit = unit;
        workflow.operationType = "INVENTORY_ADJUSTMENT";
        workflow.resource = "inventory-count";
        workflow.recordId = 1L;
        workflow.status = "AUTHORIZED";
        workflow.justification = "Autorização fictícia de ajuste após divergência identificada no inventário físico.";
        workflow.requestedAt = LocalDateTime.now().minusDays(3);
        workflow.requestedByLogin = "maria.armeira.demo";
        workflow.authorityLogin = "autoridade.demo";
        workflow.authorizedAt = LocalDateTime.now().minusDays(2);
        em.persist(workflow);

        ApprovalWorkflowEvent requested = new ApprovalWorkflowEvent();
        requested.workflow = workflow;
        requested.fromStatus = "DRAFT";
        requested.toStatus = "REQUESTED";
        requested.justification = "Solicitação encaminhada para análise.";
        requested.occurredAt = LocalDateTime.now().minusDays(3);
        requested.actorLogin = "maria.armeira.demo";
        em.persist(requested);

        ApprovalWorkflowEvent authorized = new ApprovalWorkflowEvent();
        authorized.workflow = workflow;
        authorized.fromStatus = "REQUESTED";
        authorized.toStatus = "AUTHORIZED";
        authorized.justification = "Ajuste autorizado no cenário didático.";
        authorized.occurredAt = LocalDateTime.now().minusDays(2);
        authorized.actorLogin = "autoridade.demo";
        em.persist(authorized);
        return workflow;
    }

    private void seedInspection(Organization org, OrganizationalUnit unit, AssetItem asset) {
        if (count(PeriodicInspection.class) > 0) return;
        PeriodicInspection inspection = new PeriodicInspection();
        inspection.organization = org;
        inspection.unit = unit;
        inspection.asset = asset;
        inspection.checklist = "Número de série; integridade; limpeza; funcionamento; carregadores; acessórios.";
        inspection.result = "APPROVED";
        inspection.damages = "Nenhum dano identificado.";
        inspection.inspectedAt = LocalDateTime.now().minusDays(10);
        inspection.responsibleId = 1L;
        inspection.responsibleLogin = "maria.armeira.demo";
        inspection.approvedByLogin = "autoridade.demo";
        inspection.approvedAt = LocalDateTime.now().minusDays(9);
        em.persist(inspection);
    }

    private void seedOccurrence(Organization org, OrganizationalUnit unit, StockLot lot, StockBalance balance) {
        if (count(ExceptionOccurrence.class) > 0) return;
        ExceptionOccurrence occurrence = new ExceptionOccurrence();
        occurrence.organization = org;
        occurrence.unit = unit;
        occurrence.lot = lot;
        occurrence.balance = balance;
        occurrence.type = "DIVERGENCE";
        occurrence.status = "OPEN";
        occurrence.quantity = new BigDecimal("2");
        occurrence.description = "Diferença fictícia de duas unidades identificada durante inventário físico.";
        occurrence.investigation = "Conferência de movimentações e documentos em andamento.";
        occurrence.documentReference = "SEI-DEMO-OCORRENCIA-001";
        occurrence.occurredAt = LocalDateTime.now().minusDays(2);
        occurrence.responsibleId = 1L;
        occurrence.responsibleLogin = "maria.armeira.demo";
        em.persist(occurrence);
    }

    private void seedAttachment(ApprovalWorkflow workflow) {
        if (workflow == null || count(OperationAttachment.class) > 0) return;
        OperationAttachment attachment = new OperationAttachment();
        attachment.resource = "approval-workflow";
        attachment.recordId = workflow.id;
        attachment.fileName = "parecer-ajuste-inventario-demo.txt";
        attachment.contentType = "text/plain";
        attachment.content = "Documento fictício criado exclusivamente para treinamento do COMANDOS."
            .getBytes(StandardCharsets.UTF_8);
        attachment.description = "Parecer didático vinculado ao fluxo de aprovação.";
        attachment.uploadedAt = LocalDateTime.now().minusDays(2);
        attachment.uploadedByLogin = "autoridade.demo";
        em.persist(attachment);
    }

    private StockBalance balance(StockLot lot, StockLocation location) {
        return em.createQuery("select b from StockBalance b where b.lot = :lot and b.location = :location", StockBalance.class)
            .setParameter("lot", lot)
            .setParameter("location", location)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private <T> T one(Class<T> type, String field, Object value) {
        return em.createQuery("select e from " + type.getSimpleName() + " e where e." + field + " = :value", type)
            .setParameter("value", value)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private <T> T first(Class<T> type) {
        return em.createQuery("select e from " + type.getSimpleName() + " e order by e.id", type)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private long count(Class<?> type) {
        return em.createQuery("select count(e) from " + type.getSimpleName() + " e", Long.class).getSingleResult();
    }
}
