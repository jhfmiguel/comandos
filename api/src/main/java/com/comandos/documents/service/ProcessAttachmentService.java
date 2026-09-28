package com.comandos.documents.service;

import com.comandos.audit.service.AuditService;
import com.comandos.documents.api.DocumentStorage;
import com.comandos.documents.dto.ProcessAttachmentContract.*;
import com.comandos.documents.model.ProcessAttachment;
import com.comandos.security.service.AccessPolicy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProcessAttachmentService {
    public record Download(String fileName, String contentType, byte[] content) {}

    private final EntityManager em;
    private final DocumentStorage storage;
    private final ProcessDocumentPolicy policy;
    private final AccessPolicy access;
    private final AuditService audit;

    public ProcessAttachmentService(EntityManager em, DocumentStorage storage, ProcessDocumentPolicy policy,
            AccessPolicy access, AuditService audit) {
        this.em = em;
        this.storage = storage;
        this.policy = policy;
        this.access = access;
        this.audit = audit;
    }

    public PolicyView policy(String processType) {
        String type = ProcessDocumentPolicy.normalize(processType);
        var rule = policy.rule(type);
        access.requireAny(rule.permissionResource(), "READ");
        return new PolicyView(type, rule.permissionResource(), rule.allowedTypes(), rule.requiredTypes(),
            ProcessDocumentPolicy.ALLOWED_CONTENT_TYPES, ProcessDocumentPolicy.MAX_FILE_SIZE);
    }

    @Transactional
    public AttachmentView upload(String processType, long recordId, long organizationId, Long unitId,
            String documentType, String title, String fileName, String contentType, byte[] content) {
        validateTarget(recordId, organizationId);
        String type = ProcessDocumentPolicy.normalize(processType);
        var rule = policy.rule(type);
        access.requireScope(rule.permissionResource(), "CREATE", organizationId, unitId);
        String docType = policy.validateDocumentType(type, documentType);
        byte[] bytes = content == null ? new byte[0] : content;
        policy.validateFile(contentType, bytes.length);
        String cleanTitle = clean(title, 255, "Attachment title");
        String cleanFileName = cleanFileName(fileName);
        var stored = storage.store(cleanFileName, contentType, bytes);
        var actor = audit.actor();
        var attachment = new ProcessAttachment();
        attachment.processType = type;
        attachment.recordId = recordId;
        attachment.organizationId = organizationId;
        attachment.unitId = unitId;
        attachment.documentType = docType;
        attachment.title = cleanTitle;
        attachment.fileName = stored.fileName();
        attachment.contentType = stored.contentType();
        attachment.fileSize = stored.fileSize();
        attachment.checksum = stored.checksum();
        attachment.storageId = stored.storageId();
        attachment.versionNumber = 1;
        attachment.currentVersion = true;
        attachment.uploadedAt = stored.createdAt();
        attachment.uploadedById = actor.id();
        attachment.uploadedByLogin = actor.login();
        em.persist(attachment);
        em.flush();
        var result = view(attachment);
        audit.record("process-attachments", attachment.id, "UPLOAD", null, result);
        return result;
    }

    @Transactional
    public AttachmentView replace(long attachmentId, String title, String fileName, String contentType, byte[] content) {
        var current = locked(attachmentId);
        if (!current.currentVersion) conflict("Only the current attachment version can be replaced.");
        var rule = policy.rule(current.processType);
        access.requireScope(rule.permissionResource(), "CREATE", current.organizationId, current.unitId);
        byte[] bytes = content == null ? new byte[0] : content;
        policy.validateFile(contentType, bytes.length);
        String cleanTitle = title == null || title.isBlank() ? current.title : clean(title, 255, "Attachment title");
        String cleanFileName = cleanFileName(fileName);
        var stored = storage.store(cleanFileName, contentType, bytes);
        var actor = audit.actor();
        var now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        current.currentVersion = false;
        current.retiredAt = now;
        var next = new ProcessAttachment();
        next.processType = current.processType;
        next.recordId = current.recordId;
        next.organizationId = current.organizationId;
        next.unitId = current.unitId;
        next.documentType = current.documentType;
        next.title = cleanTitle;
        next.fileName = stored.fileName();
        next.contentType = stored.contentType();
        next.fileSize = stored.fileSize();
        next.checksum = stored.checksum();
        next.storageId = stored.storageId();
        next.versionNumber = current.versionNumber + 1;
        next.supersedes = current;
        next.currentVersion = true;
        next.uploadedAt = stored.createdAt();
        next.uploadedById = actor.id();
        next.uploadedByLogin = actor.login();
        em.persist(next);
        em.flush();
        var result = view(next);
        audit.record("process-attachments", next.id, "REPLACE", view(current), result);
        return result;
    }

    public AttachmentList list(String processType, long recordId, long organizationId, Long unitId, boolean includeHistory) {
        validateTarget(recordId, organizationId);
        String type = ProcessDocumentPolicy.normalize(processType);
        var rule = policy.rule(type);
        access.requireScope(rule.permissionResource(), "READ", organizationId, unitId);
        String jpql = "select a from ProcessAttachment a where a.processType=:type and a.recordId=:record and a.organizationId=:organization"
            + (unitId == null ? "" : " and a.unitId=:unit") + (includeHistory ? "" : " and a.currentVersion=true")
            + " order by a.documentType,a.id";
        var q = em.createQuery(jpql, ProcessAttachment.class).setParameter("type", type)
            .setParameter("record", recordId).setParameter("organization", organizationId);
        if (unitId != null) q.setParameter("unit", unitId);
        return new AttachmentList(q.getResultList().stream().map(this::view).toList());
    }

    public Download download(long attachmentId) {
        var attachment = em.find(ProcessAttachment.class, attachmentId);
        if (attachment == null) notFound();
        var rule = policy.rule(attachment.processType);
        access.requireScope(rule.permissionResource(), "READ", attachment.organizationId, attachment.unitId);
        byte[] content = storage.read(attachment.storageId);
        if (content.length != attachment.fileSize) throw new IllegalStateException("Stored attachment size mismatch.");
        return new Download(attachment.fileName, attachment.contentType, content);
    }

    public ComplianceView compliance(String processType, long recordId, long organizationId, Long unitId) {
        validateTarget(recordId, organizationId);
        String type = ProcessDocumentPolicy.normalize(processType);
        var rule = policy.rule(type);
        access.requireScope(rule.permissionResource(), "READ", organizationId, unitId);
        String jpql = "select distinct a.documentType from ProcessAttachment a where a.processType=:type and a.recordId=:record"
            + " and a.organizationId=:organization and a.currentVersion=true" + (unitId == null ? "" : " and a.unitId=:unit");
        var q = em.createQuery(jpql, String.class).setParameter("type", type).setParameter("record", recordId)
            .setParameter("organization", organizationId);
        if (unitId != null) q.setParameter("unit", unitId);
        Set<String> present = new TreeSet<>(q.getResultList());
        Set<String> missing = new TreeSet<>(rule.requiredTypes());
        missing.removeAll(present);
        return new ComplianceView(type, recordId, new TreeSet<>(rule.requiredTypes()), present, missing, missing.isEmpty());
    }

    private ProcessAttachment locked(long id) {
        if (id <= 0) notFound();
        var value = em.find(ProcessAttachment.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) notFound();
        return value;
    }

    private AttachmentView view(ProcessAttachment value) {
        return new AttachmentView(value.id, value.processType, value.recordId, value.organizationId, value.unitId,
            value.documentType, value.title, value.fileName, value.contentType, value.fileSize, value.checksum,
            value.versionNumber, value.supersedes == null ? null : value.supersedes.id, value.currentVersion,
            value.uploadedAt.toString(), value.uploadedById, value.uploadedByLogin);
    }

    private static void validateTarget(long recordId, long organizationId) {
        if (recordId <= 0 || organizationId <= 0) bad("A valid process record and organization are required.");
    }
    private static String clean(String value, int max, String label) {
        if (value == null || value.isBlank()) bad(label + " is required.");
        String result = value.trim();
        if (result.length() > max) bad(label + " is too long.");
        return result;
    }
    private static String cleanFileName(String value) {
        String result = clean(value, 255, "File name");
        if (result.contains("/") || result.contains("\\") || result.contains("\u0000")) bad("File name contains invalid characters.");
        return result;
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private static void notFound() { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found."); }
}
