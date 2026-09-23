package com.comandos.inventory.model;

import java.util.Locale;

public enum AssetCondition {
    NEW,
    GOOD,
    NEEDS_INSPECTION,
    DAMAGED;

    public static AssetCondition parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Asset condition is required.");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
