package com.weaponsregistration.purchase.dto;

import java.time.LocalDateTime;

public final class ReceivingInspectionContract {
    private ReceivingInspectionContract() {}

    public record CreateRequest(
        LocalDateTime inspectedAt,
        String inspector,
        Boolean provisionalReceipt,
        Boolean definitiveReceipt,
        Boolean approved,
        String nonConformity,
        String decisionNotes
    ) {}
}