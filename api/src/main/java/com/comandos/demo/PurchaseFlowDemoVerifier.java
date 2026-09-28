package com.comandos.demo;

import com.comandos.inventory.model.ItemModel;
import com.comandos.purchase.model.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1900)
public class PurchaseFlowDemoVerifier implements ApplicationRunner {

    private final EntityManager em;

    public PurchaseFlowDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        List<Purchase> purchases = em.createQuery("select distinct p from Purchase p left join fetch p.items", Purchase.class)
            .getResultList();
        if (purchases.isEmpty()) {
            throw new IllegalStateException("Acquisition demo flow was not seeded.");
        }

        for (Purchase purchase : purchases) {
            verifyProcurement(purchase);
            verifyPurchaseQuantities(purchase);
        }

        List<EquipmentReceiving> receivings = em.createQuery(
            "select r from EquipmentReceiving r where r.sourceType = :source", EquipmentReceiving.class)
            .setParameter("source", ReceivingSourceType.ACQUISITION)
            .getResultList();
        if (receivings.isEmpty()) {
            throw new IllegalStateException("Acquisition demo flow has no receiving.");
        }

        for (EquipmentReceiving receiving : receivings) {
            verifyReceiving(receiving);
        }
    }

    private void verifyProcurement(Purchase purchase) {
        boolean publicOnerous = purchase.buyerOrganization != null
            && Boolean.TRUE.equals(purchase.buyerOrganization.publicOrganization)
            && purchase.acquisitionType == AcquisitionType.ONEROUS;
        if (!publicOnerous) return;

        if (purchase.procurementProcess == null) {
            throw new IllegalStateException("Public onerous acquisition has no procurement process: " + purchase.purchaseNumber);
        }
        if (!Set.of(ProcurementStatus.HOMOLOGATED, ProcurementStatus.CONTRACTED)
            .contains(purchase.procurementProcess.status)) {
            throw new IllegalStateException(
                "Public onerous acquisition is not homologated/contracted: " + purchase.purchaseNumber
            );
        }
    }

    private void verifyPurchaseQuantities(Purchase purchase) {
        if (purchase.items == null || purchase.items.isEmpty()) {
            throw new IllegalStateException("Acquisition has no items: " + purchase.purchaseNumber);
        }
        for (PurchaseItem item : purchase.items) {
            BigDecimal ordered = nz(item.quantity);
            BigDecimal fulfilled = nz(item.receivedQuantity);
            if (ordered.signum() <= 0 || fulfilled.signum() < 0 || fulfilled.compareTo(ordered) > 0) {
                throw new IllegalStateException("Invalid acquisition quantity state for item " + item.id);
            }
        }
    }

    private void verifyReceiving(EquipmentReceiving receiving) {
        if (receiving.acquisition == null) {
            throw new IllegalStateException("Acquisition receiving lost its acquisition reference: " + receiving.id);
        }
        if (!Set.of(
            ReceivingStatus.DEFINITIVELY_ACCEPTED,
            ReceivingStatus.DEFINITIVELY_PARTIALLY_ACCEPTED
        ).contains(receiving.status)) {
            throw new IllegalStateException("Demo acquisition receiving is not definitively accepted: " + receiving.id);
        }

        List<EquipmentReceivingItem> items = em.createQuery(
            "select i from EquipmentReceivingItem i where i.receiving.id = :id order by i.id",
            EquipmentReceivingItem.class)
            .setParameter("id", receiving.id)
            .getResultList();
        if (items.isEmpty()) {
            throw new IllegalStateException("Receiving has no items: " + receiving.id);
        }

        for (EquipmentReceivingItem item : items) {
            BigDecimal received = nz(item.receivedQuantity);
            BigDecimal accepted = nz(item.acceptedQuantity);
            BigDecimal rejected = nz(item.rejectedQuantity);
            if (accepted.add(rejected).compareTo(received) != 0) {
                throw new IllegalStateException("Accepted + rejected differs from received for receiving item " + item.id);
            }

            BigDecimal incorporated = em.createQuery(
                "select coalesce(sum(i.quantity), 0) from ReceivingIncorporation i where i.receivingItem.id = :id",
                BigDecimal.class)
                .setParameter("id", item.id)
                .getSingleResult();
            if (incorporated.compareTo(accepted) != 0) {
                throw new IllegalStateException(
                    "Incorporated quantity differs from accepted quantity for receiving item " + item.id
                );
            }

            verifyIncorporationType(item);
        }
    }

    private void verifyIncorporationType(EquipmentReceivingItem receivingItem) {
        ItemModel model = em.find(ItemModel.class, receivingItem.itemModelId);
        if (model == null || model.category == null) {
            throw new IllegalStateException("Receiving item has no valid item model/category: " + receivingItem.id);
        }

        List<ReceivingIncorporation> incorporations = em.createQuery(
            "select i from ReceivingIncorporation i where i.receivingItem.id = :id order by i.id",
            ReceivingIncorporation.class)
            .setParameter("id", receivingItem.id)
            .getResultList();

        boolean serialized = Boolean.TRUE.equals(model.category.serialized);
        boolean lotBased = Boolean.TRUE.equals(model.category.consumable)
            || Boolean.TRUE.equals(model.category.lotControlled);

        for (ReceivingIncorporation incorporation : incorporations) {
            if (incorporation.receiving == null || !incorporation.receiving.id.equals(receivingItem.receiving.id)) {
                throw new IllegalStateException("Incorporation receiving reference mismatch: " + incorporation.id);
            }
            if (serialized) {
                if (incorporation.receivingSerial == null || !"assets".equals(incorporation.inventoryResource)) {
                    throw new IllegalStateException("Serialized incorporation must create an asset: " + incorporation.id);
                }
            } else if (lotBased) {
                if (incorporation.receivingSerial != null || !"lots".equals(incorporation.inventoryResource)) {
                    throw new IllegalStateException("Lot/consumable incorporation must create stock without serial: " + incorporation.id);
                }
            }
            if (incorporation.inventoryRecordId == null) {
                throw new IllegalStateException("Incorporation has no inventory record: " + incorporation.id);
            }
        }
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
