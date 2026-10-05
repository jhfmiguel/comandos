package com.comandos.core.service;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.StockLocation;
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
        return false;
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
