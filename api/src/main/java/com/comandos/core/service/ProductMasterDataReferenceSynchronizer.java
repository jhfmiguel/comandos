package com.comandos.core.service;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.StockLocation;
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
import com.comandos.purchase.model.ProcurementProcess;
import com.comandos.lifecycle.model.PeriodicInspection;
import com.comandos.lifecycle.model.ExceptionOccurrence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Copies canonical master-data identifiers into product-domain shadow columns
 * while legacy numeric JPA relations remain in place.
 *
 * <p>This is the second phase of the persistence cutover: product tables gain
 * stable canonical references before legacy foreign keys are retired.</p>
 */
@Service
public class ProductMasterDataReferenceSynchronizer {

    private final MasterDataReferenceService references;
    private final boolean enabled;

    public ProductMasterDataReferenceSynchronizer(
            MasterDataReferenceService references,
            @Value("${comandos.master-data.product-reference-shadow.enabled:false}")
            boolean enabled) {
        this.references = references;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean synchronize(CoreEntity entity) {
        if (!enabled) return false;
        return synchronizeNow(entity);
    }

    public boolean synchronizeForBackfill(CoreEntity entity) {
        return synchronizeNow(entity);
    }

    private boolean synchronizeNow(CoreEntity entity) {
        if (entity instanceof StockLocation location) {
            synchronizeLocation(location);
            return true;
        }
        if (entity instanceof Purchase purchase) {
            synchronizePurchase(purchase);
            return true;
        }
        if (entity instanceof EquipmentReceiving receiving) {
            synchronizeReceiving(receiving);
            return true;
        }
        if (entity instanceof ProcurementProcess procurement) {
            procurement.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                procurement.organizationLegacyId,
                "procurement process organization"
            );
            return true;
        }
        if (entity instanceof InventoryTransfer transfer) {
            synchronizeTransfer(transfer);
            return true;
        }
        if (entity instanceof Custody custody) {
            synchronizeCustody(custody);
            return true;
        }
        if (entity instanceof Donation donation) {
            synchronizeDonation(donation);
            return true;
        }
        if (entity instanceof InventorySale sale) {
            synchronizeSale(sale);
            return true;
        }
        if (entity instanceof WorkOrder workOrder) {
            workOrder.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                workOrder.organizationLegacyId,
                "work order organization"
            );
            workOrder.unitCanonicalId = optional(
                MasterDataReferenceService.UNIT,
                workOrder.unitLegacyId,
                "work order unit"
            );
            return true;
        }
        if (entity instanceof ApprovalWorkflow workflow) {
            workflow.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                workflow.organizationLegacyId,
                "approval workflow organization"
            );
            workflow.unitCanonicalId = optional(
                MasterDataReferenceService.UNIT,
                workflow.unitLegacyId,
                "approval workflow unit"
            );
            return true;
        }
        if (entity instanceof DisposalProcess disposal) {
            disposal.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                disposal.organizationLegacyId,
                "disposal organization"
            );
            disposal.unitCanonicalId = optional(
                MasterDataReferenceService.UNIT,
                disposal.unitLegacyId,
                "disposal unit"
            );
            return true;
        }
        if (entity instanceof InventoryReservation reservation) {
            reservation.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                reservation.organizationLegacyId,
                "inventory reservation organization"
            );
            reservation.unitCanonicalId = optional(
                MasterDataReferenceService.UNIT,
                reservation.unitLegacyId,
                "inventory reservation unit"
            );
            return true;
        }
        if (entity instanceof InventoryCount count) {
            count.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                count.organizationLegacyId,
                "inventory count organization"
            );
            count.unitCanonicalId = optional(
                MasterDataReferenceService.UNIT,
                count.unitLegacyId,
                "inventory count unit"
            );
            return true;
        }
        if (entity instanceof AmmunitionConsumption consumption) {
            consumption.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                consumption.organizationLegacyId,
                "ammunition consumption organization"
            );
            consumption.unitCanonicalId = optional(
                MasterDataReferenceService.UNIT,
                consumption.unitLegacyId,
                "ammunition consumption unit"
            );
            consumption.responsibleCanonicalId = require(
                MasterDataReferenceService.PERSON,
                consumption.responsibleLegacyId,
                "ammunition consumption responsible"
            );
            consumption.authorizerCanonicalId = require(
                MasterDataReferenceService.PERSON,
                consumption.authorizerLegacyId,
                "ammunition consumption authorizer"
            );
            return true;
        }
        if (entity instanceof ConsumableUsage usage) {
            usage.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                usage.organizationLegacyId,
                "consumable usage organization"
            );
            usage.unitCanonicalId = optional(
                MasterDataReferenceService.UNIT,
                usage.unitLegacyId,
                "consumable usage unit"
            );
            usage.responsibleCanonicalId = require(
                MasterDataReferenceService.PERSON,
                usage.responsibleLegacyId,
                "consumable usage responsible"
            );
            usage.authorizerCanonicalId = require(
                MasterDataReferenceService.PERSON,
                usage.authorizerLegacyId,
                "consumable usage authorizer"
            );
            return true;
        }
        if (entity instanceof PurchasePlanning planning) {
            planning.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                planning.organizationLegacyId,
                "purchase planning organization"
            );
            return true;
        }
        if (entity instanceof PeriodicInspection inspection) {
            inspection.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                inspection.organizationLegacyId,
                "periodic inspection organization"
            );
            inspection.unitCanonicalId = optional(
                MasterDataReferenceService.UNIT,
                inspection.unitLegacyId,
                "periodic inspection unit"
            );
            return true;
        }
        if (entity instanceof ExceptionOccurrence occurrence) {
            occurrence.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                occurrence.organizationLegacyId,
                "exception occurrence organization"
            );
            occurrence.unitCanonicalId = optional(
                MasterDataReferenceService.UNIT,
                occurrence.unitLegacyId,
                "exception occurrence unit"
            );
            return true;
        }
        return false;
    }

    private void synchronizePurchase(Purchase purchase) {
        purchase.buyerOrganizationCanonicalId = require(
            MasterDataReferenceService.ORGANIZATION,
            purchase.buyerOrganizationLegacyId,
            "buyer organization"
        );
        purchase.supplierOrganizationCanonicalId = optional(
            MasterDataReferenceService.ORGANIZATION,
            purchase.supplierOrganizationLegacyId,
            "supplier organization"
        );
        purchase.originPersonCanonicalId = require(
            MasterDataReferenceService.PERSON,
            purchase.originPersonLegacyId,
            "origin person"
        );
    }

    private record ScopeIds(String organization, String unit) {}

    private void synchronizeScope(
            com.comandos.core.model.Organization organization,
            com.comandos.core.model.OrganizationalUnit unit,
            java.util.function.Consumer<ScopeIds> consumer,
            String label) {
        if (organization == null || organization.id == null) {
            throw new IllegalStateException(
                label + " requires a persisted organization before canonical reference synchronization."
            );
        }
        String organizationCanonicalId = require(
            MasterDataReferenceService.ORGANIZATION,
            organization.id,
            label + " organization"
        );
        String unitCanonicalId = optional(
            MasterDataReferenceService.UNIT,
            unit == null ? null : unit.id,
            label + " unit"
        );
        consumer.accept(new ScopeIds(organizationCanonicalId, unitCanonicalId));
    }

    private void synchronizeCustody(Custody custody) {
        custody.organizationCanonicalId = require(
            MasterDataReferenceService.ORGANIZATION,
            custody.organizationLegacyId,
            "custody organization"
        );
        custody.authorizerCanonicalId = require(
            MasterDataReferenceService.PERSON,
            custody.authorizerLegacyId,
            "custody authorizer"
        );
        custody.unitCanonicalId = optional(
            MasterDataReferenceService.UNIT,
            custody.unitLegacyId,
            "custody unit"
        );
        custody.recipientCanonicalId = optional(
            MasterDataReferenceService.PERSON,
            custody.recipientLegacyId,
            "custody recipient"
        );
        custody.recipientUnitCanonicalId = optional(
            MasterDataReferenceService.UNIT,
            custody.recipientUnitLegacyId,
            "custody recipient unit"
        );
    }

    private void synchronizeDonation(Donation donation) {

        donation.organizationCanonicalId = require(
            MasterDataReferenceService.ORGANIZATION,
            donation.organizationLegacyId,
            "donation organization"
        );
        donation.unitCanonicalId = optional(
            MasterDataReferenceService.UNIT,
            donation.unitLegacyId,
            "donation unit"
        );
        donation.donorCanonicalId = require(
            MasterDataReferenceService.PERSON,
            donation.donorLegacyId,
            "donation donor"
        );
        donation.doneeCanonicalId = require(
            MasterDataReferenceService.PERSON,
            donation.doneeLegacyId,
            "donation donee"
        );
    }

    private void synchronizeSale(InventorySale sale) {

        sale.organizationCanonicalId = require(
            MasterDataReferenceService.ORGANIZATION,
            sale.organizationLegacyId,
            "sale organization"
        );
        sale.unitCanonicalId = optional(
            MasterDataReferenceService.UNIT,
            sale.unitLegacyId,
            "sale unit"
        );
        sale.buyerCanonicalId = require(
            MasterDataReferenceService.PERSON,
            sale.buyerLegacyId,
            "sale buyer"
        );
    }

    private String require(String resourceType, Long legacyId, String label) {
        if (legacyId == null) {
            throw new IllegalStateException(label + " must be persisted before canonical reference synchronization.");
        }
        return references.resolveCanonicalId(resourceType, legacyId)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical " + label + " reference is missing for legacy id " + legacyId
            ));
    }

    private String optional(String resourceType, Long legacyId, String label) {
        if (legacyId == null) return null;
        return require(resourceType, legacyId, label);
    }

    private void synchronizeTransfer(InventoryTransfer transfer) {
        transfer.organizationCanonicalId = require(
            MasterDataReferenceService.ORGANIZATION,
            transfer.organizationLegacyId,
            "transfer organization"
        );
        transfer.sourceUnitCanonicalId = require(
            MasterDataReferenceService.UNIT,
            transfer.sourceUnitLegacyId,
            "transfer source unit"
        );
        transfer.destinationUnitCanonicalId = require(
            MasterDataReferenceService.UNIT,
            transfer.destinationUnitLegacyId,
            "transfer destination unit"
        );
    }

    private void synchronizeReceiving(EquipmentReceiving receiving) {
        if (receiving.receivingOrganizationLegacyId == null) {
            receiving.receivingOrganizationCanonicalId = null;
            return;
        }

        receiving.receivingOrganizationCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.ORGANIZATION,
                receiving.receivingOrganizationLegacyId)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical receiving organization reference is missing for legacy id "
                    + receiving.receivingOrganizationLegacyId
            ));
    }

    private void synchronizeLocation(StockLocation location) {
        if (location.organizationLegacyId == null) {
            throw new IllegalStateException(
                "Stock location requires a persisted organization before canonical reference synchronization."
            );
        }

        location.organizationCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.ORGANIZATION,
                location.organizationLegacyId)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical organization reference is missing for legacy id "
                    + location.organizationLegacyId
            ));

        if (location.unitLegacyId == null) {
            location.unitCanonicalId = null;
            return;
        }

        location.unitCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.UNIT,
                location.unitLegacyId)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical organizational unit reference is missing for legacy id "
                    + location.unitLegacyId
            ));
    }
}
