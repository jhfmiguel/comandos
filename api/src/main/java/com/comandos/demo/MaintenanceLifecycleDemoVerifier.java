package com.comandos.demo;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(2100)
public class MaintenanceLifecycleDemoVerifier implements ApplicationRunner {

    private static final Set<String> TYPES = Set.of("PREVENTIVE", "CORRECTIVE");
    private static final Set<String> TEST_RESULTS = Set.of("APPROVED", "REJECTED");

    private final EntityManager entityManager;

    public MaintenanceLifecycleDemoVerifier(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<WorkOrder> orders = entityManager.createQuery(
            "select w from WorkOrder w order by w.id", WorkOrder.class
        ).getResultList();
        require(!orders.isEmpty(), "Maintenance demo must contain at least one work order.");

        Set<Long> issueMovements = new HashSet<>();
        Set<Long> returnMovements = new HashSet<>();
        boolean preventive = false;
        boolean completed = false;

        for (WorkOrder workOrder : orders) {
            verifyOrderIdentity(workOrder);
            preventive |= "PREVENTIVE".equals(workOrder.maintenanceType);
            completed |= "COMPLETED".equals(workOrder.status);

            require(issueMovements.add(workOrder.issueMovement.id),
                "A maintenance issue movement cannot belong to more than one work order.");
            verifyIssueMovement(workOrder);

            if ("OPEN".equals(workOrder.status)) {
                require(workOrder.completedAt == null && workOrder.returnedAt == null && workOrder.returnMovement == null,
                    "Open maintenance cannot already contain completion/return metadata.");
                require("IN_MAINTENANCE".equals(workOrder.asset.status),
                    "An asset with an open maintenance order must remain unavailable in maintenance.");
            } else if ("COMPLETED".equals(workOrder.status)) {
                verifyCompleted(workOrder, returnMovements);
            } else {
                fail("Unsupported maintenance status: " + workOrder.status);
            }
        }

        require(preventive, "Maintenance demo must prove preventive maintenance.");
        require(completed, "Maintenance demo must prove a complete maintenance lifecycle.");
        verifyPlans();
    }

    private void verifyOrderIdentity(WorkOrder workOrder) {
        require(workOrder.organization != null && workOrder.asset != null && workOrder.asset.location != null,
            "Maintenance order must retain organization, asset and location provenance.");
        require(workOrder.asset.location.organization.id.equals(workOrder.organization.id),
            "Maintenance asset and order organization must match.");
        require(TYPES.contains(workOrder.maintenanceType),
            "Maintenance type must be PREVENTIVE or CORRECTIVE.");
        require(workOrder.openedAt != null && workOrder.sentAt != null,
            "Maintenance dispatch/open timestamps are required.");
        require(workOrder.requestId != null && !workOrder.requestId.isBlank()
                && workOrder.requestFingerprint != null && !workOrder.requestFingerprint.isBlank(),
            "Maintenance opening must retain idempotency metadata.");
        if (workOrder.plan != null) {
            require(Boolean.TRUE.equals(workOrder.plan.active), "Referenced maintenance plan must be active in demo data.");
            require(workOrder.plan.organization.id.equals(workOrder.organization.id),
                "Maintenance plan and order organization must match.");
            require(workOrder.plan.unit == null || workOrder.unit != null && workOrder.plan.unit.id.equals(workOrder.unit.id),
                "Maintenance plan and order unit scope must match.");
            require(workOrder.plan.periodicityDays != null && workOrder.plan.periodicityDays > 0,
                "Maintenance plan periodicity must be positive.");
        }
    }

    private void verifyIssueMovement(WorkOrder workOrder) {
        var movement = workOrder.issueMovement;
        require(movement != null, "Maintenance order must have an issue movement.");
        require(StockMovementNature.MAINTENANCE_ISSUE.name().equals(movement.nature),
            "Maintenance issue must use MAINTENANCE_ISSUE nature.");
        require(StockMovementReferenceType.MAINTENANCE.name().equals(movement.referenceType),
            "Maintenance issue must preserve MAINTENANCE reference type.");
        require(workOrder.id.equals(movement.referenceId),
            "Maintenance issue movement must reference its work order.");
        require(movement.asset != null && movement.asset.id.equals(workOrder.asset.id),
            "Maintenance issue movement must reference the same asset.");
        require(movement.quantity != null && movement.quantity.compareTo(BigDecimal.ONE.negate()) == 0,
            "Maintenance issue must remove exactly one serialized asset from availability.");
    }

