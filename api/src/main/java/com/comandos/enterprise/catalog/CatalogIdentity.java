package com.comandos.enterprise.catalog;

import java.text.Normalizer;
import java.util.Locale;

public final class CatalogIdentity {
    private CatalogIdentity() {}

    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .trim()
            .replaceAll("\\s+", " ")
            .toUpperCase(Locale.ROOT);
    }

    public static boolean same(String left, String right) {
        return normalize(left).equals(normalize(right));
    }
}
