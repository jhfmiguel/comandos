package com.comandos.demo;

import com.comandos.core.model.Organization;
import com.comandos.core.model.Person;
import com.comandos.inventory.model.ItemModel;
import com.comandos.inventory.model.StockLocation;
import com.comandos.purchase.dto.EquipmentReceivingContract;
import com.comandos.purchase.dto.PurchaseContract;
import com.comandos.purchase.dto.ReceivingIncorporationContract;
import com.comandos.purchase.dto.ReceivingInspectionContract;
import com.comandos.purchase.model.AcquisitionType;
import com.comandos.purchase.model.PurchaseStatus;
import com.comandos.purchase.model.ReceivingSourceType;
import com.comandos.purchase.model.ReceivingStatus;
import com.comandos.purchase.service.EquipmentReceivingService;
import com.comandos.purchase.service.PurchaseService;
import com.comandos.purchase.service.ReceivingIncorporationService;
import com.comandos.purchase.service.ReceivingInspectionService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1800)
public class ReceivingIntegrationDemoVerifier implements ApplicationRunner {

    private static final String PURCHASE_NUMBER = "REC-FLOW-DEMO-2026-001";

    private final EntityManager em;
    private final PurchaseService purchases;
    private final EquipmentReceivingService receivings;
    private final ReceivingInspectionService inspections;
    private final ReceivingIncorporationService incorporations;

