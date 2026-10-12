package com.comandos.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Permanent architectural protection against accidental restoration of the
 * obsolete COMANDOS numeric-id to canonical-id persistence registry.
 *
 * The canonical domain is the source of identifiers after the bridge removal.
 */
class MasterDataCrosswalkRetirementArchitectureTest {

    @Test
    void crosswalkEntityAndServiceMustRemainRemoved() {
        assertFalse(Files.exists(Path.of(
            "src/main/java/com/comandos/core/model/MasterDataReference.java"
        )));
        assertFalse(Files.exists(Path.of(
            "src/main/java/com/comandos/core/service/MasterDataReferenceService.java"
        )));
    }

    @Test
    void transitionalConfigurationGuardRemainsRetired() throws Exception {
        assertFalse(Files.exists(Path.of(
            "src/main/java/com/comandos/core/service/MasterDataMigrationConfigurationGuard.java"
        )));
        String directory = Files.readString(Path.of(
            "src/main/java/com/comandos/core/service/CanonicalMasterDataDirectory.java"
        ));
        assertTrue(directory.contains("void validateCanonicalReadConfiguration()"));
    }

    @Test
    void productionJavaMustNotReferenceRetiredCrosswalkTypes() throws Exception {
        var offenders = new ArrayList<String>();
        Path root = Path.of("src/main/java");
        try (var files = Files.walk(root)) {
            List<Path> sources = files.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
            for (Path source : sources) {
                String java = Files.readString(source);
                if (java.contains("MasterDataReferenceService")
                        || java.contains("com.comandos.core.model.MasterDataReference")
                        || java.contains("new MasterDataReference(")) {
                    offenders.add(source.toString());
                }
            }
        }
        assertTrue(offenders.isEmpty(), () ->
            "Legacy crosswalk consumers still present: " + offenders
        );
    }
}
