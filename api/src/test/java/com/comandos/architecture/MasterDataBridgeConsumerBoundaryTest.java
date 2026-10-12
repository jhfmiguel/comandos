package com.comandos.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Historical migration components are not ordinary runtime dependencies.
 * This boundary prevents new production consumers of temporary bridges.
 */
class MasterDataBridgeConsumerBoundaryTest {

    private static final Set<String> TRANSITIONAL = Set.of(
        "CanonicalMasterDataWriteService.java",
        "MasterDataMigrationConfigurationGuard.java",
        "MasterDataMigrationRunner.java",
        "MasterDataMigrationService.java",
        "MasterDataCutoverStatusService.java",
        "ProductMasterDataReferenceBackfillService.java"
    );

    private static final List<String> BRIDGES = List.of(
        "CanonicalMasterDataWriteService",
        "MasterDataMigrationService",
        "MasterDataParityService",
        "MasterDataCutoverStatusService",
        "ProductMasterDataReferenceSynchronizer",
        "ProductMasterDataReferenceBackfillService",
        "ProductMasterDataReferenceParityService"
    );

    @Test
    void onlyMigrationInfrastructureMayInjectTemporaryBridgeServices() throws Exception {
        List<String> consumers = new ArrayList<>();
        try (var paths = Files.walk(Path.of("src/main/java/com/comandos"))) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = path.getFileName().toString();
                if (TRANSITIONAL.contains(name)
                        || BRIDGES.stream().anyMatch(bridge -> name.equals(bridge + ".java"))) {
                    continue;
                }
                String source = Files.readString(path);
                for (String bridge : BRIDGES) {
                    if (source.contains(bridge)) {
                        consumers.add(path + " -> " + bridge);
                    }
                }
            }
        }
        assertTrue(consumers.isEmpty(), () -> "Remaining runtime bridge consumers: " + consumers);
    }
}
