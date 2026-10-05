package com.comandos.donation.model;

import com.comandos.core.model.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity @Table(name = "erp_donation")
public class Donation extends CoreEntity {
    @Column(name="organization_id", nullable=false) public Long organizationLegacyId;
    @Column(name="unit_id") public Long unitLegacyId;
    @Column(name="donor_id", nullable=false) public Long donorLegacyId;
    @Column(name="donee_id", nullable=false) public Long doneeLegacyId;
    @ManyToOne @JoinColumn(name="organization_id", insertable=false, updatable=false) public Organization organization;
    @ManyToOne @JoinColumn(name="unit_id", insertable=false, updatable=false) public OrganizationalUnit unit;
    @ManyToOne @JoinColumn(name="donor_id", insertable=false, updatable=false) public Person donor;
    @ManyToOne @JoinColumn(name="donee_id", insertable=false, updatable=false) public Person donee;
    @Column(name="organization_canonical_id", length=128) public String organizationCanonicalId;
    @Column(name="unit_canonical_id", length=128) public String unitCanonicalId;
    @Column(name="donor_canonical_id", length=128) public String donorCanonicalId;
    @Column(name="donee_canonical_id", length=128) public String doneeCanonicalId;
    @Column(nullable=false) public String organizationName;
    @Column public String unitName;
    @Column(nullable=false) public String donorName;
    @Column(nullable=false) public String doneeName;
    @Column(nullable=false, length=255) public String term;
    @Column(name="direction", nullable=false, length=30) public String direction = "OUTGOING";
    @Column(name="event_type", nullable=false, length=30) public String eventType = "REALIZED";
    @Column(name="document_reference", length=500) public String documentReference;
    @Column(name="term_confirmed", nullable=false) public boolean termConfirmed = true;
    @Column(name="title_transfer_state", nullable=false, length=50) public String titleTransferState = "TRANSFERRED_TO_DONEE";
    @Column(name="title_transferred_at") public LocalDateTime titleTransferredAt;
    @Column(name="approved_by_id") public Long approvedById;
    @Column(name="approved_by_login") public String approvedByLogin;
    @Column(name="approved_at") public LocalDateTime approvedAt;
    @Column(nullable=false, length=30) public String status = "FINALIZED";
    @Column(nullable=false) public LocalDateTime finalizedAt;
    @Column(name="received_at") public LocalDateTime receivedAt;
    @Column(name="realized_at") public LocalDateTime realizedAt;
    @Column public Long finalizedById;
    @Column public String finalizedByLogin;
    @Column(nullable=false, unique=true, length=36) public String requestId;
    @Column(nullable=false, length=64) public String requestFingerprint;

    @PrePersist
    void lifecycleDefaults() {
        direction = direction == null || direction.isBlank() ? "OUTGOING" : direction.trim().toUpperCase(Locale.ROOT);
        if ("INCOMING".equals(direction)) {
            eventType = "RECEIVED";
            titleTransferState = "TRANSFERRED_TO_ORGANIZATION";
            if (receivedAt == null) receivedAt = finalizedAt;
        } else {
            direction = "OUTGOING";
            eventType = "REALIZED";
            titleTransferState = "TRANSFERRED_TO_DONEE";
            if (realizedAt == null) realizedAt = finalizedAt;
        }
        termConfirmed = term != null && !term.isBlank();
        if (titleTransferredAt == null) titleTransferredAt = finalizedAt;
    }
}
