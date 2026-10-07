package com.comandos.architecture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateRetirementArchitectureTest {

    private static final Set<String> FORBIDDEN_FILE_MARKERS =
        Set.of(".backup-", ".corrompido-");

    @Test
    void noObsoleteBackupOrCorruptedCopiesRemain() throws IOException {
        Path repositoryRoot = Path.of("..").normalize();

        try (var paths = Files.walk(repositoryRoot)) {
            var offenders = paths
                .filter(Files::isRegularFile)
                .map(repositoryRoot::relativize)
                .map(Path::toString)
                .filter(path -> FORBIDDEN_FILE_MARKERS.stream().anyMatch(path::contains))
                .toList();

            assertTrue(offenders.isEmpty(), () -> "Obsolete duplicated files remain: " + offenders);
        }
    }

    @Test
    void allTrackedProductMasterDataAssociationsStayRetired() throws Exception {
        JsonNode manifest = readJson(Path.of("..", "architecture", "master-data-foreign-keys.json"));
        int retired = 0;

        for (JsonNode reference : manifest.path("references")) {
            String state = reference.path("state").asText();
            assertFalse("legacy-jpa-association".equals(state));
            assertFalse("canonical-id-shadow".equals(state));
            assertFalse("canonical-id-primary".equals(state));
            assertEquals("retired", state, reference.path("entity").asText());
            retired++;
        }

        assertEquals(18, retired);
    }

    @Test
    void everyRemovedInventoryEntryPointsToAbsentFiles() throws Exception {
        JsonNode inventory = readJson(Path.of("..", "architecture", "step-21-duplicate-retirement.json"));
        Set<String> removed = new HashSet<>();

        for (JsonNode group : inventory.path("groups")) {
            if (!"removed".equals(group.path("classification").asText())) continue;
            for (JsonNode file : group.path("files")) removed.add(file.asText());
        }

        assertFalse(removed.isEmpty());

        Path repositoryRoot = Path.of("..").normalize();
        for (String relative : removed) {
            assertFalse(
                Files.exists(repositoryRoot.resolve(relative)),
                () -> "Removed duplicate returned to the repository: " + relative
            );
        }
    }

    private static JsonNode readJson(Path path) throws IOException {
        return new ObjectMapper().readTree(path.normalize().toFile());
    }
}
