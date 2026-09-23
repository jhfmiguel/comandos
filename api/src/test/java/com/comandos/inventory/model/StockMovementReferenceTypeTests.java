package com.comandos.inventory.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class StockMovementReferenceTypeTests {

    @Test
    void keepsCanonicalReferenceTypesStable() {
        assertEquals(Set.of(
            "CUSTODY",
            "CUSTODY_RETURN",
            "TRANSFER",
            "MAINTENANCE",
            "DONATION",
            "DISPOSAL",
            "AMMUNITION_CONSUMPTION",
            "SALE",
            "SALE_RETURN",
            "INVENTORY_COUNT"
        ), Set.of(java.util.Arrays.stream(StockMovementReferenceType.values())
            .map(Enum::name)
            .toArray(String[]::new)));
    }
}
