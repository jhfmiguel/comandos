package com.weaponsregistration.purchase.model;

import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_purchase_receipt_item")
public class PurchaseReceiptItem extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    public PurchaseReceipt receipt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "purchase_item_id", nullable = false)
    public PurchaseItem purchaseItem;

    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "accepted_quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal acceptedQuantity = BigDecimal.ZERO;

    @Column(name = "rejected_quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal rejectedQuantity = BigDecimal.ZERO;

    @Column(name = "rejection_reason", length = 2000)
    public String rejectionReason;
}