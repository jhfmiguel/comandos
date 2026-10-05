package com.comandos.core.service;

import com.comandos.consumption.model.AmmunitionConsumption;
import com.comandos.consumption.model.ConsumableUsage;
import com.comandos.core.model.CoreEntity;
import com.comandos.custody.model.Custody;
import com.comandos.disposal.model.DisposalProcess;
import com.comandos.donation.model.Donation;
import com.comandos.inventory.model.StockLocation;
import com.comandos.lifecycle.model.ExceptionOccurrence;
import com.comandos.lifecycle.model.PeriodicInspection;
import com.comandos.maintenance.model.WorkOrder;
import com.comandos.purchase.model.EquipmentReceiving;
import com.comandos.purchase.model.Purchase;
import com.comandos.purchase.model.PurchasePlanning;
import com.comandos.reconciliation.model.InventoryCount;
import com.comandos.reservation.model.InventoryReservation;
import com.comandos.sales.model.InventorySale;
import com.comandos.transfer.model.InventoryTransfer;
import com.comandos.workflow.model.ApprovalWorkflow;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that product-domain canonical reference columns exactly match the
 * active legacy-to-canonical crosswalk before legacy foreign keys are retired.
 */
@Service
@Transactional(readOnly = true)
public class ProductMasterDataReferenceParityService {

    public record Mismatch(
        String entity,
        Long entityId,
        String field,
        String resourceType,
        Long legacyId,
        String expectedCanonicalId,
        String actualCanonicalId,
        String detail
    ) {}

    public record ParityReport(
        boolean consistent,
        long checkedReferences,
        List<Mismatch> mismatches
    ) {}

    private record Ref(
        String legacyPath,
        String canonicalField,
        String resourceType
    ) {}

    private record Spec(
        Class<? extends CoreEntity> type,
        List<Ref> refs
    ) {}

    private static Ref ref(String legacyPath, String canonicalField, String resourceType) {
        return new Ref(legacyPath, canonicalField, resourceType);
    }

