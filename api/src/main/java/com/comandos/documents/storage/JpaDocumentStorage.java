package com.comandos.documents.storage;

import com.comandos.documents.api.DocumentReference;
import com.comandos.documents.api.DocumentStorage;
import com.comandos.documents.model.StoredDocumentBlob;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@Transactional(readOnly = true)
public class JpaDocumentStorage implements DocumentStorage {
    private final EntityManager entityManager;

    public JpaDocumentStorage(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public DocumentReference store(String fileName, String contentType, byte[] content) {
        if (fileName == null || fileName.isBlank() || contentType == null || contentType.isBlank() || content == null || content.length == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name, content type and content are required.");
        var value = new StoredDocumentBlob();
        value.storageId = UUID.randomUUID().toString();
        value.fileName = fileName.trim();
        value.contentType = contentType.trim().toLowerCase(java.util.Locale.ROOT);
        value.fileSize = content.length;
        value.checksum = sha256(content);
        value.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        value.content = content.clone();
        entityManager.persist(value);
        entityManager.flush();
        return new DocumentReference(value.storageId, value.fileName, value.contentType, value.fileSize, value.checksum, value.createdAt);
    }

    @Override
    public byte[] read(String storageId) {
        if (storageId == null || storageId.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Storage ID is required.");
        try {
            var value = entityManager.createQuery("select d from StoredDocumentBlob d where d.storageId=:id", StoredDocumentBlob.class)
                .setParameter("id", storageId).getSingleResult();
            byte[] content = value.content == null ? new byte[0] : value.content.clone();
            if (!sha256(content).equals(value.checksum))
                throw new IllegalStateException("Stored document checksum mismatch for " + storageId);
            return content;
        } catch (NoResultException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored document not found.");
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
