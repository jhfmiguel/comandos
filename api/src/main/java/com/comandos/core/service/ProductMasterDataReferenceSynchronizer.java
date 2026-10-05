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
            synchronizeScope(workOrder.organization, workOrder.unit, ids -> {
                workOrder.organizationCanonicalId = ids.organization();
                workOrder.unitCanonicalId = ids.unit();
            }, "work order");
            return true;
        }
        if (entity instanceof ApprovalWorkflow workflow) {
            synchronizeScope(workflow.organization, workflow.unit, ids -> {
                workflow.organizationCanonicalId = ids.organization();
                workflow.unitCanonicalId = ids.unit();
            }, "approval workflow");
            return true;
        }
        if (entity instanceof DisposalProcess disposal) {
            synchronizeScope(disposal.organization, disposal.unit, ids -> {
                disposal.organizationCanonicalId = ids.organization();
                disposal.unitCanonicalId = ids.unit();
            }, "disposal process");
            return true;
        }
        if (entity instanceof InventoryReservation reservation) {
            synchronizeScope(reservation.organization, reservation.unit, ids -> {
                reservation.organizationCanonicalId = ids.organization();
                reservation.unitCanonicalId = ids.unit();
            }, "inventory reservation");
            return true;
        }
        if (entity instanceof InventoryCount count) {
            synchronizeScope(count.organization, count.unit, ids -> {
                count.organizationCanonicalId = ids.organization();
                count.unitCanonicalId = ids.unit();
            }, "inventory count");
            return true;
        }
        if (entity instanceof AmmunitionConsumption consumption) {
            synchronizeScope(consumption.organization, consumption.unit, ids -> {
                consumption.organizationCanonicalId = ids.organization();
                consumption.unitCanonicalId = ids.unit();
            }, "ammunition consumption");
            consumption.responsibleCanonicalId = require(
                MasterDataReferenceService.PERSON,
                consumption.responsible == null ? null : consumption.responsible.id,
                "ammunition consumption responsible"
            );
            consumption.authorizerCanonicalId = require(
                MasterDataReferenceService.PERSON,
                consumption.authorizer == null ? null : consumption.authorizer.id,
                "ammunition consumption authorizer"
            );
            return true;
        }
        if (entity instanceof ConsumableUsage usage) {
            synchronizeScope(usage.organization, usage.unit, ids -> {
                usage.organizationCanonicalId = ids.organization();
                usage.unitCanonicalId = ids.unit();
            }, "consumable usage");
            usage.responsibleCanonicalId = require(
                MasterDataReferenceService.PERSON,
                usage.responsible == null ? null : usage.responsible.id,
                "consumable usage responsible"
            );
            usage.authorizerCanonicalId = require(
                MasterDataReferenceService.PERSON,
                usage.authorizer == null ? null : usage.authorizer.id,
                "consumable usage authorizer"
            );
            return true;
        }
        if (entity instanceof PurchasePlanning planning) {
            planning.organizationCanonicalId = require(
                MasterDataReferenceService.ORGANIZATION,
                planning.organization == null ? null : planning.organization.id,
                "purchase planning organization"
            );
            return true;
        }
        if (entity instanceof PeriodicInspection inspection) {
            synchronizeScope(inspection.organization, inspection.unit, ids -> {
                inspection.organizationCanonicalId = ids.organization();
                inspection.unitCanonicalId = ids.unit();
            }, "periodic inspection");
            return true;
        }
        if (entity instanceof ExceptionOccurrence occurrence) {
            synchronizeScope(occurrence.organization, occurrence.unit, ids -> {
                occurrence.organizationCanonicalId = ids.organization();
                occurrence.unitCanonicalId = ids.unit();
            }, "exception occurrence");
            return true;
        }
        return false;
    }

    private void synchronizePurchase(Purchase purchase) {
        if (purchase.buyerOrganization == null || purchase.buyerOrganization.id == null) {
            throw new IllegalStateException(
                "Purchase requires a persisted buyer organization before canonical reference synchronization."
            );
        }
        if (purchase.originPerson == null || purchase.originPerson.id == null) {
            throw new IllegalStateException(
                "Purchase requires a persisted origin person before canonical reference synchronization."
            );
        }

        purchase.buyerOrganizationCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.ORGANIZATION,
                purchase.buyerOrganization.id)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical buyer organization reference is missing for legacy id "
                    + purchase.buyerOrganization.id
            ));

        purchase.originPersonCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.PERSON,
                purchase.originPerson.id)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical origin person reference is missing for legacy id "
                    + purchase.originPerson.id
            ));

        if (purchase.supplierOrganization == null) {
            purchase.supplierOrganizationCanonicalId = null;
            return;
        }
        if (purchase.supplierOrganization.id == null) {
            throw new IllegalStateException(
                "Purchase supplier organization must be persisted before canonical reference synchronization."
            );
        }

        purchase.supplierOrganizationCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.ORGANIZATION,
                purchase.supplierOrganization.id)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical supplier organization reference is missing for legacy id "
                    + purchase.supplierOrganization.id
            ));
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
        if (custody.organization == null || custody.organization.id == null) {
            throw new IllegalStateException("Custody requires a persisted organization before canonical reference synchronization.");
        }
        if (custody.authorizer == null || custody.authorizer.id == null) {
            throw new IllegalStateException("Custody requires a persisted authorizer before canonical reference synchronization.");
        }

        custody.organizationCanonicalId = require(
            MasterDataReferenceService.ORGANIZATION,
            custody.organization.id,
            "custody organization"
        );
        custody.authorizerCanonicalId = require(
            MasterDataReferenceService.PERSON,
            custody.authorizer.id,
            "custody authorizer"
        );
        custody.unitCanonicalId = optional(
            MasterDataReferenceService.UNIT,
            custody.unit == null ? null : custody.unit.id,
            "custody unit"
        );
        custody.recipientCanonicalId = optional(
            MasterDataReferenceService.PERSON,
            custody.recipient == null ? null : custody.recipient.id,
            "custody recipient"
        );
        custody.recipientUnitCanonicalId = optional(
            MasterDataReferenceService.UNIT,
            custody.recipientUnit == null ? null : custody.recipientUnit.id,
            "custody recipient unit"
        );
    }

    private void synchronizeDonation(Donation donation) {
        if (donation.organization == null || donation.organization.id == null) {
            throw new IllegalStateException("Donation requires a persisted organization before canonical reference synchronization.");
        }
        if (donation.donor == null || donation.donor.id == null) {
            throw new IllegalStateException("Donation requires a persisted donor before canonical reference synchronization.");
        }
        if (donation.donee == null || donation.donee.id == null) {
            throw new IllegalStateException("Donation requires a persisted donee before canonical reference synchronization.");
        }

        donation.organizationCanonicalId = require(
            MasterDataReferenceService.ORGANIZATION,
            donation.organization.id,
            "donation organization"
        );
        donation.unitCanonicalId = optional(
            MasterDataReferenceService.UNIT,
            donation.unit == null ? null : donation.unit.id,
            "donation unit"
        );
        donation.donorCanonicalId = require(
            MasterDataReferenceService.PERSON,
            donation.donor.id,
            "donation donor"
        );
        donation.doneeCanonicalId = require(
            MasterDataReferenceService.PERSON,
            donation.donee.id,
            "donation donee"
        );
    }

    private void synchronizeSale(InventorySale sale) {
        if (sale.organization == null || sale.organization.id == null) {
            throw new IllegalStateException("Sale requires a persisted organization before canonical reference synchronization.");
        }
        if (sale.buyer == null || sale.buyer.id == null) {
            throw new IllegalStateException("Sale requires a persisted buyer before canonical reference synchronization.");
        }

        sale.organizationCanonicalId = require(
            MasterDataReferenceService.ORGANIZATION,
            sale.organization.id,
            "sale organization"
        );
        sale.unitCanonicalId = optional(
            MasterDataReferenceService.UNIT,
            sale.unit == null ? null : sale.unit.id,
            "sale unit"
        );
        sale.buyerCanonicalId = require(
            MasterDataReferenceService.PERSON,
            sale.buyer.id,
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
        if (transfer.organization == null || transfer.organization.id == null) {
            throw new IllegalStateException(
                "Inventory transfer requires a persisted organization before canonical reference synchronization."
            );
        }
        if (transfer.sourceUnit == null || transfer.sourceUnit.id == null) {
            throw new IllegalStateException(
                "Inventory transfer requires a persisted source unit before canonical reference synchronization."
            );
        }
        if (transfer.destinationUnit == null || transfer.destinationUnit.id == null) {
            throw new IllegalStateException(
                "Inventory transfer requires a persisted destination unit before canonical reference synchronization."
            );
        }

        transfer.organizationCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.ORGANIZATION,
                transfer.organization.id)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical transfer organization reference is missing for legacy id "
                    + transfer.organization.id
            ));

        transfer.sourceUnitCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.UNIT,
                transfer.sourceUnit.id)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical source unit reference is missing for legacy id "
                    + transfer.sourceUnit.id
            ));

        transfer.destinationUnitCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.UNIT,
                transfer.destinationUnit.id)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical destination unit reference is missing for legacy id "
                    + transfer.destinationUnit.id
            ));
    }

    private void synchronizeReceiving(EquipmentReceiving receiving) {
        if (receiving.receivingOrganization == null) {
            receiving.receivingOrganizationCanonicalId = null;
            return;
        }
        if (receiving.receivingOrganization.id == null) {
            throw new IllegalStateException(
                "Receiving organization must be persisted before canonical reference synchronization."
            );
        }

        receiving.receivingOrganizationCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.ORGANIZATION,
                receiving.receivingOrganization.id)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical receiving organization reference is missing for legacy id "
                    + receiving.receivingOrganization.id
            ));
    }

    private void synchronizeLocation(StockLocation location) {
        if (location.organization == null || location.organization.id == null) {
            throw new IllegalStateException(
                "Stock location requires a persisted organization before canonical reference synchronization."
            );
        }

        location.organizationCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.ORGANIZATION,
                location.organization.id)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical organization reference is missing for legacy id "
                    + location.organization.id
            ));

        if (location.unit == null) {
            location.unitCanonicalId = null;
            return;
        }

        if (location.unit.id == null) {
            throw new IllegalStateException(
                "Stock location unit must be persisted before canonical reference synchronization."
            );
        }

        location.unitCanonicalId = references.resolveCanonicalId(
                MasterDataReferenceService.UNIT,
                location.unit.id)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical organizational unit reference is missing for legacy id "
                    + location.unit.id
            ));
    }
}
