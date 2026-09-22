package com.comandos.inventory.service;

import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.AssetStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentStatePolicyCanonicalTests {

    private final EquipmentStatePolicy policy = new EquipmentStatePolicy();

    @Test
    void acceptsCanonicalStatusesForSupportedOperations() {
        AssetItem asset = asset(AssetStatus.AVAILABLE);
        assertDoesNotThrow(() -> policy.require(asset, "custody"));
        assertDoesNotThrow(() -> policy.require(asset, "transfer"));
        assertDoesNotThrow(() -> policy.require(asset, "maintenance"));
    }

    @Test
    void rejectsLegacyOrUnknownStatusCodes() {
        AssetItem asset = new AssetItem();
        asset.status = "IN_CUSTODY";
        assertThrows(ResponseStatusException.class, () -> policy.require(asset, "inspection"));
    }

    @Test
    void terminalStatesAreExplicit() {
        assertTrue(policy.isTerminal(asset(AssetStatus.SOLD)));
        assertTrue(policy.isTerminal(asset(AssetStatus.DISPOSED)));
        assertFalse(policy.isTerminal(asset(AssetStatus.BLOCKED)));
    }

    private static AssetItem asset(AssetStatus status) {
        AssetItem asset = new AssetItem();
        asset.status = status.name();
        return asset;
    }
}
