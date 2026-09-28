package com.comandos.documents.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_stored_document_blob", uniqueConstraints = {
    @UniqueConstraint(name = "uk_stored_document_storage_id", columnNames = {"storage_id"})
})
public class StoredDocumentBlob extends CoreEntity {
    @Column(name = "storage_id", nullable = false, updatable = false, length = 36)
    public String storageId;
    @Column(name = "file_name", nullable = false, updatable = false, length = 255)
    public String fileName;
    @Column(name = "content_type", nullable = false, updatable = false, length = 120)
    public String contentType;
    @Column(name = "file_size", nullable = false, updatable = false)
    public long fileSize;
    @Column(name = "checksum", nullable = false, updatable = false, length = 64)
    public String checksum;
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content_bytes", nullable = false, updatable = false)
    public byte[] content;
}
