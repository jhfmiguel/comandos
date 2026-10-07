package com.comandos.core.service;

import com.comandos.consumption.model.AmmunitionConsumption;
import com.comandos.consumption.model.ConsumableUsage;
import com.comandos.core.model.CoreEntity;
import com.comandos.custody.model.Custody;
import com.comandos.custody.model.CustodyResponsibility;
import com.comandos.disposal.model.DisposalProcess;
import com.comandos.donation.model.Donation;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.EquipmentSet;
import com.comandos.inventory.model.EquipmentSetOperation;
import com.comandos.inventory.model.CertificationRecord;
import com.comandos.inventory.model.ExpirationRecord;
import com.comandos.inventory.model.Recall;
import com.comandos.lifecycle.model.ExceptionOccurrence;
import com.comandos.lifecycle.model.PeriodicInspection;
import com.comandos.maintenance.model.WorkOrder;
import com.comandos.maintenance.model.MaintenancePlan;
import com.comandos.purchase.model.EquipmentReceiving;
import com.comandos.purchase.model.Purchase;
import com.comandos.purchase.model.PurchasePlanning;
import com.comandos.purchase.model.ProcurementProcess;
import com.comandos.reconciliation.model.InventoryCount;
import com.comandos.reservation.model.InventoryReservation;
import com.comandos.sales.model.InventorySale;
import com.comandos.transfer.model.InventoryTransfer;
import com.comandos.workflow.model.ApprovalWorkflow;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backfills and validates canonical master-data shadow identifiers in product tables.
 *
 * <p>The legacy numeric associations remain authoritative during this phase; the
 * canonical identifiers are populated side-by-side so the next cutover can switch
 * references without losing traceability.</p>
 */
@Service
public class ProductMasterDataReferenceBackfillService {

    public record ReadinessReport(
        boolean ready,
        long checked,
        long incomplete,
        Map<String, Long> incompleteByEntity
    ) {}

    public record BackfillReport(
        long scanned,
        long synchronized,
        Map<String, Long> synchronizedByEntity
    ) {}

    private record Spec(
        Class<? extends CoreEntity> type,
        String incompleteWhere
    ) {}

    private static final List<Spec> SPECS = List.of(
        new Spec(StockLocation.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(EquipmentSet.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(EquipmentSetOperation.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(CertificationRecord.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(ExpirationRecord.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(Recall.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(Purchase.class,
            "e.buyerOrganizationCanonicalId is null or e.originPersonCanonicalId is null or "
                + "(e.supplierOrganizationLegacyId is not null and e.supplierOrganizationCanonicalId is null) or "
                + "(e.supplierOrganizationLegacyId is null and e.supplierOrganizationCanonicalId is not null)"),
        new Spec(EquipmentReceiving.class,
            "(e.receivingOrganizationLegacyId is not null and e.receivingOrganizationCanonicalId is null) or "
                + "(e.receivingOrganizationLegacyId is null and e.receivingOrganizationCanonicalId is not null)"),
        new Spec(ProcurementProcess.class,
            "e.organizationCanonicalId is null"),
        new Spec(InventoryTransfer.class,
            "e.organizationCanonicalId is null or e.sourceUnitCanonicalId is null or e.destinationUnitCanonicalId is null"),
        new Spec(Custody.class,
            "e.organizationCanonicalId is null or e.authorizerCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null) or "
                + "(e.recipientLegacyId is not null and e.recipientCanonicalId is null) or "
                + "(e.recipientLegacyId is null and e.recipientCanonicalId is not null) or "
                + "(e.recipientUnitLegacyId is not null and e.recipientUnitCanonicalId is null) or "
                + "(e.recipientUnitLegacyId is null and e.recipientUnitCanonicalId is not null)"),
        new Spec(CustodyResponsibility.class,
            "e.responsiblePersonCanonicalId is null"),
        new Spec(Donation.class,
            "e.organizationCanonicalId is null or e.donorCanonicalId is null or e.doneeCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(InventorySale.class,
            "e.organizationCanonicalId is null or e.buyerCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(MaintenancePlan.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(WorkOrder.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(ApprovalWorkflow.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(DisposalProcess.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(InventoryReservation.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(InventoryCount.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(AmmunitionConsumption.class,
            "e.organizationCanonicalId is null or e.responsibleCanonicalId is null or e.authorizerCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(ConsumableUsage.class,
            "e.organizationCanonicalId is null or e.responsibleCanonicalId is null or e.authorizerCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(PurchasePlanning.class,
            "e.organizationCanonicalId is null"),
        new Spec(PeriodicInspection.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)"),
        new Spec(ExceptionOccurrence.class,
            "e.organizationCanonicalId is null or "
                + "(e.unitLegacyId is not null and e.unitCanonicalId is null) or "
                + "(e.unitLegacyId is null and e.unitCanonicalId is not null)")
    );

    private final EntityManager em;
    private final ProductMasterDataReferenceSynchronizer synchronizer;

    public ProductMasterDataReferenceBackfillService(
            EntityManager em,
            ProductMasterDataReferenceSynchronizer synchronizer) {
        this.em = em;
        this.synchronizer = synchronizer;
    }

    @Transactional(readOnly = true)
    public ReadinessReport readiness() {
        long checked = 0;
        long incomplete = 0;
        Map<String, Long> incompleteByEntity = new LinkedHashMap<>();

        for (Spec spec : SPECS) {
            long rows = count(spec.type(), null);
            long missing = count(spec.type(), spec.incompleteWhere());
            checked += rows;
            incomplete += missing;
            if (missing > 0) {
                incompleteByEntity.put(spec.type().getSimpleName(), missing);
            }
        }

        return new ReadinessReport(
            incomplete == 0,
            checked,
            incomplete,
            Map.copyOf(incompleteByEntity)
        );
    }

    @Transactional
    public BackfillReport backfill() {
        long scanned = 0;
        long synchronizedCount = 0;
        Map<String, Long> synchronizedByEntity = new LinkedHashMap<>();

        for (Spec spec : SPECS) {
            long entityCount = 0;
            List<? extends CoreEntity> rows = all(spec.type());
            scanned += rows.size();

            for (CoreEntity entity : rows) {
                if (synchronizer.synchronizeForBackfill(entity)) {
                    synchronizedCount++;
                    entityCount++;
                }
            }

            if (entityCount > 0) {
                synchronizedByEntity.put(spec.type().getSimpleName(), entityCount);
            }
        }

        em.flush();

        ReadinessReport readiness = readiness();
        if (!readiness.ready()) {
            throw new IllegalStateException(
                "Canonical product-reference backfill completed with incomplete shadows: "
                    + readiness.incompleteByEntity()
            );
        }

        return new BackfillReport(
            scanned,
            synchronizedCount,
            Map.copyOf(synchronizedByEntity)
        );
    }

    private List<? extends CoreEntity> all(Class<? extends CoreEntity> type) {
        return em.createQuery(
                "select e from " + type.getSimpleName() + " e order by e.id",
                type)
            .getResultList();
    }

    private long count(Class<? extends CoreEntity> type, String where) {
        return em.createQuery(
                "select count(e) from " + type.getSimpleName() + " e"
                    + (where == null ? "" : " where " + where),
                Long.class)
            .getSingleResult();
    }
}
