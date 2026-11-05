package com.comandos.inventory.service;
import com.fariamiguel.app.inventory.service.InventoryCatalog;

import com.comandos.inventory.model.SecurityAssetStatus;

import static org.junit.jupiter.api.Assertions.*;

import com.fariamiguel.app.inventory.model.AssetCondition;
import com.fariamiguel.app.inventory.model.AssetStatus;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class InventoryCatalogCanonicalTests {
    private final InventoryCatalog inventoryCatalog = new InventoryCatalog(java.util.List.of(new ComandosInventoryCatalogContributor()));


    @Test
    void assetChoicesFollowCanonicalEnums() {
        var assets = inventoryCatalog.get("assets");

        var condition = assets.fields().stream()
            .filter(field -> field.name().equals("condition"))
            .findFirst()
            .orElseThrow();

        var status = assets.fields().stream()
            .filter(field -> field.name().equals("status"))
            .findFirst()
            .orElseThrow();

        assertEquals(
            Arrays.stream(AssetCondition.values()).map(Enum::name).toList(),
            condition.choices()
        );
        assertEquals(
            java.util.List.of("DRAFT", "AVAILABLE", "BLOCKED", "CUSTODIED", "IN_MAINTENANCE",
                "TRANSFER_PENDING", "MISSING", "RESTRICTED", "SOLD", "DONATED", "DISPOSED"),
            status.choices()
        );
    }
}
