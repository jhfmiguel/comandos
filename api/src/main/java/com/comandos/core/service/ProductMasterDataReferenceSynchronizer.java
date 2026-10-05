package com.comandos.core.service;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.StockLocation;
import com.comandos.purchase.model.EquipmentReceiving;
import com.comandos.purchase.model.Purchase;
import com.comandos.transfer.model.InventoryTransfer;
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
