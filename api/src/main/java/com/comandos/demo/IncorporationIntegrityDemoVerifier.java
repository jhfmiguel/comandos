package com.comandos.demo;

import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.StockLot;
import com.comandos.purchase.model.EquipmentReceivingItem;
import com.comandos.purchase.model.ReceivingIncorporation;
import com.comandos.purchase.model.ReceivingStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1950)
public class IncorporationIntegrityDemoVerifier implements ApplicationRunner {

    private final EntityManager em;

    public IncorporationIntegrityDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        List<ReceivingIncorporation> rows = em.createQuery(
            "select i from ReceivingIncorporation i order by i.id",
            ReceivingIncorporation.class
        ).getResultList();

        if (rows.isEmpty()) {
            throw new IllegalStateException("Incorporation integration flow produced no incorporation records.");
        }

        Map<Long, BigDecimal> incorporatedByItem = new HashMap<>();
        Set<String> inventoryDestinations = new HashSet<>();
        Set<Long> incorporatedSerials = new HashSet<>();

        for (ReceivingIncorporation row : rows) {
            if (row.receiving == null || row.receivingItem == null) {
                throw new IllegalStateException("Incorporation has no receiving provenance: " + row.id);
            }
            if (row.receivingItem.receiving == null
                || !row.receiving.id.equals(row.receivingItem.receiving.id)) {
                throw new IllegalStateException(
                    "Incorporation item does not belong to its receiving: " + row.id
                );
            }
            if (row.receiving.status != ReceivingStatus.DEFINITIVELY_ACCEPTED
                && row.receiving.status != ReceivingStatus.DEFINITIVELY_PARTIALLY_ACCEPTED) {
                throw new IllegalStateException(
                    "Incorporation originated from a receiving that was not definitively accepted: " + row.id
                );
            }

            BigDecimal quantity = nz(row.quantity);
            if (quantity.signum() <= 0) {
                throw new IllegalStateException("Incorporation quantity must be positive: " + row.id);
            }

            incorporatedByItem.merge(row.receivingItem.id, quantity, BigDecimal::add);

            if (row.receivingSerial != null) {
                if (row.receivingSerial.receivingItem == null
                    || !row.receivingItem.id.equals(row.receivingSerial.receivingItem.id)) {
                    throw new IllegalStateException(
                        "Incorporation serial does not belong to receiving item: " + row.id
                    );
                }
                if (!Boolean.TRUE.equals(row.receivingSerial.accepted)) {
                    throw new IllegalStateException("Rejected serial was incorporated: " + row.id);
                }
                if (!incorporatedSerials.add(row.receivingSerial.id)) {
                    throw new IllegalStateException(
                        "Receiving serial was incorporated more than once: " + row.receivingSerial.id
                    );
                }
            }

            if (row.inventoryResource == null || row.inventoryRecordId == null) {
                throw new IllegalStateException("Incorporation has no inventory destination: " + row.id);
            }
            String destination = row.inventoryResource + ":" + row.inventoryRecordId;
            if (!inventoryDestinations.add(destination)) {
                throw new IllegalStateException(
                    "Inventory destination is linked to more than one incorporation: " + destination
                );
            }

            switch (row.inventoryResource) {
                case "assets" -> {
                    if (em.find(AssetItem.class, row.inventoryRecordId) == null) {
                        throw new IllegalStateException(
                            "Incorporation asset record does not exist: " + row.inventoryRecordId
                        );
                    }
                }
                case "lots" -> {
                    if (em.find(StockLot.class, row.inventoryRecordId) == null) {
                        throw new IllegalStateException(
                            "Incorporation lot record does not exist: " + row.inventoryRecordId
                        );
                    }
                }
                default -> throw new IllegalStateException(
                    "Unsupported incorporation inventory resource: " + row.inventoryResource
                );
            }
        }

        for (Map.Entry<Long, BigDecimal> entry : incorporatedByItem.entrySet()) {
            EquipmentReceivingItem item = em.find(EquipmentReceivingItem.class, entry.getKey());
            if (item == null) {
                throw new IllegalStateException(
                    "Incorporation references missing receiving item: " + entry.getKey()
                );
            }
            BigDecimal accepted = nz(item.acceptedQuantity);
            if (entry.getValue().compareTo(accepted) > 0) {
                throw new IllegalStateException(
                    "Incorporated quantity exceeds accepted quantity for receiving item " + item.id
                );
            }
        }
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