    public ReceivingIntegrationDemoVerifier(
        EntityManager em,
        PurchaseService purchases,
        EquipmentReceivingService receivings,
        ReceivingInspectionService inspections,
        ReceivingIncorporationService incorporations
    ) {
        this.em = em;
        this.purchases = purchases;
        this.receivings = receivings;
        this.inspections = inspections;
        this.incorporations = incorporations;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (purchaseExists()) return;

        Organization buyer = required(Organization.class, "acronym", "SSP-DEMO");
        Person origin = required(Person.class, "taxId", "22222222222");
        ItemModel model = required(ItemModel.class, "sku", "CBC-9-LUGER-FMJ");
        StockLocation location = required(StockLocation.class, "code", "ARM-COFRE-01");

        if (model.category == null
            || (!Boolean.TRUE.equals(model.category.consumable)
                && !Boolean.TRUE.equals(model.category.lotControlled))) {
            throw new IllegalStateException("Receiving integration demo requires a consumable/lot-controlled model.");
        }

        PurchaseContract.PurchaseView acquisition = purchases.create(new PurchaseContract.CreatePurchaseRequest(
            buyer.id,
            null,
            origin.id,
            AcquisitionType.FREE,
            "Integrated validation of multiple, partial and replacement receivings.",
            PURCHASE_NUMBER,
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            "Two deliveries with rejection replacement.",
            null,
            "Oracle receiving integration scenario.",
            List.of(new PurchaseContract.CreatePurchaseItemRequest(
                model.id,
                new BigDecimal("100"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "New material",
                "Receiving integration item"
            )),
            List.of()
        ));
        acquisition = purchases.authorize(acquisition.id());
        require(acquisition.status() == PurchaseStatus.AUTHORIZED, "Acquisition was not authorized.");

        Long acquisitionId = acquisition.id();
        Long acquisitionItemId = acquisition.items().getFirst().id();

        EquipmentReceivingContract.View first = receivings.create(receivingRequest(
            acquisitionId, buyer.id, acquisitionItemId, model.id, "40", "REC-FLOW-A", "ROM-REC-001"
        ));
        require(first.status() == ReceivingStatus.PARTIALLY_RECEIVED,
            "First delivery must leave acquisition partially received.");
        require(purchases.get(acquisitionId).status() == PurchaseStatus.PARTIALLY_RECEIVED,
            "Acquisition must be partially received after first delivery.");

        Long firstReceivingId = first.id();
        Long firstItemId = first.items().getFirst().id();
        inspections.create(firstReceivingId, inspectionRequest(firstItemId, "30", "10",
            "Ten units rejected due to delivery divergence."));
        first = receivings.get(firstReceivingId);
        require(first.status() == ReceivingStatus.DEFINITIVELY_PARTIALLY_ACCEPTED,
            "First receiving must be definitively partially accepted.");
        require(first.items().getFirst().acceptedQuantity().compareTo(new BigDecimal("30")) == 0,
            "First receiving accepted quantity mismatch.");
        require(first.items().getFirst().rejectedQuantity().compareTo(new BigDecimal("10")) == 0,
            "First receiving rejected quantity mismatch.");
        require(purchases.get(acquisitionId).items().getFirst().receivedQuantity()
                .compareTo(new BigDecimal("30")) == 0,
            "Rejected quantity must reopen acquisition pending balance.");

        EquipmentReceivingContract.View second = receivings.create(receivingRequest(
            acquisitionId, buyer.id, acquisitionItemId, model.id, "70", "REC-FLOW-B", "ROM-REC-002"
        ));
        require(second.status() == ReceivingStatus.RECEIVED,
            "Second delivery must complete acquisition quantity before inspection.");

        Long secondReceivingId = second.id();
        Long secondItemId = second.items().getFirst().id();
        inspections.create(secondReceivingId, inspectionRequest(secondItemId, "70", "0", null));
        second = receivings.get(secondReceivingId);
        require(second.status() == ReceivingStatus.DEFINITIVELY_ACCEPTED,
            "Second receiving must be definitively accepted.");

        PurchaseContract.PurchaseView completed = purchases.get(acquisitionId);
        require(completed.status() == PurchaseStatus.RECEIVED,
            "Acquisition must be fully received after replacement delivery.");
        require(completed.items().getFirst().receivedQuantity().compareTo(new BigDecimal("100")) == 0,
            "Acquisition fulfilled quantity must equal acquired quantity.");

        incorporations.create(incorporationRequest(firstReceivingId, firstItemId, location.id, "REC-FLOW-A", "30"));
        incorporations.create(incorporationRequest(secondReceivingId, secondItemId, location.id, "REC-FLOW-B", "70"));

        List<EquipmentReceivingContract.View> linked = receivings.list(acquisitionId);
        require(linked.size() == 2, "Acquisition must expose exactly two linked receivings in integration scenario.");
        for (EquipmentReceivingContract.View receiving : linked) {
            require(acquisitionId.equals(receiving.acquisitionId()), "Receiving lost acquisition link.");
            require(receiving.items().size() == 1, "Receiving integration scenario must keep one linked item.");
            require(acquisitionItemId.equals(receiving.items().getFirst().acquisitionItemId()),
                "Receiving item lost acquisition-item link.");
        }

        BigDecimal incorporated = incorporations.list(null).stream()
            .filter(row -> firstReceivingId.equals(row.receivingId()) || secondReceivingId.equals(row.receivingId()))
            .map(ReceivingIncorporationContract.View::quantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        require(incorporated.compareTo(new BigDecimal("100")) == 0,
            "Integrated receiving incorporation quantity must total 100.");

        boolean extraReceivingBlocked = false;
        try {
            receivings.create(receivingRequest(
                acquisitionId, buyer.id, acquisitionItemId, model.id, "1", "REC-FLOW-C", "ROM-REC-003"
            ));
        } catch (IllegalStateException | IllegalArgumentException expected) {
            extraReceivingBlocked = true;
        }
        require(extraReceivingBlocked, "Receiving beyond completed acquisition quantity must be blocked.");
    }

    private EquipmentReceivingContract.CreateRequest receivingRequest(
        Long acquisitionId,
        Long organizationId,
        Long acquisitionItemId,
        Long modelId,
        String quantity,
        String lot,
        String deliveryDocument
    ) {
        return new EquipmentReceivingContract.CreateRequest(
            ReceivingSourceType.ACQUISITION,
            acquisitionId,
            organizationId,
            "Gerência de Armamento",
            "Cofre Central",
            deliveryDocument,
            null,
            null,
            "recebimento.integracao.demo",
            true,
            true,
            "Multiple receiving integration validation.",
            List.of(new EquipmentReceivingContract.ItemRequest(
                acquisitionItemId,
                modelId,
                null,
                new BigDecimal(quantity),
                lot,
                null,
                null,
                "Material presented for inspection.",
                null,
                List.of()
            ))
        );
    }

    private ReceivingInspectionContract.CreateRequest inspectionRequest(
        Long receivingItemId,
        String accepted,
        String rejected,
        String divergence
    ) {
        return new ReceivingInspectionContract.CreateRequest(
            null,
            "inspecao.integracao.demo",
            false,
            true,
            new BigDecimal(rejected).signum() == 0,
            divergence,
            "Definitive integrated inspection.",
            List.of(new ReceivingInspectionContract.ItemDecision(
                receivingItemId,
                new BigDecimal(accepted),
                new BigDecimal(rejected),
                divergence,
                List.of()
            ))
        );
    }

    private ReceivingIncorporationContract.CreateRequest incorporationRequest(
        Long receivingId,
        Long receivingItemId,
        Long locationId,
        String lot,
        String quantity
    ) {
        return new ReceivingIncorporationContract.CreateRequest(
            receivingId,
            receivingItemId,
            null,
            locationId,
            null,
            lot,
            new BigDecimal(quantity),
            BigDecimal.ZERO,
            "NEW",
            "incorporacao.integracao.demo",
            null,
            "Integrated receiving incorporation."
        );
    }

    private boolean purchaseExists() {
        return !em.createQuery(
            "select p.id from Purchase p where p.purchaseNumber = :number", Long.class)
            .setParameter("number", PURCHASE_NUMBER)
            .setMaxResults(1)
            .getResultList()
            .isEmpty();
    }

    private <T> T required(Class<T> type, String field, Object value) {
        return em.createQuery("select e from " + type.getSimpleName() + " e where e." + field + " = :value", type)
            .setParameter("value", value)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Required demo reference not found: " + type.getSimpleName() + "." + field + "=" + value
            ));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
