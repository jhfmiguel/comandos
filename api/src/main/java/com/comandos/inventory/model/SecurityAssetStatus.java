package com.comandos.inventory.model;
import com.fariamiguel.app.inventory.model.AssetStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Custody state and the composed status vocabulary of the security product. */
public enum SecurityAssetStatus {
    CUSTODIED;

    public static List<String> codes() {
        var codes = new ArrayList<>(Arrays.stream(AssetStatus.values()).map(Enum::name).toList());
        // Preserve the established order in the security catalog.
        codes.remove(AssetStatus.BLOCKED.name());
        codes.add(2, AssetStatus.BLOCKED.name());
        codes.add(3, CUSTODIED.name());
        return List.copyOf(codes);
    }

    public static Set<String> workflowManagedCodes() {
        var codes = new HashSet<>(AssetStatus.workflowManagedCodes());
        codes.add(CUSTODIED.name());
        return Set.copyOf(codes);
    }

    public static String parseCode(String value) {
        if (value != null && CUSTODIED.name().equalsIgnoreCase(value.trim())) return CUSTODIED.name();
        return AssetStatus.parse(value).name();
    }
}