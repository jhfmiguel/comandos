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
        for (var asset : assets) {
            String related = " Individual asset (#" + asset.id + ").";
            if (!normalize(code).isEmpty() && normalize(code).equals(normalize(asset.assetCode)))
                errors.add("Asset code is already registered." + related);
            if (!normalize(serial).isEmpty() && normalize(serial).equals(normalize(asset.serialNumber)))
                errors.add("Serial number is already registered." + related);
        }
        return errors;
    }
}
