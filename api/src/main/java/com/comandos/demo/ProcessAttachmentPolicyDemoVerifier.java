package com.comandos.demo;

import com.comandos.documents.model.ProcessAttachment;
import com.comandos.documents.service.ProcessDocumentPolicy;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1996)
public class ProcessAttachmentPolicyDemoVerifier implements ApplicationRunner {
    private static final Set<String> EXPECTED_PROCESSES = Set.of(
        "ACQUISITION", "RECEIVING", "INCORPORATION", "CUSTODY", "TRANSFER", "DONATION", "SALE",
        "CONSUMABLE_USAGE", "MAINTENANCE", "INSPECTION", "OCCURRENCE", "DISPOSAL", "EQUIPMENT_SET_OPERATION"
    );

    private final EntityManager em;
    private final ProcessDocumentPolicy policy;

    public ProcessAttachmentPolicyDemoVerifier(EntityManager em, ProcessDocumentPolicy policy) {
        this.em = em;
        this.policy = policy;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        if (!EXPECTED_PROCESSES.equals(policy.processTypes())) fail("process matrix does not cover every required lifecycle");
        for (String type : EXPECTED_PROCESSES) {
            var rule = policy.rule(type);
            if (blank(rule.permissionResource())) fail(type + " has no permission resource");
            if (rule.allowedTypes().isEmpty()) fail(type + " has no allowed document types");
            if (!rule.allowedTypes().containsAll(rule.requiredTypes())) fail(type + " requires a document type that is not allowed");
        }
        if (ProcessDocumentPolicy.MAX_FILE_SIZE != 20L * 1024L * 1024L) fail("uniform maximum file size must be 20 MB");
        if (!ProcessDocumentPolicy.ALLOWED_CONTENT_TYPES.contains("application/pdf")) fail("PDF must be supported");
        if (!ProcessDocumentPolicy.ALLOWED_CONTENT_TYPES.contains("image/jpeg") || !ProcessDocumentPolicy.ALLOWED_CONTENT_TYPES.contains("image/png"))
            fail("inspection/photo evidence MIME types are missing");

        var attachments = em.createQuery("select a from ProcessAttachment a order by a.id", ProcessAttachment.class).getResultList();
        Set<String> storageIds = new HashSet<>();
        Map<Long, Integer> childCount = new HashMap<>();
        for (var a : attachments) {
            if (!EXPECTED_PROCESSES.contains(a.processType)) fail("attachment has unsupported process type: " + a.processType);
            if (a.recordId == null || a.recordId <= 0 || a.organizationId == null || a.organizationId <= 0) fail("attachment target is invalid");
            if (!policy.rule(a.processType).allowedTypes().contains(a.documentType)) fail("attachment has invalid document type");
            if (blank(a.title) || blank(a.fileName) || blank(a.contentType) || blank(a.storageId) || blank(a.checksum)) fail("attachment metadata is incomplete");
            if (!ProcessDocumentPolicy.ALLOWED_CONTENT_TYPES.contains(a.contentType)) fail("attachment MIME type violates uniform policy");
            if (a.fileSize <= 0 || a.fileSize > ProcessDocumentPolicy.MAX_FILE_SIZE) fail("attachment file size violates uniform policy");
            if (!a.checksum.matches("[0-9a-f]{64}")) fail("attachment checksum is not SHA-256");
            if (a.versionNumber < 1) fail("attachment version must be positive");
            if (a.uploadedAt == null) fail("attachment upload timestamp is missing");
            if (!storageIds.add(a.storageId)) fail("one stored binary is linked by more than one process attachment");
            if (a.supersedes != null) {
                if (!Objects.equals(a.processType, a.supersedes.processType) || !Objects.equals(a.recordId, a.supersedes.recordId)
                        || !Objects.equals(a.documentType, a.supersedes.documentType)) fail("replacement changed attachment identity");
                if (a.versionNumber != a.supersedes.versionNumber + 1) fail("replacement version is not sequential");
                if (a.supersedes.currentVersion || a.supersedes.retiredAt == null) fail("superseded attachment was not retired");
                childCount.merge(a.supersedes.id, 1, Integer::sum);
            }
        }
        if (childCount.values().stream().anyMatch(count -> count > 1)) fail("an attachment version has multiple replacement successors");

    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void fail(String detail) { throw new IllegalStateException("Process attachment policy regression failed: " + detail); }
}
