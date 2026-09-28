package com.comandos.demo;

import com.comandos.lifecycle.model.OperationAttachment;
import com.comandos.lifecycle.model.PeriodicInspection;
import com.comandos.lifecycle.model.PeriodicInspectionItem;
import com.comandos.maintenance.model.WorkOrder;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
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
@Order(1810)
public class InspectionLifecycleDemoVerifier implements ApplicationRunner {
    private static final Set<String> INSPECTION_RESULTS = Set.of("APPROVED", "FAILED", "REPROVED", "MAINTENANCE_REQUIRED");
    private static final Set<String> ITEM_RESULTS = Set.of("APPROVED", "REPROVED", "NOT_APPLICABLE");
    private final EntityManager em;

    public InspectionLifecycleDemoVerifier(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<PeriodicInspection> inspections = em.createQuery("select i from PeriodicInspection i order by i.id", PeriodicInspection.class).getResultList();
        if (inspections.isEmpty()) fail("Demo periodic inspection is missing.");
        long totalPhotos = 0;

        for (PeriodicInspection inspection : inspections) {
            if (inspection.organization == null || inspection.asset == null) fail("Inspection provenance is incomplete.");
            if (blank(inspection.checklist)) fail("Inspection legacy checklist snapshot is required for compatibility/history.");
            if (!INSPECTION_RESULTS.contains(inspection.result)) fail("Inspection result is invalid.");
            if (inspection.inspectedAt == null || inspection.responsibleId == null || blank(inspection.responsibleLogin)) fail("Inspection responsibility trail is incomplete.");
            if (inspection.approvedAt != null) {
                if (blank(inspection.approvedByLogin)) fail("Approved inspection must retain approver identity.");
                if (inspection.approvedAt.isBefore(inspection.inspectedAt)) fail("Inspection approval cannot precede inspection.");
            }

            List<PeriodicInspectionItem> items = em.createQuery(
                "select i from PeriodicInspectionItem i where i.inspection.id=:id order by i.itemOrder,i.id", PeriodicInspectionItem.class)
                .setParameter("id", inspection.id).getResultList();
            if (items.isEmpty()) fail("Periodic inspection must contain structured checklist items.");
            Set<String> codes = new HashSet<>();
            Set<Integer> orders = new HashSet<>();
            boolean reprovedItem = false;

            for (PeriodicInspectionItem item : items) {
                if (item.inspection == null || !item.inspection.id.equals(inspection.id)) fail("Checklist item inspection linkage is invalid.");
                if (item.itemOrder == null || item.itemOrder <= 0 || !orders.add(item.itemOrder)) fail("Checklist item order must be positive and unique.");
                if (blank(item.code) || !codes.add(item.code)) fail("Checklist item code must be present and unique.");
                if (blank(item.label)) fail("Checklist item label is required.");
                if (item.required == null || !ITEM_RESULTS.contains(item.result)) fail("Checklist item state is invalid.");
                if (Boolean.TRUE.equals(item.required) && "NOT_APPLICABLE".equals(item.result)) fail("Required checklist item cannot be NOT_APPLICABLE.");
                reprovedItem |= "REPROVED".equals(item.result);

                List<OperationAttachment> photos = em.createQuery(
                    "select a from OperationAttachment a where a.inspectionItem.id=:id order by a.id", OperationAttachment.class)
                    .setParameter("id", item.id).getResultList();
                for (OperationAttachment photo : photos) {
                    totalPhotos++;
                    if (!"periodic-inspections".equals(photo.resource) || !inspection.id.equals(photo.recordId)) fail("Inspection photo provenance is invalid.");
                    if (photo.inspectionItem == null || !photo.inspectionItem.id.equals(item.id)) fail("Inspection photo item linkage is invalid.");
                    if (blank(photo.fileName) || blank(photo.contentType) || !photo.contentType.toLowerCase().startsWith("image/")) fail("Checklist evidence must be an image with metadata.");
                    if (photo.content == null || photo.content.length == 0) fail("Checklist photo content is empty.");
                }
            }

            if (reprovedItem && "APPROVED".equals(inspection.result)) fail("Approved inspection cannot contain a reproved checklist item.");
            if (Set.of("FAILED", "REPROVED", "MAINTENANCE_REQUIRED").contains(inspection.result)
                && !Set.of("BLOCKED", "IN_MAINTENANCE").contains(inspection.asset.status)) {
                fail("Failed inspection must make the asset unavailable.");
            }
            if (inspection.generatedWorkOrderId != null) {
                WorkOrder workOrder = em.find(WorkOrder.class, inspection.generatedWorkOrderId);
                if (workOrder == null || workOrder.asset == null || !workOrder.asset.id.equals(inspection.asset.id)) fail("Generated corrective work order does not belong to inspected asset.");
                if (!"CORRECTIVE".equals(workOrder.maintenanceType)) fail("Inspection-generated work order must be corrective.");
            }
        }
        if (totalPhotos == 0) fail("Demo inspection must include checklist photo evidence.");
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void fail(String message) { throw new IllegalStateException("Inspection lifecycle verification failed: " + message); }
}
