package com.comandos.lifecycle.service;

import com.comandos.audit.service.AuditService;
import com.comandos.lifecycle.dto.LifecycleContract.*;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class InspectionChecklistService {
    private static final Set<String> ITEM_RESULTS = Set.of("APPROVED", "REPROVED", "NOT_APPLICABLE");
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;
    private final MaintenanceService maintenance;

    public InspectionChecklistService(EntityManager em, AccessPolicy access, AuditService audit, MaintenanceService maintenance) {
        this.em = em;
        this.access = access;
        this.audit = audit;
        this.maintenance = maintenance;
    }

    @Transactional
    public List<InspectionItemView> save(long inspectionId, InspectionChecklistRequest request) {
        PeriodicInspection inspection = lockedInspection(inspectionId);
        require(inspection, "UPDATE");
        if (inspection.approvedAt != null) conflict("Approved inspection checklist is immutable.");
        if (request == null || request.items() == null || request.items().isEmpty()) bad("Inspection checklist requires at least one item.");

        List<PeriodicInspectionItem> existing = items(inspectionId);
        Map<String, PeriodicInspectionItem> byCode = new HashMap<>();
        for (PeriodicInspectionItem item : existing) byCode.put(item.code, item);
        Set<String> incomingCodes = new HashSet<>();
        Set<Integer> incomingOrders = new HashSet<>();
        boolean failed = false;

        for (InspectionItemRequest line : request.items()) {
            if (line == null || blank(line.code()) || blank(line.label()) || line.order() == null || line.order() <= 0 || blank(line.result())) {
                bad("Every checklist item requires positive order, code, label and result.");
            }
            String code = line.code().trim().toUpperCase(Locale.ROOT);
            if (!incomingCodes.add(code)) bad("Checklist item code must be unique inside an inspection.");
            if (!incomingOrders.add(line.order())) bad("Checklist item order must be unique inside an inspection.");
            String result = line.result().trim().toUpperCase(Locale.ROOT);
            if (!ITEM_RESULTS.contains(result)) bad("Checklist item result must be APPROVED, REPROVED or NOT_APPLICABLE.");
            boolean required = line.required() == null || line.required();
            if (required && "NOT_APPLICABLE".equals(result)) bad("Required checklist items cannot be marked NOT_APPLICABLE.");
            failed |= "REPROVED".equals(result);

            PeriodicInspectionItem item = byCode.remove(code);
            if (item == null) {
                item = new PeriodicInspectionItem();
                item.inspection = inspection;
                em.persist(item);
            }
            item.itemOrder = line.order();
            item.code = code;
            item.label = limit(line.label(), 255, "Checklist label");
            item.required = required;
            item.result = result;
            item.observation = nullable(line.observation(), 1000, "Checklist observation");
        }

        for (PeriodicInspectionItem removed : byCode.values()) {
            if (photoCount(removed.id) > 0) conflict("Checklist items with photos cannot be removed.");
            em.remove(removed);
        }

        if (failed && "APPROVED".equals(inspection.result)) inspection.result = "REPROVED";
        if (failed) {
            inspection.asset.status = "BLOCKED";
            if (Boolean.TRUE.equals(request.generateMaintenance()) && inspection.generatedWorkOrderId == null) {
                var workOrder = maintenance.open(new OpenRequest(
                    UUID.randomUUID().toString(), inspection.organization.id,
                    inspection.unit == null ? null : inspection.unit.id, null, inspection.asset.id,
                    "Periodic inspection checklist contains reproved item(s).", null, "CORRECTIVE", null, null));
                inspection.generatedWorkOrderId = workOrder.id();
            }
        }
        em.flush();
        List<InspectionItemView> result = views(inspectionId);
        Map<String,Object> change = new LinkedHashMap<>();
        change.put("items", result);
        change.put("generatedWorkOrderId", inspection.generatedWorkOrderId);
        audit.record("periodic-inspections", inspection.id, "CHECKLIST", null, change);
        return result;
    }

    public List<InspectionItemView> get(long inspectionId) {
        PeriodicInspection inspection = inspection(inspectionId);
        require(inspection, "READ");
        return views(inspectionId);
    }

    @Transactional
    public AttachmentView addPhoto(long inspectionId, long itemId, AttachmentRequest request) {
        PeriodicInspection inspection = lockedInspection(inspectionId);
        require(inspection, "UPDATE");
        if (inspection.approvedAt != null) conflict("Approved inspection evidence is immutable.");
        PeriodicInspectionItem item = em.find(PeriodicInspectionItem.class, itemId);
        if (item == null || item.inspection == null || !item.inspection.id.equals(inspection.id)) bad("Checklist item does not belong to inspection.");
        if (request == null || blank(request.fileName()) || blank(request.contentType()) || blank(request.base64())) bad("Photo file name, content type and content are required.");
        String contentType = request.contentType().trim().toLowerCase(Locale.ROOT);
        if (!contentType.startsWith("image/")) bad("Inspection item evidence must be an image.");
        byte[] data;
        try { data = Base64.getDecoder().decode(request.base64()); }
        catch (IllegalArgumentException ex) { bad("Invalid Base64 image."); return null; }
        if (data.length == 0) bad("Inspection image cannot be empty.");
        if (data.length > 10 * 1024 * 1024) bad("Inspection image exceeds 10 MB.");

        OperationAttachment attachment = new OperationAttachment();
        attachment.resource = "periodic-inspections";
        attachment.recordId = inspection.id;
        attachment.inspectionItem = item;
        attachment.fileName = limit(request.fileName(), 255, "File name");
        attachment.contentType = contentType;
        attachment.content = data;
        attachment.description = nullable(request.description(), 1000, "Description");
        attachment.uploadedAt = LocalDateTime.now();
        var actor = audit.actor();
        attachment.uploadedById = actor.id();
        attachment.uploadedByLogin = actor.login();
        em.persist(attachment);
        em.flush();
        AttachmentView view = view(attachment);
        audit.record("periodic-inspections", inspection.id, "ADD_CHECKLIST_PHOTO", null, view);
        return view;
    }

    private List<PeriodicInspectionItem> items(long inspectionId) {
        return em.createQuery("select i from PeriodicInspectionItem i where i.inspection.id=:id order by i.itemOrder,i.id", PeriodicInspectionItem.class)
            .setParameter("id", inspectionId).getResultList();
    }

    private List<InspectionItemView> views(long inspectionId) {
        List<InspectionItemView> result = new ArrayList<>();
        for (PeriodicInspectionItem item : items(inspectionId)) {
            result.add(new InspectionItemView(item.id, item.itemOrder, item.code, item.label, item.required, item.result, item.observation, photoCount(item.id)));
        }
        return result;
    }

    private long photoCount(long itemId) {
        return em.createQuery("select count(a) from OperationAttachment a where a.inspectionItem.id=:id", Long.class)
            .setParameter("id", itemId).getSingleResult();
    }

    private AttachmentView view(OperationAttachment a) {
        return new AttachmentView(a.id, a.resource, a.recordId, a.fileName, a.contentType, a.description, a.uploadedAt.toString(), a.uploadedByLogin, a.inspectionItem == null ? null : a.inspectionItem.id);
    }

    private PeriodicInspection inspection(long id) {
        PeriodicInspection value = em.find(PeriodicInspection.class, id);
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found.");
        return value;
    }

    private PeriodicInspection lockedInspection(long id) {
        PeriodicInspection value = em.find(PeriodicInspection.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found.");
        return value;
    }

    private void require(PeriodicInspection inspection, String action) {
        access.requireScope("inventory/assets", action, inspection.organization.id, inspection.unit == null ? null : inspection.unit.id);
    }

    private static String limit(String value, int max, String label) {
        if (blank(value)) bad(label + " is required.");
        String trimmed = value.trim();
        if (trimmed.length() > max) bad(label + " is too long.");
        return trimmed;
    }

    private static String nullable(String value, int max, String label) {
        if (blank(value)) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max) bad(label + " is too long.");
        return trimmed;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
