package com.comandos.inventory.model;
import com.fariamiguel.app.inventory.model.AssetStatus;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.Set;

class AssetStatusTests {

    @Test
    void terminalStatesAreSubsetOfWorkflowManagedStates() {
        assertTrue(SecurityAssetStatus.workflowManagedCodes().containsAll(AssetStatus.terminalCodes()));
        assertTrue(AssetStatus.terminalCodes().contains(AssetStatus.SOLD.name()));
        assertTrue(AssetStatus.terminalCodes().contains(AssetStatus.DONATED.name()));
        assertTrue(AssetStatus.terminalCodes().contains(AssetStatus.DISPOSED.name()));
    }

    @Test
    void transientOperationalStatesCannotBeManagedByRegistration() {
        assertEquals("CUSTODIED", SecurityAssetStatus.parseCode(" custodied "));
        assertEquals("AVAILABLE", SecurityAssetStatus.parseCode(" available "));
        assertThrows(IllegalArgumentException.class, () -> AssetStatus.parse("CUSTODIED"));
        assertThrows(IllegalArgumentException.class, () -> SecurityAssetStatus.parseCode("UNKNOWN"));
        assertEquals(SecurityAssetStatus.codes().size(), Set.copyOf(SecurityAssetStatus.codes()).size());
        assertTrue(SecurityAssetStatus.workflowManagedCodes().contains(SecurityAssetStatus.CUSTODIED.name()));
        assertTrue(SecurityAssetStatus.workflowManagedCodes().contains(AssetStatus.IN_MAINTENANCE.name()));
        assertTrue(SecurityAssetStatus.workflowManagedCodes().contains(AssetStatus.TRANSFER_PENDING.name()));
        assertTrue(SecurityAssetStatus.workflowManagedCodes().contains(AssetStatus.MISSING.name()));
        assertTrue(SecurityAssetStatus.workflowManagedCodes().contains(AssetStatus.RESTRICTED.name()));

        assertFalse(SecurityAssetStatus.workflowManagedCodes().contains(AssetStatus.DRAFT.name()));
        assertFalse(SecurityAssetStatus.workflowManagedCodes().contains(AssetStatus.AVAILABLE.name()));
        assertFalse(SecurityAssetStatus.workflowManagedCodes().contains(AssetStatus.BLOCKED.name()));
    }
}
