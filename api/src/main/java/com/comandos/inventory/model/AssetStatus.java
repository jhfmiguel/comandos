package com.comandos.inventory.model;

import java.util.Locale;
import java.util.Set;

public enum AssetStatus {
    DRAFT,
    AVAILABLE,
    CUSTODIED,
    IN_MAINTENANCE,
    TRANSFER_PENDING,
    BLOCKED,
    MISSING,
    RESTRICTED,
    SOLD,
    DONATED,
    DISPOSED;

    public static AssetStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Asset status is required.");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public static Set<String> terminalCodes() {
        return Set.of(SOLD.name(), DONATED.name(), DISPOSED.name());
    }
}
