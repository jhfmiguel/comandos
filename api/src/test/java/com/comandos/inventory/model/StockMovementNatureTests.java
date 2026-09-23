package com.comandos.inventory.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StockMovementNatureTests {

    @Test
    void parsesCanonicalNatureIgnoringCaseAndWhitespace() {
        assertEquals(StockMovementNature.CUSTODY_ISSUE, StockMovementNature.parse(" custody_issue "));
        assertEquals(StockMovementNature.TRANSFER_IN, StockMovementNature.parse("transfer_in"));
    }

    @Test
    void rejectsMissingOrUnknownNature() {
        assertThrows(IllegalArgumentException.class, () -> StockMovementNature.parse(null));
        assertThrows(IllegalArgumentException.class, () -> StockMovementNature.parse(""));
        assertThrows(IllegalArgumentException.class, () -> StockMovementNature.parse("UNKNOWN"));
    }
}
