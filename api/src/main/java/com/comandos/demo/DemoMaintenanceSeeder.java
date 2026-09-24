package com.comandos.demo;

import com.comandos.core.model.Organization;
import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.StockMovement;
import com.comandos.maintenance.model.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@Order(150)
public class DemoMaintenanceSeeder implements ApplicationRunner {

    private final EntityManager entityManager;

    public DemoMaintenanceSeeder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (count(WorkOrder.class) > 0) return;

        AssetItem asset = first(AssetItem.class);
        Organization organization = asset.location.organization;

        StockMovement issue = new StockMovement();
        issue.asset = asset;
        issue.location = asset.location;
        issue.nature = "MAINTENANCE_ISSUE";
        issue.quantity = BigDecimal.ONE;
        issue.movedAt = LocalDateTime.now().minusDays(2);
        issue.operatorLogin = "demo-seeder";
        issue.referenceType = "MAINTENANCE";
        issue.notes = "Movimento fictício para teste de manutenção.";
        entityManager.persist(issue);

        WorkOrder order = new WorkOrder();
        order.organization = organization;
        order.unit = asset.location.unit;
        order.asset = asset;
        order.issueMovement = issue;
        order.organizationName = organization.name;
        order.unitName = asset.location.unit == null ? null : asset.location.unit.name;
        order.assetCode = asset.assetCode;
        order.modelName = asset.model.name;
        order.locationName = asset.location.name;
        order.reason = "Inspeção preventiva fictícia para testes";
        order.maintenanceType = "PREVENTIVE";
        order.workshop = "Oficina Demo";
        order.gunsmith = "Armeiro Demo";
        order.sentAt = LocalDateTime.now().minusDays(2);
        order.nextMaintenanceAt = LocalDateTime.now().plusMonths(6);
        order.totalCost = new BigDecimal("350.00");
        order.status = "OPEN";
        order.openedAt = LocalDateTime.now().minusDays(2);
        order.openedByLogin = "demo-seeder";
        order.requestId = UUID.randomUUID().toString();
        order.requestFingerprint = UUID.randomUUID().toString().replace("-", "");
        entityManager.persist(order);

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.workOrder = order;
        diagnosis.defect = "Desgaste de demonstração";
        diagnosis.cause = "Uso operacional simulado";
        diagnosis.opinion = "Equipamento apto após manutenção preventiva.";
        entityManager.persist(diagnosis);

        ExecutedService service = new ExecutedService();
        service.workOrder = order;
        service.description = "Limpeza, inspeção e lubrificação geral.";
        service.cost = new BigDecimal("180.00");
        entityManager.persist(service);

        FunctionalTest test = new FunctionalTest();
        test.workOrder = order;
        test.testedAt = LocalDateTime.now().minusDays(1);
        test.result = "APPROVED";
        test.notes = "Teste funcional fictício aprovado.";
        entityManager.persist(test);

        MaintenancePart part = new MaintenancePart();
        part.workOrder = order;
        part.description = "Kit de manutenção demonstrativo";
        part.quantity = BigDecimal.ONE;
        part.unitCost = new BigDecimal("170.00");
        part.partNumber = "DEMO-PART-001";
        entityManager.persist(part);

        entityManager.flush();
    }

    private long count(Class<?> type) {
        String entityName = entityManager.getMetamodel().entity(type).getName();
        return entityManager.createQuery("select count(e) from " + entityName + " e", Long.class)
            .getSingleResult();
    }

    private <T> T first(Class<T> type) {
        String entityName = entityManager.getMetamodel().entity(type).getName();
        return entityManager.createQuery("select e from " + entityName + " e", type)
            .setMaxResults(1)
            .getSingleResult();
    }
}
