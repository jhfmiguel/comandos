package com.comandos.architecture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductMasterDataRetirementManifestTest {

    private static final Set<String> EXPECTED_PRIMARY =
        Set.of();

    @Test
    void onlyKnownFinalBridgesRemainPrimary() throws Exception {
        JsonNode root = manifest();
        Set<String> primary = new HashSet<>();
        int retired = 0;

        for (JsonNode reference : root.path("references")) {
            String state = reference.path("state").asText();
            String entity = reference.path("entity").asText();

            if ("retired".equals(state)) retired++;
            if ("canonical-id-primary".equals(state)) primary.add(entity);

            assertTrue(
                !"legacy-jpa-association".equals(state)
                    && !"canonical-id-shadow".equals(state),
                () -> entity + " regressed to " + state
            );
        }

        assertEquals(18, retired);
        assertEquals(EXPECTED_PRIMARY, primary);
    }

    private static JsonNode manifest() throws Exception {
        ClassLoader loader = ProductMasterDataRetirementManifestTest.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream("../../../architecture/master-data-foreign-keys.json")) {
            if (input != null) return new ObjectMapper().readTree(input);
        }

        java.nio.file.Path path = java.nio.file.Path.of(
            "..", "architecture", "master-data-foreign-keys.json"
        ).normalize();
        return new ObjectMapper().readTree(path.toFile());
    }
}
