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
        String code = source("core/service/ProductMasterDataReferenceBackfillService.java");
        assertFalse(code.contains("private final MasterDataReferenceService references;"));
        assertFalse(code.contains("references.resolveCanonicalId("));
        assertFalse(code.contains("MasterDataReferenceService"));
        assertTrue(code.contains("private static final String ORGANIZATION"));
        assertTrue(code.contains("private static final String PERSON"));
        assertTrue(code.contains("private static final String UNIT"));
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
    void historicalBackfillSynchronizerHasNoRuntimeShadowWriteMode() throws IOException {
        String code = source("core/service/ProductMasterDataReferenceBackfillService.java");
        assertFalse(code.contains("product-reference-shadow.enabled"));
        assertFalse(code.contains("synchronizeOnWrite("));
        assertFalse(code.contains("public boolean synchronize(CoreEntity entity)"));
        assertTrue(code.contains("public boolean synchronizeForBackfill(CoreEntity entity)"));
        assertFalse(Files.exists(Path.of("src/main/java/com/comandos/core/service/ProductMasterDataReferenceSynchronizer.java")));
    }

    @Test
    void masterDataBatchBackfillNeverDependsOnRuntimeShadowWriteFlag() throws IOException {
        String migration = source("core/service/MasterDataMigrationService.java");
        assertFalse(migration.contains("mirror.mirror("));
        assertTrue(migration.contains("mirror.mirrorForBackfill("));
        String mirror = source("core/service/CanonicalMasterDataWriteService.java");
        assertTrue(mirror.contains("public boolean mirrorForBackfill(CoreEntity entity)"));
    }

    @Test
    void inventoryWritesDirectlyToCanonicalIdsAndBackfillIsIsolated() throws IOException {
        String inventory = source("inventory/service/InventoryService.java");
        String synchronizer = source("core/service/ProductMasterDataReferenceBackfillService.java");
        assertFalse(inventory.contains("masterDataReferences.synchronizeOnWrite("));
        assertFalse(inventory.contains("ProductMasterDataReferenceSynchronizer"));
        assertTrue(inventory.contains("synchronizeCanonicalProductWrite(entity)"));
        assertTrue(inventory.contains("canonicalScope.organization("));
        assertTrue(inventory.contains("canonicalScope.unit("));
        assertTrue(synchronizer.contains("public boolean synchronizeForBackfill(CoreEntity entity)"));
    }

    @Test
    void productReferenceBackfillRejectsEveryFormOfIncompleteResult() throws IOException {
        String code = source("core/service/ProductMasterDataReferenceBackfillService.java");
        assertTrue(code.contains("!readiness.ready()"));
        assertTrue(code.contains("readiness.incomplete() != 0"));
        assertTrue(code.contains("!readiness.incompleteByEntity().isEmpty()"));
        assertTrue(code.contains("if (synchronizeForBackfill(entity))"));
    }

    @Test
    void retirementGuardPublishesActionableEvidenceInsteadOfSkippingGates() throws IOException {
        String code = source("core/service/MasterDataCutoverStatusService.java");
        assertTrue(code.contains("public List<String> retirementBlockers("));
        assertTrue(code.contains("LEGACY_READINESS_FAILED"));
        assertTrue(code.contains("PRODUCT_REFERENCE_BACKFILL_INCOMPLETE"));
        assertTrue(code.contains("LEGACY_PRODUCT_ASSOCIATIONS_ACTIVE"));
        assertTrue(code.contains("MASTER_DATA_CANONICAL_READ_DISABLED"));
    }

    @Test
    void cutoverEvidenceSnapshotIsAvailableWithoutRelaxingRetirementGuard() throws IOException {
        String code = source("core/service/MasterDataCutoverStatusService.java");
        assertTrue(code.contains("public record RetirementAssessment("));
        assertTrue(code.contains("public RetirementAssessment assessment()"));
        assertTrue(code.contains("retirementBlockers(snapshot)"));
        assertTrue(code.contains("snapshot.productReferences().incomplete()"));
        assertTrue(code.contains("requireReadyToRetireLegacyPersistence()"));
    }

    @Test
    void cutoverEvidenceSnapshotIsReadOnlyAndPreservesParityDetails() throws IOException {
        String code = source("core/service/MasterDataCutoverStatusService.java");
        assertTrue(code.contains("public record CutoverEvidence("));
        assertTrue(code.contains("public CutoverEvidence evidence()"));
        assertTrue(code.contains("public CutoverEvidence evidence(CutoverStatus current)"));
        assertTrue(code.contains("java.util.Collections.unmodifiableMap(gates)"));
        assertTrue(code.contains("java.util.List.copyOf(legacyParity.mismatches())"));
        assertTrue(code.contains("java.util.Map.copyOf(productReadiness.incompleteByEntity())"));
        assertFalse(code.contains("migration.backfill("));
        assertFalse(code.contains("productReferenceBackfill.backfill("));
    }

    @Test
    void canonicalReadDoesNotForceFullLegacyRetirementAtStartup() throws IOException {
        String runner = source("core/service/MasterDataMigrationRunner.java");
        assertTrue(runner.contains("if (retirementValidationOnStartup)"));
        assertFalse(runner.contains("retirementValidationOnStartup || canonicalReadEnabled"));
        assertTrue(runner.contains("cutoverStatus.requireReadyToRetireLegacyPersistence()"));
        assertFalse(runner.contains("cutoverStatus.canonicalReadEnabled()"));
        assertTrue(runner.contains("if (productReferenceBackfillOnStartup || parityOnStartup"));
        assertTrue(runner.contains("Product canonical reference parity mismatch:"));
    }

    @Test
    void productParityUsesCanonicalDirectoryInsteadOfCrosswalkRegistry() throws IOException {
        String parity = source("core/service/ProductMasterDataReferenceParityService.java");
        assertFalse(parity.contains("MasterDataReferenceService"));
        assertFalse(parity.contains("resolveCanonicalId("));
        assertTrue(parity.contains("private final ProductCanonicalScopeResolver canonicalScope"));
        assertTrue(parity.contains("canonicalScope.organization(legacyId)"));
        assertTrue(parity.contains("canonicalScope.person(legacyId)"));
        assertTrue(parity.contains("canonicalScope.unit(legacyId)"));
        assertTrue(parity.contains("Product canonical reference differs from the canonical directory."));
    }

    @Test
    void productParityCannotPassOnLegacyOnlyCanonicalProjections() throws IOException {
        String directory = source("core/service/CanonicalMasterDataDirectory.java");
        String resolver = source("core/service/ProductCanonicalScopeResolver.java");
        String parity = source("core/service/ProductMasterDataReferenceParityService.java");
        assertTrue(directory.contains("containsPersistedCanonicalPerson("));
        assertTrue(directory.contains("containsPersistedCanonicalOrganization("));
        assertTrue(directory.contains("containsPersistedCanonicalUnit("));
        assertTrue(resolver.contains("persistedCanonicalExists("));
        assertTrue(parity.contains("canonicalScope.persistedCanonicalExists("));
        assertTrue(parity.contains("persisted canonical record is missing"));
    }

    @Test
    void masterDataParityChecksCanonicalEntitiesWithoutTemporaryCrosswalk() throws IOException {
        String parity = source("core/service/MasterDataParityService.java");
        assertFalse(parity.contains("MasterDataReferenceService"));
        assertFalse(parity.contains("verifyCrosswalk("));
        assertFalse(parity.contains("resolveCanonicalId("));
        assertTrue(parity.contains("verifyPeople(mismatches)"));
        assertTrue(parity.contains("verifyOrganizations(mismatches)"));
        assertTrue(parity.contains("verifyQualifications(mismatches)"));
    }

    @Test
    void temporaryCrosswalkRegistryIsNoLongerPartOfRuntimeArchitecture() throws IOException {
        Path service = Path.of("src/main/java/com/comandos/core/service/MasterDataReferenceService.java");
        Path entity = Path.of("src/main/java/com/comandos/core/model/MasterDataReference.java");
        assertFalse(Files.exists(service));
        assertFalse(Files.exists(entity));
        assertFalse(source("core/service/CanonicalMasterDataWriteService.java")
            .contains("MasterDataReferenceService"));
        assertFalse(source("core/service/MasterDataParityService.java")
            .contains("MasterDataReferenceService"));
        assertFalse(source("core/service/ProductMasterDataReferenceParityService.java")
            .contains("MasterDataReferenceService"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/comandos").resolve(relativePath));
    }
}
