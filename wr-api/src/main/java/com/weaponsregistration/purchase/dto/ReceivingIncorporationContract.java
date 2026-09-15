package com.weaponsregistration.purchase.dto;

import java.time.LocalDateTime;

public final class ReceivingIncorporationContract {
    private ReceivingIncorporationContract() {}

    public record CreateRequest(
        Long receivingId,
        Long receivingItemId,
        Long receivingSerialId,
        Long stockLocationId,
        String assetCode,
        String lotNumber,
        String incorporatedBy,
        LocalDateTime incorporatedAt,
        String notes
    ) {}
}