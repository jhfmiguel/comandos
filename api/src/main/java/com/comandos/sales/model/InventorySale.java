package com.comandos.sales.model;

import com.comandos.core.model.*;
import com.comandos.model.PaymentMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_sale")
public class InventorySale extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false)
    public Organization organization;
    @ManyToOne @JoinColumn(name = "unit_id")
    public OrganizationalUnit unit;
    @Column public String unitName;
    @ManyToOne(optional = false) @JoinColumn(name = "buyer_id", nullable = false)
    public Person buyer;
    @Column(nullable = false) public String organizationName;
    @Column(nullable = false) public String buyerName;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public PaymentMethod paymentMethod;
    @Column(name = "process_number", nullable = false, length = 255) public String processNumber;
    @Column(name = "legal_basis", nullable = false, length = 500) public String legalBasis;
    @Column(name = "document_reference", nullable = false, length = 1000) public String documentReference;
    @Column(name = "title_transfer_state", nullable = false, length = 50) public String titleTransferState = "TRANSFERRED_TO_BUYER";
    @Column(name = "title_transferred_at") public LocalDateTime titleTransferredAt;
    @Column(name = "withdrawal_state", nullable = false, length = 50) public String withdrawalState = "WITHDRAWN";
    @Column(name = "withdrawn_at", nullable = false) public LocalDateTime withdrawnAt;
    @Column(name = "withdrawn_by_login", nullable = false) public String withdrawnByLogin;
    @Column(nullable = false) public String status = "FINALIZED";
    @Column(nullable = false) public LocalDateTime finalizedAt;
    @Column public Long finalizedById;
    @Column public String finalizedByLogin;
    @Column(nullable = false, precision = 19, scale = 4) public BigDecimal total;
    @Column(nullable = false, unique = true, length = 36) public String requestId;
    @Column(nullable = false, length = 64) public String requestFingerprint;

    @PrePersist
    @PreUpdate
    void enforceLifecycle() {
        if (processNumber == null || processNumber.isBlank()) throw new IllegalStateException("Sale process number is required.");
        if (legalBasis == null || legalBasis.isBlank()) throw new IllegalStateException("Sale legal basis is required.");
        if (documentReference == null || documentReference.isBlank()) throw new IllegalStateException("Sale document reference is required.");
        if (withdrawnAt == null) withdrawnAt = finalizedAt;
        if (withdrawnByLogin == null || withdrawnByLogin.isBlank()) withdrawnByLogin = finalizedByLogin == null ? "system" : finalizedByLogin;
        if (titleTransferredAt == null) titleTransferredAt = finalizedAt;
        if (titleTransferState == null || titleTransferState.isBlank()) titleTransferState = "TRANSFERRED_TO_BUYER";
        if (withdrawalState == null || withdrawalState.isBlank()) withdrawalState = "WITHDRAWN";
    }
}
