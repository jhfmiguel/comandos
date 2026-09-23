package com.comandos.inventory.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AssetStatusTests {

    @Test
    void terminalStatesAreSubsetOfWorkflowManagedStates() {
        assertTrue(AssetStatus.workflowManagedCodes().containsAll(AssetStatus.terminalCodes()));
        assertTrue(AssetStatus.terminalCodes().contains(AssetStatus.SOLD.name()));
        assertTrue(AssetStatus.terminalCodes().contains(AssetStatus.DONATED.name()));
        assertTrue(AssetStatus.terminalCodes().contains(AssetStatus.DISPOSED.name()));
    }

    @Test
    void transientOperationalStatesCannotBeManagedByRegistration() {
        assertTrue(AssetStatus.workflowManagedCodes().contains(AssetStatus.CUSTODIED.name()));
        assertTrue(AssetStatus.workflowManagedCodes().contains(AssetStatus.IN_MAINTENANCE.name()));
        assertTrue(AssetStatus.workflowManagedCodes().contains(AssetStatus.TRANSFER_PENDING.name()));
        assertTrue(AssetStatus.workflowManagedCodes().contains(AssetStatus.MISSING.name()));
        assertTrue(AssetStatus.workflowManagedCodes().contains(AssetStatus.RESTRICTED.name()));

        assertFalse(AssetStatus.workflowManagedCodes().contains(AssetStatus.DRAFT.name()));
        assertFalse(AssetStatus.workflowManagedCodes().contains(AssetStatus.AVAILABLE.name()));
        assertFalse(AssetStatus.workflowManagedCodes().contains(AssetStatus.BLOCKED.name()));
    }
}