    private static final List<Spec> SPECS = List.of(
        new Spec(StockLocation.class, List.of(
            ref("organization.id", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unit.id", "unitCanonicalId", MasterDataReferenceService.UNIT)
        )),
        new Spec(Purchase.class, List.of(
            ref("buyerOrganization.id", "buyerOrganizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("supplierOrganization.id", "supplierOrganizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("originPerson.id", "originPersonCanonicalId", MasterDataReferenceService.PERSON)
        )),
        new Spec(EquipmentReceiving.class, List.of(
            ref("receivingOrganizationLegacyId", "receivingOrganizationCanonicalId", MasterDataReferenceService.ORGANIZATION)
        )),
        new Spec(Custody.class, List.of(
            ref("organization.id", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unit.id", "unitCanonicalId", MasterDataReferenceService.UNIT),
            ref("recipient.id", "recipientCanonicalId", MasterDataReferenceService.PERSON),
            ref("recipientUnit.id", "recipientUnitCanonicalId", MasterDataReferenceService.UNIT),
            ref("authorizer.id", "authorizerCanonicalId", MasterDataReferenceService.PERSON)
        )),
        new Spec(Donation.class, List.of(
            ref("organization.id", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unit.id", "unitCanonicalId", MasterDataReferenceService.UNIT),
            ref("donor.id", "donorCanonicalId", MasterDataReferenceService.PERSON),
            ref("donee.id", "doneeCanonicalId", MasterDataReferenceService.PERSON)
        )),
        new Spec(InventorySale.class, List.of(
            ref("organization.id", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unit.id", "unitCanonicalId", MasterDataReferenceService.UNIT),
            ref("buyer.id", "buyerCanonicalId", MasterDataReferenceService.PERSON)
        )),
        new Spec(InventoryTransfer.class, List.of(
            ref("organization.id", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("sourceUnit.id", "sourceUnitCanonicalId", MasterDataReferenceService.UNIT),
            ref("destinationUnit.id", "destinationUnitCanonicalId", MasterDataReferenceService.UNIT)
        )),
        new Spec(WorkOrder.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unitLegacyId", "unitCanonicalId", MasterDataReferenceService.UNIT)
        )),
        new Spec(ApprovalWorkflow.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unitLegacyId", "unitCanonicalId", MasterDataReferenceService.UNIT)
        )),
        new Spec(AmmunitionConsumption.class, List.of(
            ref("organization.id", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unit.id", "unitCanonicalId", MasterDataReferenceService.UNIT),
            ref("responsible.id", "responsibleCanonicalId", MasterDataReferenceService.PERSON),
            ref("authorizer.id", "authorizerCanonicalId", MasterDataReferenceService.PERSON)
        )),
        new Spec(ConsumableUsage.class, List.of(
            ref("organization.id", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unit.id", "unitCanonicalId", MasterDataReferenceService.UNIT),
            ref("responsible.id", "responsibleCanonicalId", MasterDataReferenceService.PERSON),
            ref("authorizer.id", "authorizerCanonicalId", MasterDataReferenceService.PERSON)
        )),
        new Spec(DisposalProcess.class, List.of(
            ref("organization.id", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unit.id", "unitCanonicalId", MasterDataReferenceService.UNIT)
        )),
        new Spec(InventoryReservation.class, List.of(
            ref("organization.id", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unit.id", "unitCanonicalId", MasterDataReferenceService.UNIT)
        )),
        new Spec(InventoryCount.class, List.of(
            ref("organization.id", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unit.id", "unitCanonicalId", MasterDataReferenceService.UNIT)
        )),
        new Spec(PurchasePlanning.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION)
        )),
        new Spec(PeriodicInspection.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unitLegacyId", "unitCanonicalId", MasterDataReferenceService.UNIT)
        )),
        new Spec(ExceptionOccurrence.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", MasterDataReferenceService.ORGANIZATION),
            ref("unitLegacyId", "unitCanonicalId", MasterDataReferenceService.UNIT)
        ))
    );

    private final EntityManager em;
    private final MasterDataReferenceService references;

    public ProductMasterDataReferenceParityService(
            EntityManager em,
            MasterDataReferenceService references) {
        this.em = em;
        this.references = references;
    }

    public ParityReport verify() {
        List<Mismatch> mismatches = new ArrayList<>();
        long checked = 0;

        for (Spec spec : SPECS) {
            for (CoreEntity entity : all(spec.type())) {
                for (Ref ref : spec.refs()) {
                    checked++;
                    verify(entity, ref, mismatches);
                }
            }
        }

        return new ParityReport(
            mismatches.isEmpty(),
            checked,
            List.copyOf(mismatches)
        );
    }

    private void verify(CoreEntity entity, Ref ref, List<Mismatch> mismatches) {
        Long legacyId = (Long) readPath(entity, ref.legacyPath());
        String actual = (String) readField(entity, ref.canonicalField());

        if (legacyId == null) {
            if (actual != null) {
                mismatches.add(new Mismatch(
                    entity.getClass().getSimpleName(),
                    entity.id,
                    ref.canonicalField(),
                    ref.resourceType(),
                    null,
                    null,
                    actual,
                    "Canonical reference must be null when the legacy relation is null."
                ));
            }
            return;
        }

        var expected = references.resolveCanonicalId(ref.resourceType(), legacyId);
        if (expected.isEmpty()) {
            mismatches.add(new Mismatch(
                entity.getClass().getSimpleName(),
                entity.id,
                ref.canonicalField(),
                ref.resourceType(),
                legacyId,
                null,
                actual,
                "Active crosswalk entry is missing."
            ));
            return;
        }

        String expectedId = expected.orElseThrow();
        if (!expectedId.equals(actual)) {
            mismatches.add(new Mismatch(
                entity.getClass().getSimpleName(),
                entity.id,
                ref.canonicalField(),
                ref.resourceType(),
                legacyId,
                expectedId,
                actual,
                "Canonical product reference differs from the active crosswalk."
            ));
        }
    }

    private List<? extends CoreEntity> all(Class<? extends CoreEntity> type) {
        return em.createQuery(
                "select e from " + type.getSimpleName() + " e order by e.id",
                type)
            .getResultList();
    }

    private static Object readPath(Object root, String path) {
        Object current = root;
        for (String field : path.split("\\.")) {
            if (current == null) return null;
            current = readField(current, field);
        }
        return current;
    }

    private static Object readField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getField(fieldName);
            return field.get(target);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                "Unable to inspect canonical reference field "
                    + target.getClass().getName() + "." + fieldName,
                ex
            );
        }
    }
}
