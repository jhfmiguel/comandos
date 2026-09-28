package com.comandos.demo;

import com.comandos.inventory.model.StockBalance;
import com.comandos.inventory.model.StockLot;
import com.comandos.inventory.model.StockMovement;
import com.comandos.reconciliation.model.InventoryCountItem;
import com.comandos.reservation.model.ReservationItem;
import com.comandos.transfer.model.InventoryTransferItem;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1850)
public class StockOracleRegressionDemoVerifier implements ApplicationRunner {

    private final EntityManager em;

    public StockOracleRegressionDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        verifyBalances();
        verifyOpeningEntries();
        verifyReservations();
        verifyTransfers();
        verifyPhysicalInventory();
        verifyMovementIntegrity();
    }

    private void verifyBalances() {
        List<StockBalance> balances = em.createQuery("select b from StockBalance b", StockBalance.class).getResultList();
        if (balances.isEmpty()) fail("stock balances are missing");

        for (StockBalance balance : balances) {
            if (balance.lot == null || balance.location == null) fail("balance without lot/location");
            requireNonNegative(balance.available, "available balance");
            requireNonNegative(balance.reserved, "reserved balance");
            requireNonNegative(balance.blocked, "blocked balance");

            BigDecimal controlled = value(balance.available).add(value(balance.reserved)).add(value(balance.blocked));
            if (balance.lot.initialQuantity != null && controlled.compareTo(balance.lot.initialQuantity) > 0) {
                fail("balance buckets exceed lot initial quantity: lot=" + balance.lot.id);
            }
        }
    }

    private void verifyOpeningEntries() {
        List<StockLot> lots = em.createQuery("select l from StockLot l", StockLot.class).getResultList();
        if (lots.isEmpty()) fail("stock lots are missing");

        for (StockLot lot : lots) {
            requireNonNegative(lot.initialQuantity, "lot initial quantity");
            requireNonNegative(lot.availableQuantity, "lot available quantity");
            if (lot.openingLocation == null || lot.model == null) fail("lot without opening location/model");

            long opening = em.createQuery(
                    "select count(m) from StockMovement m where m.lot.id = :lotId and m.nature = 'OPENING'",
                    Long.class)
                .setParameter("lotId", lot.id)
                .getSingleResult();
            if (opening == 0) fail("lot without OPENING movement: lot=" + lot.id);
        }
    }

    private void verifyReservations() {
        List<ReservationItem> items = em.createQuery("select i from ReservationItem i", ReservationItem.class).getResultList();
        if (items.isEmpty()) fail("reservation regression data is missing");

        for (ReservationItem item : items) {
            if (item.reservation == null || item.model == null || item.location == null) fail("reservation item linkage is incomplete");
            if (item.quantity == null || item.quantity.signum() <= 0) fail("reservation quantity must be positive");
            if (item.balance != null) {
                requireNonNegative(item.balance.available, "reservation available balance");
                requireNonNegative(item.balance.reserved, "reservation reserved balance");
                if (item.lot != null && !item.balance.lot.id.equals(item.lot.id)) fail("reservation balance points to another lot");
                if (!item.balance.location.id.equals(item.location.id)) fail("reservation balance points to another location");
            }
            if (item.reservation.status == null || item.reservation.requestId == null || item.reservation.requestFingerprint == null) {
                fail("reservation state/idempotency metadata is incomplete");
            }
        }
    }

    private void verifyTransfers() {
        List<InventoryTransferItem> items = em.createQuery("select i from InventoryTransferItem i", InventoryTransferItem.class).getResultList();
        if (items.isEmpty()) fail("transfer regression data is missing");

        for (InventoryTransferItem item : items) {
            if (item.transfer == null || item.sourceLocation == null || item.destinationLocation == null) fail("transfer linkage is incomplete");
            if (item.sourceLocation.id.equals(item.destinationLocation.id)) fail("transfer source and destination cannot be the same");
            if (item.quantity == null || item.quantity.signum() <= 0) fail("transfer quantity must be positive");
            if (item.outMovement == null || item.inMovement == null) fail("transfer must own OUT and IN movements");
            if (!"TRANSFER_OUT".equals(item.outMovement.nature) || !"TRANSFER_IN".equals(item.inMovement.nature)) {
                fail("transfer movement natures are inconsistent");
            }
            if (!item.sourceLocation.id.equals(item.outMovement.location.id)
                    || !item.destinationLocation.id.equals(item.inMovement.location.id)) {
                fail("transfer movement locations are inconsistent");
            }
            if (item.outMovement.quantity == null || item.inMovement.quantity == null
                    || item.outMovement.quantity.compareTo(item.quantity) != 0
                    || item.inMovement.quantity.compareTo(item.quantity) != 0) {
                fail("transfer movement quantities differ from transfer item");
            }
            if (item.asset != null) {
                if (item.outMovement.asset == null || item.inMovement.asset == null
                        || !item.asset.id.equals(item.outMovement.asset.id)
                        || !item.asset.id.equals(item.inMovement.asset.id)) {
                    fail("serialized transfer lost asset traceability");
                }
            }
            if (item.lot != null) {
                if (item.outMovement.lot == null || item.inMovement.lot == null
                        || !item.lot.id.equals(item.outMovement.lot.id)
                        || !item.lot.id.equals(item.inMovement.lot.id)) {
                    fail("lot transfer lost lot traceability");
                }
            }
        }
    }

    private void verifyPhysicalInventory() {
        List<InventoryCountItem> items = em.createQuery("select i from InventoryCountItem i", InventoryCountItem.class).getResultList();
        if (items.isEmpty()) fail("physical inventory regression data is missing");

        for (InventoryCountItem item : items) {
            if (item.inventoryCount == null || item.model == null || item.result == null) fail("inventory count linkage/result is incomplete");
            if (item.systemQuantity == null || item.countedQuantity == null || item.differenceQuantity == null) {
                fail("inventory count quantities are incomplete");
            }
            BigDecimal expected = item.countedQuantity.subtract(item.systemQuantity);
            if (expected.compareTo(item.differenceQuantity) != 0) fail("inventory count difference is inconsistent");
            if (item.balance != null) {
                requireNonNegative(item.balance.available, "inventory count available balance");
                requireNonNegative(item.balance.reserved, "inventory count reserved balance");
                requireNonNegative(item.balance.blocked, "inventory count blocked balance");
            }
            if (item.adjustmentMovement != null) {
                if (item.adjustmentMovement.quantity == null || item.adjustmentMovement.movedAt == null) {
                    fail("reconciliation adjustment movement is incomplete");
                }
                if (item.lot != null && (item.adjustmentMovement.lot == null
                        || !item.lot.id.equals(item.adjustmentMovement.lot.id))) {
                    fail("reconciliation adjustment lost lot traceability");
                }
                if (item.asset != null && (item.adjustmentMovement.asset == null
                        || !item.asset.id.equals(item.adjustmentMovement.asset.id))) {
                    fail("reconciliation adjustment lost asset traceability");
                }
            }
        }
    }

    private void verifyMovementIntegrity() {
        List<StockMovement> movements = em.createQuery("select m from StockMovement m", StockMovement.class).getResultList();
        if (movements.isEmpty()) fail("stock movement history is missing");

        for (StockMovement movement : movements) {
            if (movement.location == null || movement.movedAt == null || movement.nature == null || movement.nature.isBlank()) {
                fail("stock movement core metadata is incomplete");
            }
            if (movement.asset == null && movement.lot == null) fail("stock movement has neither asset nor lot");
            if (movement.asset != null && movement.lot != null) fail("stock movement cannot point to asset and lot simultaneously");
            if (movement.quantity == null || movement.quantity.signum() < 0) fail("stock movement quantity cannot be negative");
        }
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static void requireNonNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) fail(label + " cannot be null/negative");
    }

    private static void fail(String detail) {
        throw new IllegalStateException("Oracle stock regression failed: " + detail);
    }
}
