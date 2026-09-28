package com.comandos.lifecycle.service;

import com.comandos.audit.service.AuditService;
import com.comandos.lifecycle.dto.LifecycleContract.InspectionItemView;
import com.comandos.lifecycle.dto.LifecycleContract.InspectionView;
import com.comandos.lifecycle.model.OperationAttachment;
import com.comandos.lifecycle.model.PeriodicInspection;
import com.comandos.lifecycle.model.PeriodicInspectionItem;
import com.comandos.maintenance.dto.MaintenanceContract.OpenRequest;
import com.comandos.maintenance.service.MaintenanceService;
import com.comandos.security.service.AccessPolicy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class InspectionApprovalService {
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;
    private final MaintenanceService maintenance;

    public InspectionApprovalService(EntityManager em, AccessPolicy access, AuditService audit, MaintenanceService maintenance) {
        this.em = em;
        this.access = access;
        this.audit = audit;
        this.maintenance = maintenance;
    }

    @Transactional
    public InspectionView approve(long id) {
        PeriodicInspection inspection = em.find(PeriodicInspection.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (inspection == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found.");
        access.requireScope("inventory/assets", "UPDATE", inspection.organization.id, inspection.unit == null ? null : inspection.unit.id);
        if (inspection.approvedAt != null) return view(inspection);

        List<PeriodicInspectionItem> items = items(id);
        if (items.isEmpty()) conflict("Inspection requires a structured checklist before approval.");
        boolean failed = false;
        for (PeriodicInspectionItem item : items) {
            if (item.required == null || blank(item.code) || blank(item.label) || blank(item.result)) conflict("Inspection checklist is incomplete.");
            if (Boolean.TRUE.equals(item.required) && "NOT_APPLICABLE".equals(item.result)) conflict("Required checklist items cannot be NOT_APPLICABLE.");
            if ("REPROVED".equals(item.result)) {
                failed = true;
                if (photoCount(item.id) == 0) conflict("Every reproved checklist item requires photographic evidence before approval.");
            }
        }

        if (failed) inspection.result = "REPROVED";
        boolean correctiveRequired = failed || Set.of("FAILED", "REPROVED", "MAINTENANCE_REQUIRED").contains(inspection.result);
        if (correctiveRequired) {
            inspection.asset.status = "BLOCKED";
            if (inspection.generatedWorkOrderId == null) {
                var workOrder = maintenance.open(new OpenRequest(
                    UUID.randomUUID().toString(), inspection.organization.id,
                    inspection.unit == null ? null : inspection.unit.id, null, inspection.asset.id,
                    "Corrective maintenance automatically generated from periodic inspection " + inspection.id + ".",
                    null, "CORRECTIVE", null, null));
                inspection.generatedWorkOrderId = workOrder.id();
            }
        }

        var actor = audit.actor();
        inspection.approvedById = actor.id();
        inspection.approvedByLogin = actor.login();
        inspection.approvedAt = LocalDateTime.now();
        em.flush();
        InspectionView result = view(inspection);
        audit.record("periodic-inspections", inspection.id, "APPROVE", null, result);
        return result;
    }

    private InspectionView view(PeriodicInspection inspection) {
        List<InspectionItemView> itemViews = new ArrayList<>();
        long totalPhotos = 0;
        for (PeriodicInspectionItem item : items(inspection.id)) {
            long photos = photoCount(item.id);
            totalPhotos += photos;
            itemViews.add(new InspectionItemView(item.id, item.itemOrder, item.code, item.label, item.required, item.result, item.observation, photos));
        }
        return new InspectionView(
            inspection.id, inspection.asset.id, inspection.asset.assetCode, inspection.checklist, inspection.result,
            inspection.damages, inspection.inspectedAt.toString(), inspection.responsibleLogin, inspection.approvedByLogin,
            inspection.generatedWorkOrderId, itemViews, totalPhotos);
    }

    private List<PeriodicInspectionItem> items(long inspectionId) {
        return em.createQuery("select i from PeriodicInspectionItem i where i.inspection.id=:id order by i.itemOrder,i.id", PeriodicInspectionItem.class)
            .setParameter("id", inspectionId).getResultList();
    }

    private long photoCount(long itemId) {
        return em.createQuery("select count(a) from OperationAttachment a where a.inspectionItem.id=:id", Long.class)
            .setParameter("id", itemId).getSingleResult();
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
