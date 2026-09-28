package com.comandos.inventory.service;

import com.comandos.inventory.model.AssetItem;
import java.text.Normalizer;
import java.util.*;

/** NFKC, uppercase ROOT and no whitespace; punctuation remains significant. */
final class AssetIdentity {
    private AssetIdentity() {}

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replaceAll("[\\p{javaWhitespace}\\p{Z}]", "").toUpperCase(Locale.ROOT);
    }

    static List<String> conflicts(List<AssetItem> assets, String code, String serial, String internalCode) {
        List<String> errors = new ArrayList<>();
        String normalizedCode = normalize(code);
        String normalizedSerial = normalize(serial);
        String normalizedInternalCode = normalize(internalCode);
        for (var asset : assets) {
            String related = " Individual asset (#" + asset.id + ").";
            if (!normalizedCode.isEmpty() && normalizedCode.equals(normalize(asset.assetCode)))
                errors.add("Asset code is already registered." + related);
            if (!normalizedSerial.isEmpty() && normalizedSerial.equals(normalize(asset.serialNumber)))
                errors.add("Serial number is already registered." + related);
            if (!normalizedInternalCode.isEmpty() && normalizedInternalCode.equals(normalize(asset.internalCode)))
                errors.add("Internal code is already registered." + related);
        }
        return errors;
    }
}
