package com.comandos.inventory.model;
import com.fariamiguel.app.inventory.model.EquipmentOperation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EquipmentOperationTests {

    @Test
    void parsesOperationIgnoringCaseAndWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> EquipmentOperation.parse(" custody "));
        assertEquals(EquipmentOperation.DONATION, EquipmentOperation.parse("donation"));
    }

    @Test
    void rejectsMissingOrUnknownOperation() {
        assertThrows(IllegalArgumentException.class, () -> EquipmentOperation.parse(null));
        assertThrows(IllegalArgumentException.class, () -> EquipmentOperation.parse(""));
        assertThrows(IllegalArgumentException.class, () -> EquipmentOperation.parse("LOAN"));
    }
}
