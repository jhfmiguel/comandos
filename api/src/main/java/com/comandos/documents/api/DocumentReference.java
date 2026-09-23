package com.comandos.documents.api;

import java.time.Instant;

public record DocumentReference(
    String id,
    String fileName,
    String contentType,
    long size,
    String checksum,
    Instant createdAt
) {
}
