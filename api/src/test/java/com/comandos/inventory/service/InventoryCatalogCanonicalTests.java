package com.comandos.inventory.service;

import static org.junit.jupiter.api.Assertions.*;

import com.comandos.inventory.model.AssetCondition;
import com.comandos.inventory.model.AssetStatus;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class InventoryCatalogCanonicalTests {

    @Test
    void assetChoicesFollowCanonicalEnums() {
        var assets = InventoryCatalog.get("assets");

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
            Arrays.stream(AssetStatus.values()).map(Enum::name).toList(),
            status.choices()
        );
    }
}
