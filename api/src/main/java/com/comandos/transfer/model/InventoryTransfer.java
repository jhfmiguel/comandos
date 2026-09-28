package com.comandos.transfer.model;

import com.comandos.core.model.*;
import com.comandos.inventory.model.StockLocation;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_inventory_transfer")
public class InventoryTransfer extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    public Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_unit_id", nullable = false)
    public OrganizationalUnit sourceUnit;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_unit_id", nullable = false)
    public OrganizationalUnit destinationUnit;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_location_id", nullable = false)
    public StockLocation destinationLocation;

    @Column(nullable = false)
    public String organizationName;

    @Column(nullable = false)
    public String sourceUnitName;

    @Column(nullable = false)
    public String destinationUnitName;

    @Column(nullable = false)
    public String destinationLocationName;

    @Column(nullable = false, length = 255)
    public String purpose;

    @Column(name = "transfer_type", nullable = false, length = 30)
    public String transferType = "INTERNAL";

    @Column(name = "legal_instrument", length = 255)
    public String legalInstrument;

    @Column(name = "document_reference", length = 500)
    public String documentReference;

    /** Backward-compatible workflow state used by existing clients. */
    @Column(nullable = false, length = 30)
    public String status = "PENDING_ACCEPTANCE";

    /**
     * Explicit physical/logistical state. It is nullable at schema level so an
     * existing installation can add the column without invalidating historical
     * rows; every new transfer is persisted with a concrete state.
     */
    @Column(name = "transit_state", length = 30)
    public String transitState = "IN_TRANSIT";

    /** Legacy dispatch timestamp kept for API/database compatibility. */
    @Column(nullable = false)
    public LocalDateTime sentAt;

    /** Explicit dispatch metadata for the transit lifecycle. */
    @Column(name = "dispatched_at")
    public LocalDateTime dispatchedAt;

    @Column(name = "dispatched_by_id")
    public Long dispatchedById;

    @Column(name = "dispatched_by_login")
    public String dispatchedByLogin;

    @Column(name = "received_at")
    public LocalDateTime receivedAt;

    @Column(name = "received_by_id")
    public Long receivedById;

    @Column(name = "received_by_login")
    public String receivedByLogin;

    @Column(name = "transit_closed_at")
    public LocalDateTime transitClosedAt;

    @Column(name = "approved_by_id")
    public Long approvedById;

    @Column(name = "approved_by_login")
    public String approvedByLogin;

    @Column(name = "approved_at")
    public LocalDateTime approvedAt;

    @Column(name = "rejected_by_id")
    public Long rejectedById;

    @Column(name = "rejected_by_login")
    public String rejectedByLogin;

    @Column(name = "rejected_at")
    public LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    public String rejectionReason;

    /** Legacy issuer/finalizer identity kept for existing integrations. */
    @Column
    public Long finalizedById;

    @Column
    public String finalizedByLogin;

    @Column(nullable = false, unique = true, length = 36)
    public String requestId;

    @Column(nullable = false, length = 64)
    public String requestFingerprint;
}
