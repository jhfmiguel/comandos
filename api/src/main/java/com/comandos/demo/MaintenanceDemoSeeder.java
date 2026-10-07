package com.comandos.demo;

import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.AssetStatus;
import com.comandos.inventory.model.StockMovement;
import com.comandos.inventory.model.StockMovementNature;
import com.comandos.inventory.model.StockMovementReferenceType;
import com.comandos.maintenance.model.Diagnosis;
import com.comandos.maintenance.model.ExecutedService;
import com.comandos.maintenance.model.FunctionalTest;
import com.comandos.maintenance.model.MaintenancePart;
import com.comandos.maintenance.model.MaintenancePlan;
import com.comandos.maintenance.model.WorkOrder;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
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
        if (firstWorkOrder() != null) return;

        AssetItem asset = firstAsset();
        if (asset == null) return;

        LocalDateTime openedAt = LocalDateTime.now().minusHours(2);
        LocalDateTime completedAt = LocalDateTime.now().minusMinutes(30);

        MaintenancePlan plan = new MaintenancePlan();
        plan.organizationLegacyId = asset.location.organizationLegacyId;
        plan.unitLegacyId = asset.location.unitLegacyId;
        plan.name = "Plano preventivo demo";
        plan.type = "PREVENTIVE";
        plan.periodicityDays = 180;
        plan.active = true;
        entityManager.persist(plan);

        StockMovement issue = new StockMovement();
        issue.asset = asset;
        issue.location = asset.location;
        issue.nature = StockMovementNature.MAINTENANCE_ISSUE.name();
        issue.referenceType = StockMovementReferenceType.MAINTENANCE.name();
        issue.quantity = BigDecimal.ONE.negate();
        issue.movedAt = openedAt;
        issue.operatorLogin = "demo.seed";
        issue.notes = "Saída fictícia para manutenção preventiva";
        entityManager.persist(issue);

        WorkOrder workOrder = new WorkOrder();
        workOrder.organizationLegacyId = asset.location.organizationLegacyId;
        workOrder.unitLegacyId = asset.location.unitLegacyId;
        workOrder.plan = plan;
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
        workOrder.sentAt = openedAt;
        workOrder.openedAt = openedAt;
        workOrder.openedByLogin = "demo.seed";
        workOrder.requestId = UUID.randomUUID().toString();
        workOrder.requestFingerprint = demoFingerprint();
        workOrder.status = "OPEN";
        entityManager.persist(workOrder);
        entityManager.flush();
        issue.referenceId = workOrder.id;

        asset.status = AssetStatus.IN_MAINTENANCE.name();

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.workOrder = workOrder;
        diagnosis.defect = "Desgaste preventivo de demonstração";
        diagnosis.cause = "Uso operacional simulado";
        diagnosis.opinion = "Recomendada limpeza, inspeção e substituição preventiva de componente.";
        entityManager.persist(diagnosis);

        ExecutedService service = new ExecutedService();
        service.workOrder = workOrder;
        service.description = "Limpeza técnica e revisão preventiva de demonstração";
        service.cost = new BigDecimal("150.00");
        entityManager.persist(service);

        MaintenancePart part = new MaintenancePart();
        part.workOrder = workOrder;
        part.description = "Componente de reposição fictício";
        part.quantity = BigDecimal.ONE;
        part.unitCost = new BigDecimal("100.00");
        part.partNumber = "DEMO-PART-001";
        entityManager.persist(part);

        FunctionalTest test = new FunctionalTest();
        test.workOrder = workOrder;
        test.testedAt = completedAt;
        test.result = "APPROVED";
        test.notes = "Teste funcional fictício aprovado sem anormalidades.";
        entityManager.persist(test);

        StockMovement returned = new StockMovement();
        returned.asset = asset;
        returned.location = asset.location;
        returned.nature = StockMovementNature.MAINTENANCE_RETURN.name();
        returned.referenceType = StockMovementReferenceType.MAINTENANCE.name();
        returned.referenceId = workOrder.id;
        returned.quantity = BigDecimal.ONE;
        returned.movedAt = completedAt;
        returned.operatorLogin = "demo.seed";
        returned.notes = "Retorno fictício após manutenção preventiva";
        entityManager.persist(returned);

        workOrder.returnMovement = returned;
        workOrder.totalCost = new BigDecimal("250.00");
        workOrder.completedAt = completedAt;
        workOrder.returnedAt = completedAt;
        workOrder.nextMaintenanceAt = completedAt.plusDays(plan.periodicityDays);
        workOrder.status = "COMPLETED";
        workOrder.completedByLogin = "demo.seed";
        workOrder.completionRequestId = UUID.randomUUID().toString();
        workOrder.completionFingerprint = demoFingerprint();
        asset.status = asset.validUntil == null || !asset.validUntil.isBefore(LocalDate.now())
            ? AssetStatus.AVAILABLE.name()
            : AssetStatus.BLOCKED.name();

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
        return entityManager.createQuery("select a from AssetItem a where a.status in ('AVAILABLE','BLOCKED') order by a.id", AssetItem.class)
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    }

    private static String demoFingerprint() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
