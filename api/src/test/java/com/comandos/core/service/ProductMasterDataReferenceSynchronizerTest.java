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
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(10L)).thenReturn("org-canonical-10");
        when(canonicalScope.unit(20L)).thenReturn("unit-canonical-20");

        Organization organization = new Organization();
        organization.id = 10L;
        OrganizationalUnit unit = new OrganizationalUnit();
        unit.id = 20L;

        StockLocation location = new StockLocation();
        location.organizationLegacyId = organization.id;
        location.unitLegacyId = unit.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertTrue(synchronizer.synchronizeForBackfill(location));
        assertEquals("org-canonical-10", location.organizationCanonicalId);
        assertEquals("unit-canonical-20", location.unitCanonicalId);
    }

    @Test
    void clearsUnitCanonicalIdWhenLocationHasNoUnit() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(10L)).thenReturn("org-canonical-10");

        Organization organization = new Organization();
        organization.id = 10L;

        StockLocation location = new StockLocation();
        location.organizationLegacyId = organization.id;
        location.unitCanonicalId = "stale-unit";

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertTrue(synchronizer.synchronizeForBackfill(location));
        assertEquals("org-canonical-10", location.organizationCanonicalId);
        assertNull(location.unitCanonicalId);
    }

    @Test
    void failsWhenCanonicalOrganizationReferenceIsMissing() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(10L)).thenThrow(new IllegalStateException("Canonical record missing for legacy id 10"));

        Organization organization = new Organization();
        organization.id = 10L;

        StockLocation location = new StockLocation();
        location.organizationLegacyId = organization.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> synchronizer.synchronizeForBackfill(location)
        );

        assertTrue(error.getMessage().contains("legacy id 10"));
    }

    @Test
    void ignoresIndirectInventoryEntities() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertFalse(synchronizer.synchronizeForBackfill(new StockMovement()));
        verifyNoInteractions(canonicalScope);
    }

    @Test
    void synchronizesPurchaseMasterDataReferences() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(10L)).thenReturn("buyer-org");
        when(canonicalScope.organization(11L)).thenReturn("supplier-org");
        when(canonicalScope.person(20L)).thenReturn("origin-person");

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
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertTrue(synchronizer.synchronizeForBackfill(purchase));
        assertEquals("buyer-org", purchase.buyerOrganizationCanonicalId);
        assertEquals("supplier-org", purchase.supplierOrganizationCanonicalId);
        assertEquals("origin-person", purchase.originPersonCanonicalId);
    }

    @Test
    void synchronizesOptionalReceivingOrganization() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(30L)).thenReturn("receiving-org");

        EquipmentReceiving receiving = new EquipmentReceiving();
        receiving.receivingOrganizationLegacyId = 30L;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertTrue(synchronizer.synchronizeForBackfill(receiving));
        assertEquals("receiving-org", receiving.receivingOrganizationCanonicalId);
    }

    @Test
    void clearsOptionalPurchaseSupplierReference() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(10L)).thenReturn("buyer-org");
        when(canonicalScope.person(20L)).thenReturn("origin-person");

        Organization buyer = new Organization();
        buyer.id = 10L;
        com.comandos.core.model.Person origin = new com.comandos.core.model.Person();
        origin.id = 20L;

        Purchase purchase = new Purchase();
        purchase.buyerOrganizationLegacyId = buyer.id;
        purchase.originPersonLegacyId = origin.id;
        purchase.supplierOrganizationCanonicalId = "stale-supplier";

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertTrue(synchronizer.synchronizeForBackfill(purchase));
        assertNull(purchase.supplierOrganizationCanonicalId);
    }

    @Test
    void synchronizesInventoryTransferScope() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(40L)).thenReturn("transfer-org");
        when(canonicalScope.unit(41L)).thenReturn("source-unit");
        when(canonicalScope.unit(42L)).thenReturn("destination-unit");

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
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertTrue(synchronizer.synchronizeForBackfill(transfer));
        assertEquals("transfer-org", transfer.organizationCanonicalId);
        assertEquals("source-unit", transfer.sourceUnitCanonicalId);
        assertEquals("destination-unit", transfer.destinationUnitCanonicalId);
    }

    @Test
    void synchronizesCustodyReferences() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(50L)).thenReturn("custody-org");
        when(canonicalScope.unit(51L)).thenReturn("custody-unit");
        when(canonicalScope.person(52L)).thenReturn("recipient-person");
        when(canonicalScope.unit(53L)).thenReturn("recipient-unit");
        when(canonicalScope.person(54L)).thenReturn("authorizer-person");

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
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertTrue(synchronizer.synchronizeForBackfill(custody));
        assertEquals("custody-org", custody.organizationCanonicalId);
        assertEquals("custody-unit", custody.unitCanonicalId);
        assertEquals("recipient-person", custody.recipientCanonicalId);
        assertEquals("recipient-unit", custody.recipientUnitCanonicalId);
        assertEquals("authorizer-person", custody.authorizerCanonicalId);
    }

    @Test
    void synchronizesDonationReferences() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(60L)).thenReturn("donation-org");
        when(canonicalScope.unit(61L)).thenReturn("donation-unit");
        when(canonicalScope.person(62L)).thenReturn("donor-person");
        when(canonicalScope.person(63L)).thenReturn("donee-person");

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
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertTrue(synchronizer.synchronizeForBackfill(donation));
        assertEquals("donation-org", donation.organizationCanonicalId);
        assertEquals("donation-unit", donation.unitCanonicalId);
        assertEquals("donor-person", donation.donorCanonicalId);
        assertEquals("donee-person", donation.doneeCanonicalId);
    }

    @Test
    void synchronizesSaleReferences() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(70L)).thenReturn("sale-org");
        when(canonicalScope.unit(71L)).thenReturn("sale-unit");
        when(canonicalScope.person(72L)).thenReturn("buyer-person");

        Organization organization = new Organization(); organization.id = 70L;
        OrganizationalUnit unit = new OrganizationalUnit(); unit.id = 71L;
        com.comandos.core.model.Person buyer = new com.comandos.core.model.Person(); buyer.id = 72L;

        InventorySale sale = new InventorySale();
        sale.organizationLegacyId = organization.id;
        sale.unitLegacyId = unit.id;
        sale.buyerLegacyId = buyer.id;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertTrue(synchronizer.synchronizeForBackfill(sale));
        assertEquals("sale-org", sale.organizationCanonicalId);
        assertEquals("sale-unit", sale.unitCanonicalId);
        assertEquals("buyer-person", sale.buyerCanonicalId);
    }

    @Test
    void synchronizesOperationalScopeEntities() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(80L)).thenReturn("ops-org");
        when(canonicalScope.unit(81L)).thenReturn("ops-unit");

        Organization organization = new Organization(); organization.id = 80L;
        OrganizationalUnit unit = new OrganizationalUnit(); unit.id = 81L;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        WorkOrder workOrder = new WorkOrder();
        workOrder.organizationLegacyId = organization.id;
        workOrder.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronizeForBackfill(workOrder));
        assertEquals("ops-org", workOrder.organizationCanonicalId);
        assertEquals("ops-unit", workOrder.unitCanonicalId);

        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.organizationLegacyId = organization.id;
        workflow.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronizeForBackfill(workflow));
        assertEquals("ops-org", workflow.organizationCanonicalId);
        assertEquals("ops-unit", workflow.unitCanonicalId);

        DisposalProcess disposal = new DisposalProcess();
        disposal.organizationLegacyId = organization.id;
        disposal.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronizeForBackfill(disposal));
        assertEquals("ops-org", disposal.organizationCanonicalId);
        assertEquals("ops-unit", disposal.unitCanonicalId);

        InventoryReservation reservation = new InventoryReservation();
        reservation.organizationLegacyId = organization.id;
        reservation.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronizeForBackfill(reservation));
        assertEquals("ops-org", reservation.organizationCanonicalId);
        assertEquals("ops-unit", reservation.unitCanonicalId);

        InventoryCount count = new InventoryCount();
        count.organizationLegacyId = organization.id;
        count.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronizeForBackfill(count));
        assertEquals("ops-org", count.organizationCanonicalId);
        assertEquals("ops-unit", count.unitCanonicalId);
    }

    @Test
    void synchronizesRemainingMasterDataReferences() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(90L)).thenReturn("remaining-org");
        when(canonicalScope.unit(91L)).thenReturn("remaining-unit");
        when(canonicalScope.person(92L)).thenReturn("responsible-person");
        when(canonicalScope.person(93L)).thenReturn("authorizer-person");

        Organization organization = new Organization(); organization.id = 90L;
        OrganizationalUnit unit = new OrganizationalUnit(); unit.id = 91L;
        com.comandos.core.model.Person responsible = new com.comandos.core.model.Person(); responsible.id = 92L;
        com.comandos.core.model.Person authorizer = new com.comandos.core.model.Person(); authorizer.id = 93L;

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        AmmunitionConsumption ammunition = new AmmunitionConsumption();
        ammunition.organizationLegacyId = organization.id;
        ammunition.unitLegacyId = unit.id;
        ammunition.responsibleLegacyId = responsible.id;
        ammunition.authorizerLegacyId = authorizer.id;
        assertTrue(synchronizer.synchronizeForBackfill(ammunition));
        assertEquals("remaining-org", ammunition.organizationCanonicalId);
        assertEquals("remaining-unit", ammunition.unitCanonicalId);
        assertEquals("responsible-person", ammunition.responsibleCanonicalId);
        assertEquals("authorizer-person", ammunition.authorizerCanonicalId);

        ConsumableUsage usage = new ConsumableUsage();
        usage.organizationLegacyId = organization.id;
        usage.unitLegacyId = unit.id;
        usage.responsibleLegacyId = responsible.id;
        usage.authorizerLegacyId = authorizer.id;
        assertTrue(synchronizer.synchronizeForBackfill(usage));
        assertEquals("remaining-org", usage.organizationCanonicalId);
        assertEquals("remaining-unit", usage.unitCanonicalId);

        PurchasePlanning planning = new PurchasePlanning();
        planning.organizationLegacyId = organization.id;
        assertTrue(synchronizer.synchronizeForBackfill(planning));
        assertEquals("remaining-org", planning.organizationCanonicalId);

        PeriodicInspection inspection = new PeriodicInspection();
        inspection.organizationLegacyId = organization.id;
        inspection.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronizeForBackfill(inspection));
        assertEquals("remaining-org", inspection.organizationCanonicalId);
        assertEquals("remaining-unit", inspection.unitCanonicalId);

        ExceptionOccurrence occurrence = new ExceptionOccurrence();
        occurrence.organizationLegacyId = organization.id;
        occurrence.unitLegacyId = unit.id;
        assertTrue(synchronizer.synchronizeForBackfill(occurrence));
        assertEquals("remaining-org", occurrence.organizationCanonicalId);
        assertEquals("remaining-unit", occurrence.unitCanonicalId);
    }

    @Test
    void historicalBackfillIsIndependentOfTheLegacyRuntimeShadowFlag() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        when(canonicalScope.organization(100L)).thenReturn("backfill-org");
        PurchasePlanning planning = new PurchasePlanning();
        planning.organizationLegacyId = 100L;
        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, false);

        assertFalse(synchronizer.enabled());
        assertTrue(synchronizer.synchronizeForBackfill(planning));
        assertEquals("backfill-org", planning.organizationCanonicalId);
    }

    @Test
    void clearsReceivingOrganizationWhenLegacyReferenceIsAbsent() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        EquipmentReceiving receiving = new EquipmentReceiving();
        receiving.receivingOrganizationCanonicalId = "stale-organization";

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertTrue(synchronizer.synchronizeForBackfill(receiving));
        assertNull(receiving.receivingOrganizationCanonicalId);
        verifyNoInteractions(canonicalScope);
    }

    @Test
    void stockLocationRequiresOrganizationBeforeSynchronizing() {
        ProductCanonicalScopeResolver canonicalScope = mock(ProductCanonicalScopeResolver.class);
        StockLocation location = new StockLocation();
        location.organizationCanonicalId = "previous-organization";

        ProductMasterDataReferenceSynchronizer synchronizer =
            new ProductMasterDataReferenceSynchronizer(canonicalScope, true);

        assertThrows(IllegalStateException.class, () -> synchronizer.synchronizeForBackfill(location));
        assertEquals("previous-organization", location.organizationCanonicalId);
        verifyNoInteractions(canonicalScope);
    }

    @Test
    void backfillDoesNotDependOnLegacyShadowWriteEnabled() {
        var resolver = mock(ProductCanonicalScopeResolver.class);
        when(resolver.organization(10L)).thenReturn("canonical-org-10");
        var location = new StockLocation();
        location.organizationLegacyId = 10L;
        var synchronizer = new ProductMasterDataReferenceSynchronizer(resolver, false);
        assertTrue(synchronizer.synchronizeForBackfill(location));
        assertEquals("canonical-org-10", location.organizationCanonicalId);
    }
}
