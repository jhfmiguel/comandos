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

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/comandos").resolve(relativePath));
    }
}
