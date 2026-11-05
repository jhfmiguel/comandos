package com.comandos.inventory.model;

/** Security-specific codes persisted in the shared stock history. */
public enum SecurityStockMovementNature {
    CUSTODY_ISSUE,
    CUSTODY_RETURN,
    CONSUMPTION_DEFLAGRATION;
    public static SecurityStockMovementNature parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Stock movement nature is required.");
        }
        return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
