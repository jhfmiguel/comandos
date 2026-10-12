package com.comandos.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductBridgeConsumerRetirementArchitectureTest {

    private static final List<String> DIRECT_CANONICAL_CONSUMERS = List.of(
        "purchase/service/PurchaseService.java",
        "purchase/service/EquipmentReceivingService.java",
        "transfer/service/TransferService.java",
        "sales/service/InventorySalesService.java",
        "reconciliation/service/InventoryCountService.java",
        "maintenance/service/MaintenanceService.java",
        "lifecycle/service/LifecycleService.java",
        "consumption/service/AmmunitionConsumptionService.java",
        "consumption/service/ConsumableUsageService.java",
        "custody/service/CustodyService.java",
        "disposal/service/DisposalService.java",
        "donation/service/DonationService.java",
        "reservation/service/ReservationService.java",
        "workflow/service/WorkflowService.java"
    );

    @Test
    void canonicalProductConsumersHaveNoRedundantSynchronizationBridgeDependency() throws IOException {
        for (String relativePath : DIRECT_CANONICAL_CONSUMERS) {
            String source = read(relativePath);
            assertFalse(source.contains("ProductMasterDataReferenceSynchronizer"),
                relativePath + " must not inject the temporary product synchronizer");
            assertFalse(source.contains("masterDataReferences"),
                relativePath + " must not retain an unused bridge field");
            assertTrue(source.contains("ProductCanonicalScopeResolver"),
                relativePath + " must continue using canonical scope resolution");
        }
    }

    @Test
    void inventoryRuntimeWritesNoLongerDependOnTemporarySynchronizationBridge() throws IOException {
        String inventory = read("inventory/service/InventoryService.java");
        String synchronizer = read("core/service/ProductMasterDataReferenceSynchronizer.java");

        assertFalse(inventory.contains("ProductMasterDataReferenceSynchronizer"));
        assertFalse(inventory.contains("masterDataReferences"));
        assertTrue(inventory.contains("synchronizeCanonicalProductWrite(entity)"));
        assertTrue(inventory.contains("canonicalScope.organization("));
        assertTrue(inventory.contains("canonicalScope.unit("));
        assertTrue(synchronizer.contains("synchronizeForBackfill("));
    }

    @Test
    void coreRuntimeDependsOnCanonicalWritePortInsteadOfLegacyMigrationMirror() throws IOException {
        String core = read("core/service/CoreService.java");
        String port = read("core/service/CanonicalMasterDataWritePort.java");
        String adapter = read("core/service/CanonicalMasterDataMirrorService.java");

        assertFalse(core.contains("CanonicalMasterDataMirrorService"));
        assertFalse(core.contains("masterDataMirror"));
        assertTrue(core.contains("CanonicalMasterDataWritePort canonicalMasterData"));
        assertTrue(core.contains("canonicalMasterData.synchronize(entity)"));
        assertTrue(core.contains("canonicalMasterData.delete(entity)"));
        assertTrue(port.contains("boolean synchronize(CoreEntity entity)"));
        assertTrue(adapter.contains("implements CanonicalMasterDataWritePort"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/comandos").resolve(relativePath));
    }
}
