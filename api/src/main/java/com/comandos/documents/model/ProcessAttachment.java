package com.comandos.documents.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "erp_process_attachment", indexes = {
    @Index(name = "idx_process_attachment_target", columnList = "process_type,record_id"),
    @Index(name = "idx_process_attachment_scope", columnList = "organization_id,unit_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_process_attachment_storage", columnNames = {"storage_id"})
})
public class ProcessAttachment extends CoreEntity {
    @Column(name = "process_type", nullable = false, length = 40)
    public String processType;
    @Column(name = "record_id", nullable = false)
    public Long recordId;
    @Column(name = "organization_id", nullable = false)
    public Long organizationId;
    @Column(name = "unit_id")
    public Long unitId;
    @Column(name = "document_type", nullable = false, length = 50)
    public String documentType;
    @Column(name = "title", nullable = false, length = 255)
    public String title;
    @Column(name = "file_name", nullable = false, length = 255)
    public String fileName;
    @Column(name = "content_type", nullable = false, length = 120)
    public String contentType;
    @Column(name = "file_size", nullable = false)
    public long fileSize;
    @Column(name = "checksum", nullable = false, length = 64)
    public String checksum;
    @Column(name = "storage_id", nullable = false, length = 255)
    public String storageId;
    @Column(name = "version_number", nullable = false)
    public int versionNumber = 1;
    @ManyToOne
    @JoinColumn(name = "supersedes_id")
    public ProcessAttachment supersedes;
    @Column(name = "current_version", nullable = false)
    public boolean currentVersion = true;
    @Column(name = "uploaded_at", nullable = false)
    public Instant uploadedAt;
    @Column(name = "uploaded_by_id")
    public Long uploadedById;
    @Column(name = "uploaded_by_login", length = 255)
    public String uploadedByLogin;
    @Column(name = "retired_at")
    public Instant retiredAt;
}
