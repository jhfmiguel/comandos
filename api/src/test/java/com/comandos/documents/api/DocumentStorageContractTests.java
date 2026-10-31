package com.comandos.documents.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fariamiguel.documents.api.DocumentReference;
import com.fariamiguel.documents.api.DocumentStorage;
import com.fariamiguel.documents.api.DocumentWrite;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentStorageContractTests {

    @Test
    void storageRoundTripUsesCanonicalFariaMiguelContract() throws Exception {
        DocumentStorage storage = new InMemoryDocumentStorage();
        byte[] content = "document-content".getBytes();

        var stored = storage.store(new DocumentWrite(
            "report.txt",
            "text/plain",
            content.length,
            null,
            new ByteArrayInputStream(content)
        ));

        assertEquals("report.txt", stored.fileName());
        assertEquals("text/plain", stored.contentType());
        assertEquals(content.length, stored.size());
        assertNotNull(stored.id());
        assertEquals(stored.id().toString(), stored.storageKey());

        try (InputStream input = storage.open(stored.id())) {
            assertArrayEquals(content, input.readAllBytes());
        }
    }

    @Test
    void deleteRemovesStoredDocument() {
        DocumentStorage storage = new InMemoryDocumentStorage();
        byte[] content = { 1, 2, 3 };

        var stored = storage.store(new DocumentWrite(
            "binary.bin",
            "application/octet-stream",
            content.length,
            null,
            new ByteArrayInputStream(content)
        ));

        storage.delete(stored.id());

        assertThrows(IllegalArgumentException.class, () -> storage.open(stored.id()));
    }

    private static final class InMemoryDocumentStorage implements DocumentStorage {
        private final Map<UUID, byte[]> content = new HashMap<>();

        @Override
        public DocumentReference store(DocumentWrite document) {
            try {
                UUID id = UUID.randomUUID();
                byte[] bytes = document.content().readAllBytes();
                if (bytes.length != document.size()) {
                    throw new IllegalArgumentException("Declared size does not match content.");
                }

                content.put(id, bytes);
                return new DocumentReference(
                    id,
                    document.fileName(),
                    document.contentType(),
                    document.size(),
                    document.checksum(),
                    id.toString()
                );
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public InputStream open(UUID id) {
            byte[] bytes = content.get(id);
            if (bytes == null) {
                throw new IllegalArgumentException("Document not found.");
            }
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void delete(UUID id) {
            content.remove(id);
        }
    }
}
