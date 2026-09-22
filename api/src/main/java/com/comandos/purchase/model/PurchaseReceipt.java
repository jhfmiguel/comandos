package com.comandos.purchase.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "erp_purchase_receipt")
public class PurchaseReceipt extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    public Purchase purchase;

    @Column(name = "receipt_number", nullable = false, length = 100)
    public String receiptNumber;

    @Column(name = "receipt_date", nullable = false)
    public LocalDate receiptDate;

    @Column(name = "invoice_number", length = 100)
    public String invoiceNumber;

    @Column(name = "invoice_key", length = 100)
    public String invoiceKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    public ReceiptStatus status = ReceiptStatus.DRAFT;

    @Column(name = "inspection_notes", length = 4000)
    public String inspectionNotes;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<PurchaseReceiptItem> items = new ArrayList<>();
}