package com.comandos.inventory.service;
import com.fariamiguel.app.inventory.service.EquipmentStatePolicy;

import com.comandos.inventory.model.SecurityAssetStatus;

import com.fariamiguel.app.inventory.model.AssetItem;
import com.fariamiguel.app.inventory.model.AssetStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentStatePolicyCanonicalTests {

    private final ComandosEquipmentStatePolicy policy = new ComandosEquipmentStatePolicy();

    @Test
    void acceptsCanonicalStatusesForSupportedOperations() {
        AssetItem asset = asset(AssetStatus.AVAILABLE);
        assertDoesNotThrow(() -> policy.require(asset, "custody"));
        assertDoesNotThrow(() -> policy.require(asset, "transfer"));
        assertDoesNotThrow(() -> policy.require(asset, "maintenance"));
        assertDoesNotThrow(() -> policy.require(asset, "donation"));
        AssetItem custodied = asset(SecurityAssetStatus.CUSTODIED);
        assertDoesNotThrow(() -> policy.require(custodied, "inspection"));
        assertDoesNotThrow(() -> policy.require(custodied, "occurrence"));
        assertThrows(ResponseStatusException.class, () -> policy.require(custodied, "transfer"));
        var generic = new EquipmentStatePolicy();
        assertThrows(ResponseStatusException.class, () -> generic.require(asset, "custody"));
        assertThrows(ResponseStatusException.class, () -> generic.require(custodied, "inspection"));
    }

    @Test
    void rejectsDonationOutsideAvailableState() {
        assertThrows(ResponseStatusException.class, () -> policy.require(asset(AssetStatus.BLOCKED), "donation"));
        assertThrows(ResponseStatusException.class, () -> policy.require(asset(SecurityAssetStatus.CUSTODIED), "donation"));
        assertThrows(ResponseStatusException.class, () -> policy.require(asset(AssetStatus.DONATED), "donation"));
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
        assertTrue(policy.isTerminal(asset(AssetStatus.DONATED)));
        assertTrue(policy.isTerminal(asset(AssetStatus.DISPOSED)));
        assertFalse(policy.isTerminal(asset(AssetStatus.DRAFT)));
        assertFalse(policy.isTerminal(asset(AssetStatus.BLOCKED)));
    }

    private static AssetItem asset(Enum<?> status) {
        AssetItem asset = new AssetItem();
        asset.status = status.name();
        return asset;
    }
}
