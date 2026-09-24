package com.comandos.demo;

import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.StockMovement;
import com.comandos.inventory.model.StockMovementNature;
import com.comandos.maintenance.model.Diagnosis;
import com.comandos.maintenance.model.ExecutedService;
import com.comandos.maintenance.model.FunctionalTest;
import com.comandos.maintenance.model.MaintenancePart;
import com.comandos.maintenance.model.WorkOrder;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(200)
public class MaintenanceDemoSeeder implements ApplicationRunner {

    private final EntityManager entityManager;

    public MaintenanceDemoSeeder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        WorkOrder workOrder = firstWorkOrder();

        if (workOrder == null) {
            AssetItem asset = firstAsset();
            if (asset == null) return;

            StockMovement issue = new StockMovement();
            issue.asset = asset;
            issue.location = asset.location;
            issue.nature = StockMovementNature.MAINTENANCE_ISSUE.name();
            issue.quantity = BigDecimal.ONE;
            issue.movedAt = LocalDateTime.now().minusHours(2);
            issue.operatorLogin = "demo.seed";
            issue.notes = "Movimento fictício para validação de manutenção";
            entityManager.persist(issue);

            workOrder = new WorkOrder();
            workOrder.organization = asset.location.organization;
            workOrder.unit = asset.location.unit;
            workOrder.asset = asset;
            workOrder.issueMovement = issue;
            workOrder.organizationName = asset.location.organization.name;
            workOrder.unitName = asset.location.unit == null ? null : asset.location.unit.name;
            workOrder.assetCode = asset.assetCode;
            workOrder.modelName = asset.model.name;
            workOrder.locationName = asset.location.name;
            workOrder.reason = "Manutenção preventiva de demonstração";
            workOrder.maintenanceType = "PREVENTIVE";
            workOrder.workshop = "Oficina Demo";
            workOrder.gunsmith = "Armeiro Demo";
            workOrder.sentAt = LocalDateTime.now().minusHours(2);
            workOrder.nextMaintenanceAt = LocalDateTime.now().plusMonths(6);
            workOrder.totalCost = new BigDecimal("250.00");
            workOrder.status = "OPEN";
            workOrder.openedAt = LocalDateTime.now().minusHours(2);
            workOrder.openedByLogin = "demo.seed";
            workOrder.requestId = UUID.randomUUID().toString();
            workOrder.requestFingerprint = "demo-maintenance-" + UUID.randomUUID();
            entityManager.persist(workOrder);

            asset.status = "IN_MAINTENANCE";
        }

        final WorkOrder target = workOrder;
        if (count("Diagnosis") == 0) {
            Diagnosis diagnosis = new Diagnosis();
            diagnosis.workOrder = target;
            diagnosis.defect = "Desgaste preventivo de demonstração";
            diagnosis.cause = "Uso operacional simulado";
            diagnosis.opinion = "Recomendada limpeza, inspeção e substituição preventiva de componente.";
            entityManager.persist(diagnosis);
        }

        if (count("ExecutedService") == 0) {
            ExecutedService service = new ExecutedService();
            service.workOrder = target;
            service.description = "Limpeza técnica e revisão preventiva de demonstração";
            service.cost = new BigDecimal("150.00");
            entityManager.persist(service);
        }

        if (count("MaintenancePart") == 0) {
            MaintenancePart part = new MaintenancePart();
            part.workOrder = target;
            part.description = "Componente de reposição fictício";
            part.quantity = BigDecimal.ONE;
            part.unitCost = new BigDecimal("100.00");
            part.partNumber = "DEMO-PART-001";
            entityManager.persist(part);
        }

        if (count("FunctionalTest") == 0) {
            FunctionalTest test = new FunctionalTest();
            test.workOrder = target;
            test.testedAt = LocalDateTime.now().minusMinutes(30);
            test.result = "PASSED";
            test.notes = "Teste funcional fictício concluído sem anormalidades.";
            entityManager.persist(test);
        }

        entityManager.flush();
    }

    private WorkOrder firstWorkOrder() {
        return entityManager.createQuery("select w from WorkOrder w order by w.id", WorkOrder.class)
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    }

    private AssetItem firstAsset() {
        return entityManager.createQuery("select a from AssetItem a order by a.id", AssetItem.class)
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    }

    private long count(String entityName) {
        return entityManager.createQuery("select count(e) from " + entityName + " e", Long.class)
            .getSingleResult();
    }
}
