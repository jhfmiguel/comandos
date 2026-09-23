package com.comandos.inventory.model;

import java.util.Locale;

public enum EquipmentOperation {
    CUSTODY,
    TRANSFER,
    MAINTENANCE,
    SALE,
    DONATION,
    DISPOSAL,
    INSPECTION,
    OCCURRENCE;

    public static EquipmentOperation parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Equipment operation is required.");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
