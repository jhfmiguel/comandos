package com.comandos.documents.storage;

import com.comandos.documents.api.DocumentReference;
import com.comandos.documents.api.DocumentStorage;
import com.comandos.documents.model.ProcessAttachment;
import com.comandos.documents.model.StoredDocumentBlob;
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
    public DocumentReference store(String fileName, String contentType, InputStream content, long size) {
        if (fileName == null || fileName.isBlank() || contentType == null || contentType.isBlank() || content == null || size <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name, content type, content and a positive size are required.");
        byte[] bytes = readAll(content);
        if (bytes.length != size)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Declared document size does not match the received content.");
        var value = new StoredDocumentBlob();
        value.storageId = UUID.randomUUID().toString();
        value.fileName = fileName.trim();
        value.contentType = contentType.trim().toLowerCase(Locale.ROOT);
        value.fileSize = bytes.length;
        value.checksum = sha256(bytes);
        value.createdAt = Instant.now();
        value.content = bytes.clone();
        entityManager.persist(value);
        entityManager.flush();
        return reference(value);
    }

    @Override
    public Optional<DocumentReference> metadata(String id) {
        return find(id).map(this::reference);
    }

    @Override
    public InputStream open(String id) {
        var value = find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored document not found."));
        byte[] bytes = value.content == null ? new byte[0] : value.content.clone();
        if (bytes.length != value.fileSize || !sha256(bytes).equals(value.checksum))
            throw new IllegalStateException("Stored document integrity check failed for " + id);
        return new ByteArrayInputStream(bytes);
    }

    @Override
    @Transactional
    public void delete(String id) {
        var value = find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored document not found."));
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

    private DocumentReference reference(StoredDocumentBlob value) {
        return new DocumentReference(value.storageId, value.fileName, value.contentType,
            value.fileSize, value.checksum, value.createdAt);
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
