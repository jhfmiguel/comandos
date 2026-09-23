package com.comandos.inventory.model;

/**
 * Canonical origin types for immutable stock movement history.
 */
public enum StockMovementReferenceType {
    CUSTODY,
    CUSTODY_RETURN,
    TRANSFER,
    MAINTENANCE,
    DONATION,
    DISPOSAL,
    AMMUNITION_CONSUMPTION,
    SALE,
    SALE_RETURN,
    INVENTORY_COUNT
}
