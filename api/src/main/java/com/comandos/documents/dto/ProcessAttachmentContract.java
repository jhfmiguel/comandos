package com.comandos.documents.dto;

import java.util.List;
import java.util.Set;

public final class ProcessAttachmentContract {
    private ProcessAttachmentContract() {}

    public record AttachmentView(
        long id, String processType, long recordId, long organizationId, Long unitId,
        String documentType, String title, String fileName, String contentType, long fileSize,
        String checksum, int versionNumber, Long supersedesId, boolean currentVersion,
        String uploadedAt, Long uploadedById, String uploadedByLogin
    ) {}

    public record PolicyView(
        String processType, String permissionResource, Set<String> allowedDocumentTypes,
        Set<String> requiredDocumentTypes, Set<String> allowedContentTypes, long maxFileSize
    ) {}

    public record ComplianceView(
        String processType, long recordId, Set<String> requiredTypes,
        Set<String> presentTypes, Set<String> missingTypes, boolean complete
    ) {}

    public record AttachmentList(List<AttachmentView> content) {}
}
