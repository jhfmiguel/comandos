package com.comandos.core.service;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.StockMovement;
import com.comandos.purchase.model.EquipmentReceiving;
import com.comandos.purchase.model.Purchase;
import com.comandos.transfer.model.InventoryTransfer;
import com.comandos.custody.model.Custody;
import com.comandos.donation.model.Donation;
import com.comandos.sales.model.InventorySale;
import com.comandos.maintenance.model.WorkOrder;
import com.comandos.workflow.model.ApprovalWorkflow;
import com.comandos.disposal.model.DisposalProcess;
import com.comandos.reservation.model.InventoryReservation;
import com.comandos.reconciliation.model.InventoryCount;
import com.comandos.consumption.model.AmmunitionConsumption;
import com.comandos.consumption.model.ConsumableUsage;
import com.comandos.purchase.model.PurchasePlanning;
import com.comandos.lifecycle.model.PeriodicInspection;
import com.comandos.lifecycle.model.ExceptionOccurrence;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductMasterDataReferenceSynchronizerTest {

    @Test
    void synchronizesOrganizationAndUnitForStockLocation() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 10L))
            .thenReturn(Optional.of("org-canonical-10"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 20L))
            .thenReturn(Optional.of("unit-canonical-20"));

        Organization organization = new Organization();
        organization.id = 10L;
        OrganizationalUnit unit = new OrganizationalUnit();
        unit.id = 20L;

        StockLocation location = new StockLocation();
        location.organizationLegacyId = organization.id;
        location.unitLegacyId = unit.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(location));
        assertEquals("org-canonical-10", location.organizationCanonicalId);
        assertEquals("unit-canonical-20", location.unitCanonicalId);
    }

    @Test
    void clearsUnitCanonicalIdWhenLocationHasNoUnit() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 10L))
            .thenReturn(Optional.of("org-canonical-10"));

        Organization organization = new Organization();
        organization.id = 10L;

        StockLocation location = new StockLocation();
        location.organizationLegacyId = organization.id;
        location.unitCanonicalId = "stale-unit";

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(location));
        assertEquals("org-canonical-10", location.organizationCanonicalId);
        assertNull(location.unitCanonicalId);
    }

    @Test
    void failsWhenCanonicalOrganizationReferenceIsMissing() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 10L))
            .thenReturn(Optional.empty());

        Organization organization = new Organization();
        organization.id = 10L;

        StockLocation location = new StockLocation();
        location.organizationLegacyId = organization.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> synchronizer.synchronize(location)
        );

        assertTrue(error.getMessage().contains("legacy id 10"));
    }

    @Test
    void ignoresIndirectInventoryEntities() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertFalse(synchronizer.synchronize(new StockMovement()));
        verifyNoInteractions(references);
    }

    @Test
    void synchronizesPurchaseMasterDataReferences() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 10L))
            .thenReturn(Optional.of("buyer-org"));
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 11L))
            .thenReturn(Optional.of("supplier-org"));
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 20L))
            .thenReturn(Optional.of("origin-person"));

        Organization buyer = new Organization();
        buyer.id = 10L;
        Organization supplier = new Organization();
        supplier.id = 11L;
        com.comandos.core.model.Person origin = new com.comandos.core.model.Person();
        origin.id = 20L;

        Purchase purchase = new Purchase();
        purchase.buyerOrganizationLegacyId = buyer.id;
        purchase.supplierOrganizationLegacyId = supplier.id;
        purchase.originPersonLegacyId = origin.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(purchase));
        assertEquals("buyer-org", purchase.buyerOrganizationCanonicalId);
        assertEquals("supplier-org", purchase.supplierOrganizationCanonicalId);
        assertEquals("origin-person", purchase.originPersonCanonicalId);
    }

    @Test
    void synchronizesOptionalReceivingOrganization() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 30L))
            .thenReturn(Optional.of("receiving-org"));

        EquipmentReceiving receiving = new EquipmentReceiving();
        receiving.receivingOrganizationLegacyId = 30L;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(receiving));
        assertEquals("receiving-org", receiving.receivingOrganizationCanonicalId);
    }

    @Test
    void clearsOptionalPurchaseSupplierReference() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 10L))
            .thenReturn(Optional.of("buyer-org"));
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 20L))
            .thenReturn(Optional.of("origin-person"));

        Organization buyer = new Organization();
        buyer.id = 10L;
        com.comandos.core.model.Person origin = new com.comandos.core.model.Person();
        origin.id = 20L;

        Purchase purchase = new Purchase();
        purchase.buyerOrganizationLegacyId = buyer.id;
        purchase.originPersonLegacyId = origin.id;
        purchase.supplierOrganizationCanonicalId = "stale-supplier";

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(purchase));
        assertNull(purchase.supplierOrganizationCanonicalId);
    }

    @Test
    void synchronizesInventoryTransferScope() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 40L))
            .thenReturn(Optional.of("transfer-org"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 41L))
            .thenReturn(Optional.of("source-unit"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 42L))
            .thenReturn(Optional.of("destination-unit"));

        Organization organization = new Organization();
        organization.id = 40L;
        OrganizationalUnit source = new OrganizationalUnit();
        source.id = 41L;
        OrganizationalUnit destination = new OrganizationalUnit();
        destination.id = 42L;

        InventoryTransfer transfer = new InventoryTransfer();
        transfer.organizationLegacyId = organization.id;
        transfer.sourceUnitLegacyId = source.id;
        transfer.destinationUnitLegacyId = destination.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(transfer));
        assertEquals("transfer-org", transfer.organizationCanonicalId);
        assertEquals("source-unit", transfer.sourceUnitCanonicalId);
        assertEquals("destination-unit", transfer.destinationUnitCanonicalId);
    }

    @Test
    void synchronizesCustodyReferences() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 50L))
            .thenReturn(Optional.of("custody-org"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 51L))
            .thenReturn(Optional.of("custody-unit"));
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 52L))
            .thenReturn(Optional.of("recipient-person"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 53L))
            .thenReturn(Optional.of("recipient-unit"));
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 54L))
            .thenReturn(Optional.of("authorizer-person"));

        Organization organization = new Organization(); organization.id = 50L;
        OrganizationalUnit unit = new OrganizationalUnit(); unit.id = 51L;
        com.comandos.core.model.Person recipient = new com.comandos.core.model.Person(); recipient.id = 52L;
        OrganizationalUnit recipientUnit = new OrganizationalUnit(); recipientUnit.id = 53L;
        com.comandos.core.model.Person authorizer = new com.comandos.core.model.Person(); authorizer.id = 54L;

        Custody custody = new Custody();
        custody.organizationLegacyId = organization.id;
        custody.unitLegacyId = unit.id;
        custody.recipientLegacyId = recipient.id;
        custody.recipientUnitLegacyId = recipientUnit.id;
        custody.authorizerLegacyId = authorizer.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(custody));
        assertEquals("custody-org", custody.organizationCanonicalId);
        assertEquals("custody-unit", custody.unitCanonicalId);
        assertEquals("recipient-person", custody.recipientCanonicalId);
        assertEquals("recipient-unit", custody.recipientUnitCanonicalId);
        assertEquals("authorizer-person", custody.authorizerCanonicalId);
    }

    @Test
    void synchronizesDonationReferences() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 60L))
            .thenReturn(Optional.of("donation-org"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 61L))
            .thenReturn(Optional.of("donation-unit"));
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 62L))
            .thenReturn(Optional.of("donor-person"));
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 63L))
            .thenReturn(Optional.of("donee-person"));

        Organization organization = new Organization(); organization.id = 60L;
        OrganizationalUnit unit = new OrganizationalUnit(); unit.id = 61L;
        com.comandos.core.model.Person donor = new com.comandos.core.model.Person(); donor.id = 62L;
        com.comandos.core.model.Person donee = new com.comandos.core.model.Person(); donee.id = 63L;

        Donation donation = new Donation();
        donation.organizationLegacyId = organization.id;
        donation.unitLegacyId = unit.id;
        donation.donorLegacyId = donor.id;
        donation.doneeLegacyId = donee.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(donation));
        assertEquals("donation-org", donation.organizationCanonicalId);
        assertEquals("donation-unit", donation.unitCanonicalId);
        assertEquals("donor-person", donation.donorCanonicalId);
        assertEquals("donee-person", donation.doneeCanonicalId);
    }

    @Test
    void synchronizesSaleReferences() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 70L))
            .thenReturn(Optional.of("sale-org"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 71L))
            .thenReturn(Optional.of("sale-unit"));
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 72L))
            .thenReturn(Optional.of("buyer-person"));

        Organization organization = new Organization(); organization.id = 70L;
        OrganizationalUnit unit = new OrganizationalUnit(); unit.id = 71L;
        com.comandos.core.model.Person buyer = new com.comandos.core.model.Person(); buyer.id = 72L;

        InventorySale sale = new InventorySale();
        sale.organizationLegacyId = organization.id;
        sale.unitLegacyId = unit.id;
        sale.buyerLegacyId = buyer.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        assertTrue(synchronizer.synchronize(sale));
        assertEquals("sale-org", sale.organizationCanonicalId);
        assertEquals("sale-unit", sale.unitCanonicalId);
        assertEquals("buyer-person", sale.buyerCanonicalId);
    }

    @Test
    void synchronizesOperationalScopeEntities() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 80L))
            .thenReturn(Optional.of("ops-org"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 81L))
            .thenReturn(Optional.of("ops-unit"));

        Organization organization = new Organization(); organization.id = 80L;
        OrganizationalUnit unit = new OrganizationalUnit(); unit.id = 81L;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        WorkOrder workOrder = new WorkOrder();
        workOrder.organizationLegacyId = organization.id;
        workOrder.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronize(workOrder));
        assertEquals("ops-org", workOrder.organizationCanonicalId);
        assertEquals("ops-unit", workOrder.unitCanonicalId);

        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.organizationLegacyId = organization.id;
        workflow.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronize(workflow));
        assertEquals("ops-org", workflow.organizationCanonicalId);
        assertEquals("ops-unit", workflow.unitCanonicalId);

        DisposalProcess disposal = new DisposalProcess();
        disposal.organizationLegacyId = organization.id;
        disposal.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronize(disposal));
        assertEquals("ops-org", disposal.organizationCanonicalId);
        assertEquals("ops-unit", disposal.unitCanonicalId);

        InventoryReservation reservation = new InventoryReservation();
        reservation.organizationLegacyId = organization.id;
        reservation.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronize(reservation));
        assertEquals("ops-org", reservation.organizationCanonicalId);
        assertEquals("ops-unit", reservation.unitCanonicalId);

        InventoryCount count = new InventoryCount();
        count.organizationLegacyId = organization.id;
        count.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronize(count));
        assertEquals("ops-org", count.organizationCanonicalId);
        assertEquals("ops-unit", count.unitCanonicalId);
    }

    @Test
    void synchronizesRemainingMasterDataReferences() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 90L))
            .thenReturn(Optional.of("remaining-org"));
        when(references.resolveCanonicalId(MasterDataReferenceService.UNIT, 91L))
            .thenReturn(Optional.of("remaining-unit"));
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 92L))
            .thenReturn(Optional.of("responsible-person"));
        when(references.resolveCanonicalId(MasterDataReferenceService.PERSON, 93L))
            .thenReturn(Optional.of("authorizer-person"));

        Organization organization = new Organization(); organization.id = 90L;
        OrganizationalUnit unit = new OrganizationalUnit(); unit.id = 91L;
        com.comandos.core.model.Person responsible = new com.comandos.core.model.Person(); responsible.id = 92L;
        com.comandos.core.model.Person authorizer = new com.comandos.core.model.Person(); authorizer.id = 93L;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, true);

        AmmunitionConsumption ammunition = new AmmunitionConsumption();
        ammunition.organizationLegacyId = organization.id;
        ammunition.unitLegacyId = unit.id;
        ammunition.responsibleLegacyId = responsible.id;
        ammunition.authorizerLegacyId = authorizer.id;
        assertTrue(synchronizer.synchronize(ammunition));
        assertEquals("remaining-org", ammunition.organizationCanonicalId);
        assertEquals("remaining-unit", ammunition.unitCanonicalId);
        assertEquals("responsible-person", ammunition.responsibleCanonicalId);
        assertEquals("authorizer-person", ammunition.authorizerCanonicalId);

        ConsumableUsage usage = new ConsumableUsage();
        usage.organizationLegacyId = organization.id;
        usage.unitLegacyId = unit.id;
        usage.responsibleLegacyId = responsible.id;
        usage.authorizerLegacyId = authorizer.id;
        assertTrue(synchronizer.synchronize(usage));
        assertEquals("remaining-org", usage.organizationCanonicalId);
        assertEquals("remaining-unit", usage.unitCanonicalId);

        PurchasePlanning planning = new PurchasePlanning();
        planning.organizationLegacyId = organization.id;
        assertTrue(synchronizer.synchronize(planning));
        assertEquals("remaining-org", planning.organizationCanonicalId);

        PeriodicInspection inspection = new PeriodicInspection();
        inspection.organizationLegacyId = organization.id;
        inspection.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronize(inspection));
        assertEquals("remaining-org", inspection.organizationCanonicalId);
        assertEquals("remaining-unit", inspection.unitCanonicalId);

        ExceptionOccurrence occurrence = new ExceptionOccurrence();
        occurrence.organizationLegacyId = organization.id;
        occurrence.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronize(occurrence));
        assertEquals("remaining-org", occurrence.organizationCanonicalId);
        assertEquals("remaining-unit", occurrence.unitCanonicalId);
    }

    @Test
    void backfillBypassesRuntimeShadowFlag() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        when(references.resolveCanonicalId(MasterDataReferenceService.ORGANIZATION, 100L))
            .thenReturn(Optional.of("backfill-org"));

        Organization organization = new Organization();
        organization.id = 100L;

        PurchasePlanning planning = new PurchasePlanning();
        planning.organizationLegacyId = organization.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, false);

        assertFalse(synchronizer.synchronize(planning));
        assertNull(planning.organizationCanonicalId);

        assertTrue(synchronizer.synchronizeForBackfill(planning));
        assertEquals("backfill-org", planning.organizationCanonicalId);
    }

    @Test
    void runtimeFlagCanDisableSynchronization() {
        MasterDataReferenceService references = mock(MasterDataReferenceService.class);
        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(references, false);

        StockLocation location = new StockLocation();

        assertFalse(synchronizer.synchronize(location));
        verifyNoInteractions(references);
    }
}