    private void verifyCompleted(WorkOrder workOrder, Set<Long> returnMovements) {
        require(workOrder.completedAt != null && workOrder.returnedAt != null,
            "Completed maintenance requires completion and return timestamps.");
        require(!workOrder.completedAt.isBefore(workOrder.openedAt),
            "Maintenance completion cannot precede opening.");
        require(!workOrder.returnedAt.isBefore(workOrder.completedAt),
            "Maintenance return cannot precede completion.");
        require(workOrder.completionRequestId != null && !workOrder.completionRequestId.isBlank()
                && workOrder.completionFingerprint != null && !workOrder.completionFingerprint.isBlank(),
            "Completed maintenance must retain completion idempotency metadata.");

        List<Diagnosis> diagnoses = entityManager.createQuery(
            "select d from Diagnosis d where d.workOrder.id=:id", Diagnosis.class
        ).setParameter("id", workOrder.id).getResultList();
        require(diagnoses.size() == 1, "Completed maintenance requires exactly one diagnosis.");
        Diagnosis diagnosis = diagnoses.getFirst();
        require(notBlank(diagnosis.defect) && notBlank(diagnosis.cause) && notBlank(diagnosis.opinion),
            "Maintenance diagnosis must contain defect, cause and opinion.");

        List<ExecutedService> services = entityManager.createQuery(
            "select s from ExecutedService s where s.workOrder.id=:id", ExecutedService.class
        ).setParameter("id", workOrder.id).getResultList();
        require(!services.isEmpty(), "Completed maintenance requires at least one executed service.");

        List<MaintenancePart> parts = entityManager.createQuery(
            "select p from MaintenancePart p where p.workOrder.id=:id", MaintenancePart.class
        ).setParameter("id", workOrder.id).getResultList();

        BigDecimal expectedCost = services.stream()
            .peek(service -> require(notBlank(service.description) && service.cost != null && service.cost.signum() >= 0,
                "Maintenance service requires description and non-negative cost."))
            .map(service -> service.cost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (MaintenancePart part : parts) {
            require(notBlank(part.description) && part.quantity != null && part.quantity.signum() > 0
                    && part.unitCost != null && part.unitCost.signum() >= 0,
                "Maintenance part requires description, positive quantity and non-negative unit cost.");
            expectedCost = expectedCost.add(part.quantity.multiply(part.unitCost));
        }
        require(workOrder.totalCost != null && workOrder.totalCost.compareTo(expectedCost) == 0,
            "Maintenance total cost must equal services plus parts.");

        List<FunctionalTest> tests = entityManager.createQuery(
            "select t from FunctionalTest t where t.workOrder.id=:id", FunctionalTest.class
        ).setParameter("id", workOrder.id).getResultList();
        require(tests.size() == 1, "Completed maintenance requires exactly one functional test.");
        FunctionalTest test = tests.getFirst();
        require(TEST_RESULTS.contains(test.result) && notBlank(test.notes) && test.testedAt != null,
            "Functional test must use APPROVED/REJECTED with timestamp and notes.");
        require(!test.testedAt.isBefore(workOrder.openedAt),
            "Functional test cannot precede maintenance opening.");

        var returned = workOrder.returnMovement;
        require(returned != null, "Completed maintenance requires a return movement.");
        require(returnMovements.add(returned.id),
            "A maintenance return movement cannot belong to more than one work order.");
        require(StockMovementNature.MAINTENANCE_RETURN.name().equals(returned.nature),
            "Maintenance return must use MAINTENANCE_RETURN nature.");
        require(StockMovementReferenceType.MAINTENANCE.name().equals(returned.referenceType),
            "Maintenance return must preserve MAINTENANCE reference type.");
        require(workOrder.id.equals(returned.referenceId) && returned.asset.id.equals(workOrder.asset.id),
            "Maintenance return movement must reference the same work order and asset.");
        require(returned.quantity != null && returned.quantity.compareTo(BigDecimal.ONE) == 0,
            "Maintenance return must restore exactly one serialized asset.");
        require(workOrder.issueMovement.quantity.add(returned.quantity).compareTo(BigDecimal.ZERO) == 0,
            "Maintenance issue and return movements must net to zero units.");

        if (workOrder.plan != null) {
            require(workOrder.nextMaintenanceAt != null,
                "Completed planned maintenance must calculate the next maintenance date.");
            require(workOrder.nextMaintenanceAt.equals(workOrder.completedAt.plusDays(workOrder.plan.periodicityDays)),
                "Next maintenance date must follow the plan periodicity when no override is used in demo data.");
        }
    }

    private void verifyPlans() {
        List<MaintenancePlan> plans = entityManager.createQuery(
            "select p from MaintenancePlan p", MaintenancePlan.class
        ).getResultList();
        for (MaintenancePlan plan : plans) {
            require(plan.organization != null && notBlank(plan.name) && notBlank(plan.type),
                "Maintenance plan requires organization, name and type.");
            require(plan.periodicityDays != null && plan.periodicityDays > 0,
                "Maintenance plan periodicity must be positive.");
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) fail(message);
    }

    private static void fail(String message) {
        throw new IllegalStateException("Maintenance lifecycle verification failed: " + message);
    }
}
