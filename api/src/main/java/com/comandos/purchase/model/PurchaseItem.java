package com.comandos.purchase.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.ItemModel;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_purchase_item")
public class PurchaseItem extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    public Purchase purchase;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_model_id", nullable = false)
    public ItemModel itemModel;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    public BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 19, scale = 4)
    public BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "condition_description", length = 1000)
    public String conditionDescription;

    @Column(name = "notes", length = 2000)
    public String notes;

    public BigDecimal calculateTotal() {
        return (quantity == null ? BigDecimal.ZERO : quantity)
            .multiply(unitPrice == null ? BigDecimal.ZERO : unitPrice)
            .subtract(discount == null ? BigDecimal.ZERO : discount);
    }

    public BigDecimal pendingQuantity() {
        return (quantity == null ? BigDecimal.ZERO : quantity)
            .subtract(receivedQuantity == null ? BigDecimal.ZERO : receivedQuantity);
    }
}