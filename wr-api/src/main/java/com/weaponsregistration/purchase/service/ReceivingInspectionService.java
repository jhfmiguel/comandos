package com.weaponsregistration.purchase.service;

import com.weaponsregistration.purchase.dto.ReceivingInspectionContract;
import com.weaponsregistration.purchase.model.*;
import com.weaponsregistration.purchase.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReceivingInspectionService {
    private final ReceivingInspectionRepository inspections;
    private final EquipmentReceivingRepository receivings;
    private final EquipmentReceivingItemRepository items;
    private final ReceivingSerialRepository serials;
    private final PurchaseItemRepository purchaseItems;
    private final PurchaseRepository purchases;

    public ReceivingInspectionService(
        ReceivingInspectionRepository inspections,
        EquipmentReceivingRepository receivings,
        EquipmentReceivingItemRepository items,
        ReceivingSerialRepository serials,
        PurchaseItemRepository purchaseItems,
        PurchaseRepository purchases
    ) {
        this.inspections = inspections;
        this.receivings = receivings;
        this.items = items;
        this.serials = serials;
        this.purchaseItems = purchaseItems;
        this.purchases = purchases;
    }

    public List<ReceivingInspection> list(Long receivingId) {
        return receivingId == null
            ? inspections.findAll()
            : inspections.findByReceivingIdOrderByCreatedAtAsc(receivingId);
    }

    @Transactional
    public ReceivingInspection create(
        Long receivingId,
        ReceivingInspectionContract.CreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Inspection request is required.");
        }

        EquipmentReceiving receiving = receivings.findById(receivingId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Receiving not found: " + receivingId
            ));

        if (receiving.status == ReceivingStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled receiving cannot be inspected.");
        }
        if (receiving.status == ReceivingStatus.DEFINITIVELY_ACCEPTED
            || receiving.status == ReceivingStatus.DEFINITIVELY_PARTIALLY_ACCEPTED
            || receiving.status == ReceivingStatus.REJECTED) {
            throw new IllegalStateException("Definitive receiving inspection is already closed.");
        }
        if (!Boolean.TRUE.equals(receiving.physicalChecked)
            || !Boolean.TRUE.equals(receiving.documentsChecked)) {
            throw new IllegalStateException(
                "Physical and documentary checks are required before acceptance."
            );
        }

        boolean provisional = Boolean.TRUE.equals(request.provisionalReceipt());
        boolean definitive = Boolean.TRUE.equals(request.definitiveReceipt());
        if (provisional && definitive) {
            throw new IllegalArgumentException(
                "Inspection cannot be provisional and definitive at the same time."
            );
        }

        List<EquipmentReceivingItem> receivingItems =
            items.findByReceivingIdOrderByCreatedAtAsc(receivingId);
        if (receivingItems.isEmpty()) {
            throw new IllegalStateException("Receiving has no items.");
        }

        Map<Long, ReceivingInspectionContract.ItemDecision> decisions = new HashMap<>();
        if (request.items() != null) {
            for (ReceivingInspectionContract.ItemDecision decision : request.items()) {
                if (decision.receivingItemId() == null) {
                    throw new IllegalArgumentException("Receiving item is required in inspection.");
                }
                if (decisions.put(decision.receivingItemId(), decision) != null) {
                    throw new IllegalArgumentException(
                        "Receiving item cannot be repeated in inspection."
                    );
                }
            }
        }

        boolean legacyWholeReceivingDecision = decisions.isEmpty();
        boolean anyAccepted = false;
        boolean anyRejected = false;

        for (EquipmentReceivingItem item : receivingItems) {
            ReceivingInspectionContract.ItemDecision decision = decisions.get(item.id);

            BigDecimal accepted;
            BigDecimal rejected;

            if (legacyWholeReceivingDecision) {
                accepted = Boolean.TRUE.equals(request.approved())
                    ? item.receivedQuantity : BigDecimal.ZERO;
                rejected = Boolean.TRUE.equals(request.approved())
                    ? BigDecimal.ZERO : item.receivedQuantity;
            } else {
                if (decision == null) {
                    throw new IllegalArgumentException(
                        "Inspection decision is required for receiving item " + item.id + "."
                    );
                }
                accepted = nz(decision.acceptedQuantity());
                rejected = nz(decision.rejectedQuantity());
                if (accepted.signum() < 0 || rejected.signum() < 0) {
                    throw new IllegalArgumentException(
                        "Accepted and rejected quantities cannot be negative."
                    );
                }
                if (accepted.add(rejected).compareTo(item.receivedQuantity) != 0) {
                    throw new IllegalArgumentException(
                        "Accepted plus rejected quantity must equal received quantity for item "
                        + item.id + "."
                    );
                }
                if (decision.divergenceDescription() != null
                    && !decision.divergenceDescription().isBlank()) {
                    item.divergenceDescription = decision.divergenceDescription().trim();
                }
            }

            BigDecimal oldRejected = nz(item.rejectedQuantity);
            item.acceptedQuantity = accepted;
            item.rejectedQuantity = rejected;

            if (accepted.signum() > 0) {
                anyAccepted = true;
            }
            if (rejected.signum() > 0) {
                anyRejected = true;
                if (item.divergenceDescription == null || item.divergenceDescription.isBlank()) {
                    item.divergenceDescription =
                        "Inspection rejected " + rejected.toPlainString() + " unit(s).";
                }
            }

            updateAcquisitionFulfilledQuantity(item, oldRejected, rejected);
            applySerialDecisions(item, decision, accepted, rejected);
            items.save(item);
        }

        if (!legacyWholeReceivingDecision && decisions.size() != receivingItems.size()) {
            throw new IllegalArgumentException(
                "Inspection must contain a decision for every receiving item."
            );
        }

        ReceivingInspection inspection = new ReceivingInspection();
        inspection.receiving = receiving;
        inspection.inspectedAt = request.inspectedAt() == null
            ? LocalDateTime.now() : request.inspectedAt();
        inspection.inspector = clean(request.inspector());
        inspection.provisionalReceipt = provisional;
        inspection.definitiveReceipt = definitive;
        inspection.approved = anyAccepted && !anyRejected;
        inspection.nonConformity = clean(request.nonConformity());
        inspection.decisionNotes = clean(request.decisionNotes());
        inspection = inspections.save(inspection);

        if (anyAccepted && anyRejected) {
            receiving.status = definitive
                ? ReceivingStatus.DEFINITIVELY_PARTIALLY_ACCEPTED
                : provisional
                    ? ReceivingStatus.PROVISIONALLY_PARTIALLY_ACCEPTED
                    : ReceivingStatus.PARTIALLY_REJECTED;
        } else if (anyRejected) {
            receiving.status = definitive
                ? ReceivingStatus.REJECTED
                : ReceivingStatus.PARTIALLY_REJECTED;
        } else if (definitive) {
            receiving.status = ReceivingStatus.DEFINITIVELY_ACCEPTED;
        } else if (provisional) {
            receiving.status = ReceivingStatus.PROVISIONALLY_ACCEPTED;
        } else {
            receiving.status = ReceivingStatus.UNDER_INSPECTION;
        }
        receivings.save(receiving);

        updateAcquisitionStatus(receiving.acquisition);
        return inspection;
    }

    private void updateAcquisitionFulfilledQuantity(
        EquipmentReceivingItem item,
        BigDecimal oldRejected,
        BigDecimal newRejected
    ) {
        if (item.acquisitionItem == null) {
            return;
        }

        PurchaseItem acquisitionItem = purchaseItems.findByIdForUpdate(item.acquisitionItem.id)
            .orElseThrow(() -> new IllegalStateException(
                "Acquisition item no longer exists: " + item.acquisitionItem.id
            ));

        BigDecimal rejectionDelta = newRejected.subtract(oldRejected);
        BigDecimal fulfilled = nz(acquisitionItem.receivedQuantity).subtract(rejectionDelta);

        if (fulfilled.signum() < 0) {
            throw new IllegalStateException(
                "Acquisition fulfilled quantity cannot become negative."
            );
        }
        if (fulfilled.compareTo(acquisitionItem.quantity) > 0) {
            throw new IllegalStateException(
                "Acquisition fulfilled quantity cannot exceed acquired quantity."
            );
        }

        acquisitionItem.receivedQuantity = fulfilled;
        purchaseItems.save(acquisitionItem);
    }

    private void applySerialDecisions(
        EquipmentReceivingItem item,
        ReceivingInspectionContract.ItemDecision decision,
        BigDecimal acceptedQuantity,
        BigDecimal rejectedQuantity
    ) {
        List<ReceivingSerial> itemSerials =
            serials.findByReceivingItemIdOrderByCreatedAtAsc(item.id);

        if (itemSerials.isEmpty()) {
            return;
        }

        Map<Long, ReceivingInspectionContract.SerialDecision> serialDecisions = new HashMap<>();
        if (decision != null && decision.serials() != null) {
            for (ReceivingInspectionContract.SerialDecision serialDecision : decision.serials()) {
                if (serialDecision.serialId() == null) {
                    throw new IllegalArgumentException("Serial id is required.");
                }
                if (serialDecisions.put(serialDecision.serialId(), serialDecision) != null) {
                    throw new IllegalArgumentException(
                        "Serial cannot be repeated in inspection."
                    );
                }
            }
        }

        if (!serialDecisions.isEmpty() && serialDecisions.size() != itemSerials.size()) {
            throw new IllegalArgumentException(
                "Every serial of a serialized receiving item must be inspected."
            );
        }

        long acceptedSerials = 0;
        for (ReceivingSerial serial : itemSerials) {
            ReceivingInspectionContract.SerialDecision serialDecision =
                serialDecisions.get(serial.id);

            if (serialDecision != null) {
                serial.accepted = Boolean.TRUE.equals(serialDecision.accepted());
                serial.rejectionReason = Boolean.TRUE.equals(serial.accepted)
                    ? null : clean(serialDecision.rejectionReason());
            } else {
                serial.accepted = rejectedQuantity.signum() == 0;
                serial.rejectionReason = Boolean.TRUE.equals(serial.accepted)
                    ? null : "Rejected by receiving inspection.";
            }

            if (Boolean.TRUE.equals(serial.accepted)) {
                acceptedSerials++;
            }
            serials.save(serial);
        }

        if (acceptedQuantity.stripTrailingZeros().scale() > 0
            || acceptedQuantity.longValueExact() != acceptedSerials) {
            throw new IllegalArgumentException(
                "Accepted serialized quantity must equal accepted serial count."
            );
        }
    }

    private void updateAcquisitionStatus(Purchase acquisition) {
        if (acquisition == null) {
            return;
        }
        boolean pending = acquisition.items.stream()
            .anyMatch(item -> item.pendingQuantity().signum() > 0);
        acquisition.status = pending
            ? PurchaseStatus.PARTIALLY_RECEIVED
            : PurchaseStatus.RECEIVED;
        purchases.save(acquisition);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
