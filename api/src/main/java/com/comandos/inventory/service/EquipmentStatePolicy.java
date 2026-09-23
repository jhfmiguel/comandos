package com.comandos.inventory.service;

import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.AssetStatus;
import com.comandos.inventory.model.EquipmentOperation;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EquipmentStatePolicy {

    private static final Map<EquipmentOperation, Set<AssetStatus>> ALLOWED = Map.of(
        EquipmentOperation.CUSTODY, Set.of(AssetStatus.AVAILABLE),
        EquipmentOperation.TRANSFER, Set.of(AssetStatus.AVAILABLE),
        EquipmentOperation.MAINTENANCE, Set.of(AssetStatus.AVAILABLE, AssetStatus.BLOCKED),
        EquipmentOperation.SALE, Set.of(AssetStatus.AVAILABLE, AssetStatus.BLOCKED),
        EquipmentOperation.DONATION, Set.of(AssetStatus.AVAILABLE),
        EquipmentOperation.DISPOSAL, Set.of(AssetStatus.AVAILABLE, AssetStatus.BLOCKED, AssetStatus.MISSING, AssetStatus.RESTRICTED),
        EquipmentOperation.INSPECTION, Set.of(AssetStatus.AVAILABLE, AssetStatus.CUSTODIED, AssetStatus.IN_MAINTENANCE, AssetStatus.BLOCKED),
        EquipmentOperation.OCCURRENCE, Set.of(AssetStatus.AVAILABLE, AssetStatus.CUSTODIED, AssetStatus.IN_MAINTENANCE, AssetStatus.BLOCKED, AssetStatus.MISSING, AssetStatus.RESTRICTED)
    );

    public void require(AssetItem asset, String operation) {
        if (asset == null) bad("Asset is required.");
        EquipmentOperation normalizedOperation;
        try {
            normalizedOperation = EquipmentOperation.parse(operation);
        } catch (IllegalArgumentException exception) {
            bad("Unsupported equipment operation.");
            return;
        }
        Set<AssetStatus> states = ALLOWED.get(normalizedOperation);

        AssetStatus current;
        try {
            current = AssetStatus.parse(asset.status);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Asset has an unknown operational status: " + asset.status + ".");
        }

        if (!states.contains(current)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Operation " + normalizedOperation + " is incompatible with asset status " + current.name() + ".");
        }
    }

    public String code(AssetStatus status) {
        return status.name();
    }

    public boolean isTerminal(AssetItem asset) {
        if (asset == null || asset.status == null) return false;
        return AssetStatus.terminalCodes().contains(asset.status);
    }

    private static void bad(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
