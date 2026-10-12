package com.comandos.core.service;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.EquipmentSet;
import com.comandos.inventory.model.EquipmentSetOperation;
import com.comandos.inventory.model.CertificationRecord;
import com.comandos.inventory.model.ExpirationRecord;
import com.comandos.inventory.model.Recall;
import com.comandos.purchase.model.EquipmentReceiving;
import com.comandos.purchase.model.Purchase;
import com.comandos.transfer.model.InventoryTransfer;
import com.comandos.custody.model.Custody;
import com.comandos.custody.model.CustodyResponsibility;
import com.comandos.donation.model.Donation;
import com.comandos.sales.model.InventorySale;
import com.comandos.maintenance.model.WorkOrder;
import com.comandos.maintenance.model.MaintenancePlan;
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

    private static final String ORGANIZATION = "ORGANIZATION";
    private static final String PERSON = "PERSON";
    private static final String UNIT = "UNIT";

    private final ProductCanonicalScopeResolver canonicalScope;
    private final boolean enabled;

    public ProductMasterDataReferenceSynchronizer(
            ProductCanonicalScopeResolver canonicalScope,
            @Value("${comandos.master-data.product-reference-shadow.enabled:false}")
            boolean enabled) {
        this.canonicalScope = canonicalScope;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean synchronizeForBackfill(CoreEntity entity) {
        return synchronizeNow(entity);
    }

    private boolean synchronizeNow(CoreEntity entity) {
        if (entity instanceof StockLocation location) {
            synchronizeLocation(location);
            return true;
        }
        if (entity instanceof EquipmentSet equipmentSet) {
            equipmentSet.organizationCanonicalId = require(
                ORGANIZATION,
                equipmentSet.organizationLegacyId,
                "equipment set organization"
            );
            equipmentSet.unitCanonicalId = optional(
                UNIT,
                equipmentSet.unitLegacyId,
                "equipment set unit"
            );
            return true;
        }
        if (entity instanceof EquipmentSetOperation operation) {
            operation.organizationCanonicalId = require(
                ORGANIZATION,
                operation.organizationLegacyId,
                "equipment set operation organization"
            );
            operation.unitCanonicalId = optional(
                UNIT,
                operation.unitLegacyId,
                "equipment set operation unit"
            );
            return true;
        }
        if (entity instanceof CertificationRecord certification) {
            certification.organizationCanonicalId = require(
                ORGANIZATION,
                certification.organizationLegacyId,
                "certification organization"
            );
            certification.unitCanonicalId = optional(
                UNIT,
                certification.unitLegacyId,
                "certification unit"
            );
            return true;
        }
        if (entity instanceof ExpirationRecord expiration) {
            expiration.organizationCanonicalId = require(
                ORGANIZATION,
                expiration.organizationLegacyId,
                "expiration organization"
            );
            expiration.unitCanonicalId = optional(
                UNIT,
                expiration.unitLegacyId,
                "expiration unit"
            );
            return true;
        }
        if (entity instanceof Recall recall) {
            recall.organizationCanonicalId = require(
                ORGANIZATION,
                recall.organizationLegacyId,
                "recall organization"
            );
            recall.unitCanonicalId = optional(
                UNIT,
                recall.unitLegacyId,
                "recall unit"
            );
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
                ORGANIZATION,
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
        if (entity instanceof CustodyResponsibility responsibility) {
            responsibility.responsiblePersonCanonicalId = require(
                PERSON,
                responsibility.responsiblePersonLegacyId,
                "custody responsibility person"
            );
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
        if (entity instanceof MaintenancePlan maintenancePlan) {
            maintenancePlan.organizationCanonicalId = require(
                ORGANIZATION,
                maintenancePlan.organizationLegacyId,
                "maintenance plan organization"
            );
            maintenancePlan.unitCanonicalId = optional(
                UNIT,
                maintenancePlan.unitLegacyId,
                "maintenance plan unit"
            );
            return true;
        }
        if (entity instanceof WorkOrder workOrder) {
            workOrder.organizationCanonicalId = require(
                ORGANIZATION,
                workOrder.organizationLegacyId,
                "work order organization"
            );
            workOrder.unitCanonicalId = optional(
                UNIT,
                workOrder.unitLegacyId,
                "work order unit"
            );
            return true;
        }
        if (entity instanceof ApprovalWorkflow workflow) {
            workflow.organizationCanonicalId = require(
                ORGANIZATION,
                workflow.organizationLegacyId,
                "approval workflow organization"
            );
            workflow.unitCanonicalId = optional(
                UNIT,
                workflow.unitLegacyId,
                "approval workflow unit"
            );
            return true;
        }
        if (entity instanceof DisposalProcess disposal) {
            disposal.organizationCanonicalId = require(
                ORGANIZATION,
                disposal.organizationLegacyId,
                "disposal organization"
            );
            disposal.unitCanonicalId = optional(
                UNIT,
                disposal.unitLegacyId,
                "disposal unit"
            );
            return true;
        }
        if (entity instanceof InventoryReservation reservation) {
            reservation.organizationCanonicalId = require(
                ORGANIZATION,
                reservation.organizationLegacyId,
                "inventory reservation organization"
            );
            reservation.unitCanonicalId = optional(
                UNIT,
                reservation.unitLegacyId,
                "inventory reservation unit"
            );
            return true;
        }
        if (entity instanceof InventoryCount count) {
            count.organizationCanonicalId = require(
                ORGANIZATION,
                count.organizationLegacyId,
                "inventory count organization"
            );
            count.unitCanonicalId = optional(
                UNIT,
                count.unitLegacyId,
                "inventory count unit"
            );
            return true;
        }
        if (entity instanceof AmmunitionConsumption consumption) {
            consumption.organizationCanonicalId = require(
                ORGANIZATION,
                consumption.organizationLegacyId,
                "ammunition consumption organization"
            );
            consumption.unitCanonicalId = optional(
                UNIT,
                consumption.unitLegacyId,
                "ammunition consumption unit"
            );
            consumption.responsibleCanonicalId = require(
                PERSON,
                consumption.responsibleLegacyId,
                "ammunition consumption responsible"
            );
            consumption.authorizerCanonicalId = require(
                PERSON,
                consumption.authorizerLegacyId,
                "ammunition consumption authorizer"
            );
            return true;
        }
        if (entity instanceof ConsumableUsage usage) {
            usage.organizationCanonicalId = require(
                ORGANIZATION,
                usage.organizationLegacyId,
                "consumable usage organization"
            );
            usage.unitCanonicalId = optional(
                UNIT,
                usage.unitLegacyId,
                "consumable usage unit"
            );
            usage.responsibleCanonicalId = require(
                PERSON,
                usage.responsibleLegacyId,
                "consumable usage responsible"
            );
            usage.authorizerCanonicalId = require(
                PERSON,
                usage.authorizerLegacyId,
                "consumable usage authorizer"
            );
            return true;
        }
        if (entity instanceof PurchasePlanning planning) {
            planning.organizationCanonicalId = require(
                ORGANIZATION,
                planning.organizationLegacyId,
                "purchase planning organization"
            );
            return true;
        }
        if (entity instanceof PeriodicInspection inspection) {
            inspection.organizationCanonicalId = require(
                ORGANIZATION,
                inspection.organizationLegacyId,
                "periodic inspection organization"
            );
            inspection.unitCanonicalId = optional(
                UNIT,
                inspection.unitLegacyId,
                "periodic inspection unit"
            );
            return true;
        }
        if (entity instanceof ExceptionOccurrence occurrence) {
            occurrence.organizationCanonicalId = require(
                ORGANIZATION,
                occurrence.organizationLegacyId,
                "exception occurrence organization"
            );
            occurrence.unitCanonicalId = optional(
                UNIT,
                occurrence.unitLegacyId,
                "exception occurrence unit"
            );
            return true;
        }
        return false;
    }

    private void synchronizePurchase(Purchase purchase) {
        purchase.buyerOrganizationCanonicalId = require(
            ORGANIZATION,
            purchase.buyerOrganizationLegacyId,
            "buyer organization"
        );
        purchase.supplierOrganizationCanonicalId = optional(
            ORGANIZATION,
            purchase.supplierOrganizationLegacyId,
            "supplier organization"
        );
        purchase.originPersonCanonicalId = require(
            PERSON,
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
            ORGANIZATION,
            organization.id,
            label + " organization"
        );
        String unitCanonicalId = optional(
            UNIT,
            unit == null ? null : unit.id,
            label + " unit"
        );
        consumer.accept(new ScopeIds(organizationCanonicalId, unitCanonicalId));
    }

    private void synchronizeCustody(Custody custody) {
        custody.organizationCanonicalId = require(
            ORGANIZATION,
            custody.organizationLegacyId,
            "custody organization"
        );
        custody.authorizerCanonicalId = require(
            PERSON,
            custody.authorizerLegacyId,
            "custody authorizer"
        );
        custody.unitCanonicalId = optional(
            UNIT,
            custody.unitLegacyId,
            "custody unit"
        );
        custody.recipientCanonicalId = optional(
            PERSON,
            custody.recipientLegacyId,
            "custody recipient"
        );
        custody.recipientUnitCanonicalId = optional(
            UNIT,
            custody.recipientUnitLegacyId,
            "custody recipient unit"
        );
    }

    private void synchronizeDonation(Donation donation) {

        donation.organizationCanonicalId = require(
            ORGANIZATION,
            donation.organizationLegacyId,
            "donation organization"
        );
        donation.unitCanonicalId = optional(
            UNIT,
            donation.unitLegacyId,
            "donation unit"
        );
        donation.donorCanonicalId = require(
            PERSON,
            donation.donorLegacyId,
            "donation donor"
        );
        donation.doneeCanonicalId = require(
            PERSON,
            donation.doneeLegacyId,
            "donation donee"
        );
    }

    private void synchronizeSale(InventorySale sale) {

        sale.organizationCanonicalId = require(
            ORGANIZATION,
            sale.organizationLegacyId,
            "sale organization"
        );
        sale.unitCanonicalId = optional(
            UNIT,
            sale.unitLegacyId,
            "sale unit"
        );
        sale.buyerCanonicalId = require(
            PERSON,
            sale.buyerLegacyId,
            "sale buyer"
        );
    }

    private String require(String resourceType, Long legacyId, String label) {
        if (legacyId == null) {
            throw new IllegalStateException(label + " must be persisted before canonical reference synchronization.");
        }
        try {
            return switch (resourceType) {
                case ORGANIZATION -> canonicalScope.organization(legacyId);
                case PERSON -> canonicalScope.person(legacyId);
                case UNIT -> canonicalScope.unit(legacyId);
                default -> throw new IllegalArgumentException(
                    "Unsupported product master-data reference type: " + resourceType
                );
            };
        } catch (IllegalStateException exception) {
            throw new IllegalStateException(
                "Canonical " + label + " reference is missing for legacy id " + legacyId,
                exception
            );
        }
    }

    private String optional(String resourceType, Long legacyId, String label) {
        if (legacyId == null) return null;
        return require(resourceType, legacyId, label);
    }

    private void synchronizeTransfer(InventoryTransfer transfer) {
        transfer.organizationCanonicalId = require(
            ORGANIZATION,
            transfer.organizationLegacyId,
            "transfer organization"
        );
        transfer.sourceUnitCanonicalId = require(
            UNIT,
            transfer.sourceUnitLegacyId,
            "transfer source unit"
        );
        transfer.destinationUnitCanonicalId = require(
            UNIT,
            transfer.destinationUnitLegacyId,
            "transfer destination unit"
        );
    }

    private void synchronizeReceiving(EquipmentReceiving receiving) {
        receiving.receivingOrganizationCanonicalId = optional(
            ORGANIZATION,
            receiving.receivingOrganizationLegacyId,
            "receiving organization"
        );
    }

    private void synchronizeLocation(StockLocation location) {
        location.organizationCanonicalId = require(
            ORGANIZATION,
            location.organizationLegacyId,
            "stock location organization"
        );
        location.unitCanonicalId = optional(
            UNIT,
            location.unitLegacyId,
            "stock location unit"
        );
    }
}
