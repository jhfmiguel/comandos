package com.comandos.inventory.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AssetConditionTests {

    @Test
    void parsesCanonicalConditionIgnoringCaseAndWhitespace() {
        assertEquals(AssetCondition.GOOD, AssetCondition.parse(" good "));
        assertEquals(AssetCondition.NEEDS_INSPECTION, AssetCondition.parse("needs_inspection"));
    }

    @Test
    void rejectsMissingOrUnknownCondition() {
        assertThrows(IllegalArgumentException.class, () -> AssetCondition.parse(null));
        assertThrows(IllegalArgumentException.class, () -> AssetCondition.parse(" "));
        assertThrows(IllegalArgumentException.class, () -> AssetCondition.parse("BROKEN"));
    }
}
