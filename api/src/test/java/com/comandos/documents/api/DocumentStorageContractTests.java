package com.comandos.documents.api;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentStorageContractTests {

    @Test
    void storageRoundTripPreservesMetadataAndContent() throws Exception {
        DocumentStorage storage = new InMemoryDocumentStorage();
        byte[] content = "document-content".getBytes();

        var stored = storage.store(
            "report.txt",
            "text/plain",
            new ByteArrayInputStream(content),
            content.length
        );

        assertEquals("report.txt", stored.fileName());
        assertEquals("text/plain", stored.contentType());
        assertEquals(content.length, stored.size());
        assertNotNull(stored.id());
        assertNotNull(stored.createdAt());

        var metadata = storage.metadata(stored.id());
        assertTrue(metadata.isPresent());
        assertEquals(stored, metadata.orElseThrow());

        try (InputStream input = storage.open(stored.id())) {
            assertArrayEquals(content, input.readAllBytes());
        }
    }

    @Test
    void deleteRemovesStoredDocument() {
        DocumentStorage storage = new InMemoryDocumentStorage();
        byte[] content = { 1, 2, 3 };

        var stored = storage.store(
            "binary.bin",
            "application/octet-stream",
            new ByteArrayInputStream(content),
            content.length
        );

        storage.delete(stored.id());

        assertTrue(storage.metadata(stored.id()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> storage.open(stored.id()));
    }

    private static final class InMemoryDocumentStorage implements DocumentStorage {
        private final Map<String, byte[]> content = new HashMap<>();
        private final Map<String, DocumentReference> metadata = new HashMap<>();

        @Override
        public DocumentReference store(
            String fileName,
            String contentType,
            InputStream input,
            long size
        ) {
            try {
                String id = UUID.randomUUID().toString();
                byte[] bytes = input.readAllBytes();
                if (bytes.length != size) {
                    throw new IllegalArgumentException("Declared size does not match content.");
                }

                var reference = new DocumentReference(
                    id,
                    fileName,
                    contentType,
                    size,
                    null,
                    Instant.now()
                );

                content.put(id, bytes);
                metadata.put(id, reference);
                return reference;
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public Optional<DocumentReference> metadata(String id) {
            return Optional.ofNullable(metadata.get(id));
        }

        @Override
        public InputStream open(String id) {
            byte[] bytes = content.get(id);
            if (bytes == null) {
                throw new IllegalArgumentException("Document not found.");
            }
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void delete(String id) {
            content.remove(id);
            metadata.remove(id);
        }
    }
}
