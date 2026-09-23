package com.comandos.documents.api;

import java.io.InputStream;
import java.util.Optional;

public interface DocumentStorage {

    DocumentReference store(
        String fileName,
        String contentType,
        InputStream content,
        long size
    );

    Optional<DocumentReference> metadata(String id);

    InputStream open(String id);

    void delete(String id);
}
