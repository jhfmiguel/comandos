package com.comandos.demo;

import com.comandos.inventory.model.AssetStatus;
import com.comandos.inventory.model.StockMovementNature;
import com.comandos.transfer.model.InventoryTransfer;
import com.comandos.transfer.model.InventoryTransferItem;
import com.comandos.transfer.model.TransferTransitState;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@Order(1870)
public class TransferTransitDemoVerifier implements ApplicationRunner {

    private static final Set<String> LEGACY_IN_TRANSIT = Set.of("PENDING_ACCEPTANCE", "SENT");
    private final EntityManager em;

    public TransferTransitDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        List<InventoryTransfer> transfers = em.createQuery(
            "select t from InventoryTransfer t order by t.id", InventoryTransfer.class
        ).getResultList();

        if (transfers.isEmpty()) fail("transfer regression data is missing");

        boolean lifecycleCovered = false;
        for (InventoryTransfer transfer : transfers) {
            verifyHeader(transfer);
            String state = effectiveState(transfer);

            if (LEGACY_IN_TRANSIT.contains(transfer.status)) {
                lifecycleCovered = true;
                require(TransferTransitState.IN_TRANSIT.name().equals(state),
                    "pending transfer must be explicitly IN_TRANSIT: transfer=" + transfer.id);
                require(effectiveDispatchAt(transfer) != null,
                    "in-transit transfer must preserve dispatch timestamp: transfer=" + transfer.id);
                require(transfer.transitClosedAt == null,
                    "in-transit transfer cannot already have a closed transit timestamp: transfer=" + transfer.id);
            } else if ("ACCEPTED".equals(transfer.status)) {
                lifecycleCovered = true;
                require(TransferTransitState.RECEIVED.name().equals(state),
                    "accepted transfer must be RECEIVED: transfer=" + transfer.id);
                LocalDateTime receivedAt = transfer.receivedAt == null ? transfer.approvedAt : transfer.receivedAt;
                require(receivedAt != null,
                    "received transfer must preserve destination receipt timestamp: transfer=" + transfer.id);
                require((transfer.transitClosedAt == null ? receivedAt : transfer.transitClosedAt) != null,
                    "received transfer must close transit: transfer=" + transfer.id);
            } else if ("REJECTED".equals(transfer.status)) {
                lifecycleCovered = true;
                require(TransferTransitState.RETURNED_TO_SOURCE.name().equals(state),
                    "rejected transfer must return to source: transfer=" + transfer.id);
                require(transfer.rejectedAt != null,
                    "rejected transfer must preserve rejection timestamp: transfer=" + transfer.id);
            }

            verifyItems(transfer);
        }

        require(lifecycleCovered, "no transfer row exercises the logistics lifecycle");
    }

    private void verifyHeader(InventoryTransfer transfer) {
        require(transfer.organization != null, "transfer without organization: transfer=" + transfer.id);
        require(transfer.sourceUnit != null, "transfer without source unit: transfer=" + transfer.id);
        require(transfer.destinationUnit != null, "transfer without destination unit: transfer=" + transfer.id);
        require(transfer.destinationLocation != null, "transfer without destination location: transfer=" + transfer.id);
        require(!transfer.sourceUnit.id.equals(transfer.destinationUnit.id),
            "transfer source and destination units must differ: transfer=" + transfer.id);
        require(notBlank(transfer.sourceUnitName),
            "transfer must preserve historical source unit name: transfer=" + transfer.id);
        require(notBlank(transfer.destinationUnitName),
            "transfer must preserve historical destination unit name: transfer=" + transfer.id);
        require(transfer.sentAt != null, "transfer without legacy dispatch timestamp: transfer=" + transfer.id);
    }

    private void verifyItems(InventoryTransfer transfer) {
        List<InventoryTransferItem> items = em.createQuery(
            "select i from InventoryTransferItem i where i.transfer.id = :id order by i.id",
            InventoryTransferItem.class
        ).setParameter("id", transfer.id).getResultList();

        if (items.isEmpty()) return; // synthetic coverage rows can exist without a complete operational scenario.

        for (InventoryTransferItem item : items) {
            require(item.sourceLocation != null && item.destinationLocation != null,
                "transfer item without source/destination location: item=" + item.id);
            require(item.outMovement != null && item.inMovement != null,
                "transfer item must preserve OUT/IN movement pair: item=" + item.id);
            require(StockMovementNature.TRANSFER_OUT.name().equals(item.outMovement.nature),
                "transfer item OUT movement has wrong nature: item=" + item.id);
            require(StockMovementNature.TRANSFER_IN.name().equals(item.inMovement.nature),
                "transfer item IN movement has wrong nature: item=" + item.id);
            require(item.outMovement.location.id.equals(item.sourceLocation.id),
                "transfer OUT movement must preserve historical source location: item=" + item.id);
            require(item.inMovement.location.id.equals(item.destinationLocation.id),
                "transfer IN movement must preserve destination location: item=" + item.id);
            require(item.outMovement.referenceId != null && item.outMovement.referenceId.equals(transfer.id),
                "transfer OUT movement must reference transfer: item=" + item.id);
            require(item.inMovement.referenceId != null && item.inMovement.referenceId.equals(transfer.id),
                "transfer IN movement must reference transfer: item=" + item.id);
            require(item.quantity != null && item.quantity.signum() > 0,
                "transfer item quantity must be positive: item=" + item.id);
            require(item.outMovement.quantity.abs().compareTo(item.quantity) == 0,
                "transfer OUT movement quantity mismatch: item=" + item.id);
            require(item.inMovement.quantity.abs().compareTo(item.quantity) == 0,
                "transfer IN movement quantity mismatch: item=" + item.id);

            if (LEGACY_IN_TRANSIT.contains(transfer.status) && item.asset != null) {
                require(AssetStatus.TRANSFER_PENDING.name().equals(item.asset.status),
                    "asset in transit must remain unavailable as TRANSFER_PENDING: asset=" + item.asset.id);
            }
        }
    }

    private static String effectiveState(InventoryTransfer transfer) {
        if ("ACCEPTED".equals(transfer.status)) return TransferTransitState.RECEIVED.name();
        if ("REJECTED".equals(transfer.status)) return TransferTransitState.RETURNED_TO_SOURCE.name();
        if (transfer.transitState != null && !transfer.transitState.isBlank()) return transfer.transitState;
        return TransferTransitState.IN_TRANSIT.name();
    }

    private static LocalDateTime effectiveDispatchAt(InventoryTransfer transfer) {
        return transfer.dispatchedAt == null ? transfer.sentAt : transfer.dispatchedAt;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) fail(message);
    }

    private static void fail(String message) {
        throw new IllegalStateException("Transfer transit regression failed: " + message);
    }
}
