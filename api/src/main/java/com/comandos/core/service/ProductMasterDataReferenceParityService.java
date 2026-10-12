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
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(EquipmentSet.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(EquipmentSetOperation.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(CertificationRecord.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(ExpirationRecord.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(Recall.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(Purchase.class, List.of(
            ref("buyerOrganizationLegacyId", "buyerOrganizationCanonicalId", "ORGANIZATION"),
            ref("supplierOrganizationLegacyId", "supplierOrganizationCanonicalId", "ORGANIZATION"),
            ref("originPersonLegacyId", "originPersonCanonicalId", "PERSON")
        )),
        new Spec(EquipmentReceiving.class, List.of(
            ref("receivingOrganizationLegacyId", "receivingOrganizationCanonicalId", "ORGANIZATION")
        )),
        new Spec(ProcurementProcess.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION")
        )),
        new Spec(Custody.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT"),
            ref("recipientLegacyId", "recipientCanonicalId", "PERSON"),
            ref("recipientUnitLegacyId", "recipientUnitCanonicalId", "UNIT"),
            ref("authorizerLegacyId", "authorizerCanonicalId", "PERSON")
        )),
        new Spec(CustodyResponsibility.class, List.of(
            ref("responsiblePersonLegacyId", "responsiblePersonCanonicalId", "PERSON")
        )),
        new Spec(Donation.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT"),
            ref("donorLegacyId", "donorCanonicalId", "PERSON"),
            ref("doneeLegacyId", "doneeCanonicalId", "PERSON")
        )),
        new Spec(InventorySale.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT"),
            ref("buyerLegacyId", "buyerCanonicalId", "PERSON")
        )),
        new Spec(InventoryTransfer.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("sourceUnitLegacyId", "sourceUnitCanonicalId", "UNIT"),
            ref("destinationUnitLegacyId", "destinationUnitCanonicalId", "UNIT")
        )),
        new Spec(MaintenancePlan.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(WorkOrder.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(ApprovalWorkflow.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(AmmunitionConsumption.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT"),
            ref("responsibleLegacyId", "responsibleCanonicalId", "PERSON"),
            ref("authorizerLegacyId", "authorizerCanonicalId", "PERSON")
        )),
        new Spec(ConsumableUsage.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT"),
            ref("responsibleLegacyId", "responsibleCanonicalId", "PERSON"),
            ref("authorizerLegacyId", "authorizerCanonicalId", "PERSON")
        )),
        new Spec(DisposalProcess.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(InventoryReservation.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(InventoryCount.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(PurchasePlanning.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION")
        )),
        new Spec(PeriodicInspection.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        )),
        new Spec(ExceptionOccurrence.class, List.of(
            ref("organizationLegacyId", "organizationCanonicalId", "ORGANIZATION"),
            ref("unitLegacyId", "unitCanonicalId", "UNIT")
        ))
    );

    private final EntityManager em;
    private final ProductCanonicalScopeResolver canonicalScope;

    public ProductMasterDataReferenceParityService(
            EntityManager em,
            ProductCanonicalScopeResolver canonicalScope) {
        this.em = em;
        this.canonicalScope = canonicalScope;
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

        String expectedId;
        try {
            expectedId = switch (ref.resourceType()) {
                case "ORGANIZATION" -> canonicalScope.organization(legacyId);
                case "PERSON" -> canonicalScope.person(legacyId);
                case "UNIT" -> canonicalScope.unit(legacyId);
                default -> throw new IllegalArgumentException(
                    "Unsupported canonical master-data reference: " + ref.resourceType()
                );
            };
        } catch (IllegalStateException missingCanonical) {
            mismatches.add(new Mismatch(
                entity.getClass().getSimpleName(),
                entity.id,
                ref.canonicalField(),
                ref.resourceType(),
                legacyId,
                null,
                actual,
                "Canonical master-data entity cannot be resolved: " + missingCanonical.getMessage()
            ));
            return;
        }

        // Canonical IDs are the authoritative expected values. An absent
        // shadow value remains a mismatch even when the canonical entity exists.
        if (!expectedId.equals(actual)) {
            mismatches.add(new Mismatch(
                entity.getClass().getSimpleName(),
                entity.id,
                ref.canonicalField(),
                ref.resourceType(),
                legacyId,
                expectedId,
                actual,
                "Product canonical reference differs from the canonical directory."
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
