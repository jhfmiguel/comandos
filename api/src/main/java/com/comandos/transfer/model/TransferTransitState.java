package com.comandos.transfer.model;

/**
 * Physical/logistical lifecycle of stock after it leaves the source unit.
 * This is intentionally separate from the legacy workflow status so existing
 * integrations can keep using PENDING_ACCEPTANCE / ACCEPTED / REJECTED.
 */
public enum TransferTransitState {
    IN_TRANSIT,
    RECEIVED,
    RETURNED_TO_SOURCE
}
