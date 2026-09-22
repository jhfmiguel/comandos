package com.comandos.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ReceivingInspectionContract {
    private ReceivingInspectionContract() {}

    public record SerialDecision(
        Long serialId,
        Boolean accepted,
        String rejectionReason
    ) {}

    public record ItemDecision(
        Long receivingItemId,
        BigDecimal acceptedQuantity,
        BigDecimal rejectedQuantity,
        String divergenceDescription,
        List<SerialDecision> serials
    ) {}

    public record CreateRequest(
        LocalDateTime inspectedAt,
        String inspector,
        Boolean provisionalReceipt,
        Boolean definitiveReceipt,
        Boolean approved,
        String nonConformity,
        String decisionNotes,
        List<ItemDecision> items
    ) {}
}
