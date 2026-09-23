package com.comandos.inventory.model;

import java.util.Locale;

public enum StockMovementNature {
    OPENING,
    CUSTODY_ISSUE,
    CUSTODY_RETURN,
    TRANSFER_OUT,
    TRANSFER_IN,
    TRANSFER_REJECT_OUT,
    TRANSFER_REJECT_RETURN,
    DONATION,
    DISPOSAL,
    MAINTENANCE_ISSUE,
    MAINTENANCE_RETURN,
    INVENTORY_ADJUSTMENT,
    CONSUMPTION_DEFLAGRATION;

    public static StockMovementNature parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Stock movement nature is required.");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
