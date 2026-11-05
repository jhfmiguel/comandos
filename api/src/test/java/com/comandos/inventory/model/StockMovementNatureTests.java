package com.comandos.inventory.model;
import com.fariamiguel.app.inventory.model.StockMovementReferenceType;
import com.fariamiguel.app.inventory.model.StockMovementNature;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StockMovementNatureTests {

    @Test
    void parsesCanonicalNatureIgnoringCaseAndWhitespace() {
        assertEquals(SecurityStockMovementNature.CUSTODY_ISSUE, SecurityStockMovementNature.parse(" custody_issue "));
        assertEquals(StockMovementNature.TRANSFER_IN, StockMovementNature.parse("transfer_in"));
        assertThrows(IllegalArgumentException.class, () -> StockMovementNature.parse("custody_issue"));
        assertThrows(IllegalArgumentException.class, () -> StockMovementNature.parse("consumption_deflagration"));
        assertEquals("AMMUNITION_CONSUMPTION", SecurityStockMovementReferenceType.AMMUNITION_CONSUMPTION.name());
        assertThrows(IllegalArgumentException.class, () -> StockMovementReferenceType.valueOf("AMMUNITION_CONSUMPTION"));
    }

    @Test
    void rejectsMissingOrUnknownNature() {
        assertThrows(IllegalArgumentException.class, () -> StockMovementNature.parse(null));
        assertThrows(IllegalArgumentException.class, () -> StockMovementNature.parse(""));
        assertThrows(IllegalArgumentException.class, () -> StockMovementNature.parse("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> SecurityStockMovementNature.parse(null));
        assertThrows(IllegalArgumentException.class, () -> SecurityStockMovementNature.parse(" "));
        assertThrows(IllegalArgumentException.class, () -> SecurityStockMovementNature.parse("TRANSFER_IN"));
    }
}
