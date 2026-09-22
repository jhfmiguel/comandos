package com.weaponsregistration.sales.model;

import com.weaponsregistration.core.model.*;
import com.weaponsregistration.model.PaymentMethod;
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
    @Column(name = "process_number", length = 255) public String processNumber;
    @Column(name = "legal_basis", length = 500) public String legalBasis;
    @Column(name = "document_reference", length = 1000) public String documentReference;
    @Column(name = "withdrawn_at") public LocalDateTime withdrawnAt;
    @Column(name = "withdrawn_by_login") public String withdrawnByLogin;
    @Column(nullable = false) public String status = "FINALIZED";
    @Column(nullable = false) public LocalDateTime finalizedAt;
    @Column public Long finalizedById;
    @Column public String finalizedByLogin;
    @Column(nullable = false, precision = 19, scale = 4) public BigDecimal total;
    @Column(nullable = false, unique = true, length = 36) public String requestId;
    @Column(nullable = false, length = 64) public String requestFingerprint;
}
