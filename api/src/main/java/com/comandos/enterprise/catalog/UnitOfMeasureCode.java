package com.comandos.enterprise.catalog;

import java.util.Locale;
import java.util.Set;

public record UnitOfMeasureCode(String value) {
    private static final Set<String> COMMON_CODES = Set.of(
        "UN", "EA", "PC", "KG", "G", "L", "ML", "M", "CM", "MM", "M2", "M3", "CX", "PCT"
    );

    public UnitOfMeasureCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Unit of measure is required.");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z0-9]{1,10}")) {
            throw new IllegalArgumentException("Unit of measure must use 1 to 10 uppercase letters or numbers.");
        }
    }

    public boolean isCommon() {
        return COMMON_CODES.contains(value);
    }
}
