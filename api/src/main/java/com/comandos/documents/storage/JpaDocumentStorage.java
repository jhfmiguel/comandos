package com.comandos.documents.storage;

import com.comandos.documents.model.ProcessAttachment;
import com.comandos.documents.model.StoredDocumentBlob;
import com.fariamiguel.documents.api.DocumentReference;
import com.fariamiguel.documents.api.DocumentStorage;
import com.fariamiguel.documents.api.DocumentWrite;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
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
    public DocumentReference store(DocumentWrite document) {
        if (document == null) throw new IllegalArgumentException("document is required");
        String fileName = document.fileName();
        String contentType = document.contentType();
        if (contentType == null || contentType.isBlank() || document.size() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content type and a positive size are required.");

        byte[] bytes = readAll(document.content());
        if (bytes.length != document.size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Declared document size does not match the received content.");

        String checksum = sha256(bytes);
        if (document.checksum() != null && !document.checksum().isBlank()
                && !document.checksum().equalsIgnoreCase(checksum))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Declared document checksum does not match the received content.");

        UUID id = UUID.randomUUID();
        var value = new StoredDocumentBlob();
        value.storageId = id.toString();
        value.fileName = fileName.trim();
        value.contentType = contentType.trim().toLowerCase(Locale.ROOT);
        value.fileSize = bytes.length;
        value.checksum = checksum;
        value.createdAt = Instant.now();
        value.content = bytes.clone();
        entityManager.persist(value);
        entityManager.flush();

        return new DocumentReference(id, value.fileName, value.contentType, value.fileSize, value.checksum, value.storageId);
    }

    @Override
    public InputStream open(UUID documentId) {
        if (documentId == null) throw new IllegalArgumentException("documentId is required");
        var value = find(documentId.toString())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored document not found."));
        byte[] bytes = value.content == null ? new byte[0] : value.content.clone();
        if (bytes.length != value.fileSize || !sha256(bytes).equals(value.checksum))
            throw new IllegalStateException("Stored document integrity check failed for " + documentId);
        return new ByteArrayInputStream(bytes);
    }

    @Override
    @Transactional
    public void delete(UUID documentId) {
        if (documentId == null) throw new IllegalArgumentException("documentId is required");
        String id = documentId.toString();
        var value = find(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored document not found."));
        long references = entityManager.createQuery(
                "select count(a) from ProcessAttachment a where a.storageId=:id", Long.class)
            .setParameter("id", id).getSingleResult();
        if (references > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stored document is preserved by process attachment history.");
        entityManager.remove(value);
        entityManager.flush();
    }

    private Optional<StoredDocumentBlob> find(String id) {
        if (id == null || id.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Storage ID is required.");
        return entityManager.createQuery("select d from StoredDocumentBlob d where d.storageId=:id", StoredDocumentBlob.class)
            .setParameter("id", id).getResultStream().findFirst();
    }

    private static byte[] readAll(InputStream content) {
        try {
            return content.readAllBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read document content.", exception);
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
