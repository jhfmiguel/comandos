package com.comandos.enterprise.catalog;

public record CatalogTrackingPolicy(
    boolean serialized,
    boolean lotControlled,
    boolean consumable
) {
    public CatalogTrackingPolicy {
        if (serialized && consumable) {
            throw new IllegalArgumentException("Consumable catalog items cannot require individual serialization.");
        }
        if (serialized && lotControlled) {
            throw new IllegalArgumentException("A catalog category cannot be serial- and lot-controlled at the same time.");
        }
        if (consumable && !lotControlled) {
            throw new IllegalArgumentException("Consumable catalog items must be lot-controlled.");
        }
    }

    public CatalogTrackingMode mode() {
        if (serialized) return CatalogTrackingMode.SERIAL;
        if (lotControlled) return CatalogTrackingMode.LOT;
        return CatalogTrackingMode.NONE;
    }
}
