package com.comandos.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductCanonicalSynchronizationArchitectureTest {

    @Test
    void productSynchronizerDoesNotDependOnLegacyReferenceRegistry() throws IOException {
        String code = source("core/service/ProductMasterDataReferenceSynchronizer.java");
        assertFalse(code.contains("private final MasterDataReferenceService references;"));
        assertFalse(code.contains("references.resolveCanonicalId("));
        assertTrue(code.contains("private final ProductCanonicalScopeResolver canonicalScope;"));
    }

    @Test
    void canonicalProductScopeDoesNotQueryCrosswalk() throws IOException {
        String code = source("core/service/ProductCanonicalScopeResolver.java");
        assertFalse(code.contains("MasterDataReferenceService"));
        assertFalse(code.contains("resolveCanonicalId("));
        assertTrue(code.contains("CanonicalMasterDataDirectory"));
    }

    @Test
    void productSynchronizationPreservesShadowWriteControl() throws IOException {
        String code = source("core/service/ProductMasterDataReferenceSynchronizer.java");
        assertTrue(code.contains("if (!enabled) return false;"));
        assertTrue(code.contains("public boolean synchronizeForBackfill(CoreEntity entity)"));
    }

    @Test
    void masterDataBatchBackfillNeverDependsOnRuntimeShadowWriteFlag() throws IOException {
        String migration = source("core/service/MasterDataMigrationService.java");
        assertFalse(migration.contains("mirror.mirror("));
        assertTrue(migration.contains("mirror.mirrorForBackfill("));
        String mirror = source("core/service/CanonicalMasterDataMirrorService.java");
        assertTrue(mirror.contains("public boolean mirrorForBackfill(CoreEntity entity)"));
    }

    @Test
    void inventoryWriteAndBackfillHaveDistinctEntryPoints() throws IOException {
        String inventory = source("inventory/service/InventoryService.java");
        String synchronizer = source("core/service/ProductMasterDataReferenceSynchronizer.java");
        assertTrue(inventory.contains("masterDataReferences.synchronizeOnWrite("));
        assertFalse(inventory.contains("masterDataReferences.synchronizeForBackfill("));
        assertTrue(synchronizer.contains("public boolean synchronizeOnWrite(CoreEntity entity, boolean canonicalPrimaryReadEnabled)"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/comandos").resolve(relativePath));
    }
}
