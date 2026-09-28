package com.comandos.consumption.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_consumable_usage_item")
public class ConsumableUsageItem extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "usage_id", nullable = false) public ConsumableUsage usage;
    @ManyToOne(optional = false) @JoinColumn(name = "lot_id", nullable = false) public StockLot lot;
    @ManyToOne(optional = false) @JoinColumn(name = "balance_id", nullable = false) public StockBalance balance;
    @ManyToOne(optional = false) @JoinColumn(name = "location_id", nullable = false) public StockLocation location;
    @OneToOne(optional = false) @JoinColumn(name = "delivery_movement_id", nullable = false, unique = true) public StockMovement deliveryMovement;
    @OneToOne @JoinColumn(name = "return_movement_id", unique = true) public StockMovement returnMovement;
    @Column(nullable = false) public String family;
    @Column(nullable = false) public String modelName;
    @Column(nullable = false) public String sku;
    @Column(nullable = false) public String lotNumber;
    @Column(nullable = false) public String locationName;
    @Column(nullable = false) public String unitOfMeasure;
    @Column(name = "delivered_quantity", nullable = false, precision = 19, scale = 4) public BigDecimal deliveredQuantity;
    @Column(name = "used_quantity", nullable = false, precision = 19, scale = 4) public BigDecimal usedQuantity;
    @Column(name = "returned_quantity", nullable = false, precision = 19, scale = 4) public BigDecimal returnedQuantity;
    @Column(name = "used_at", nullable = false) public LocalDateTime usedAt;
    @Column(name = "returned_at") public LocalDateTime returnedAt;
    @Column(nullable = false, length = 255) public String result;

    @PrePersist @PreUpdate
    void validateLifecycle() {
        if (deliveredQuantity == null || usedQuantity == null || returnedQuantity == null
                || deliveredQuantity.signum() <= 0 || usedQuantity.signum() < 0 || returnedQuantity.signum() < 0
                || usedQuantity.add(returnedQuantity).compareTo(deliveredQuantity) != 0) {
            throw new IllegalStateException("Delivered quantity must equal used plus returned quantity.");
        }
        if (deliveryMovement == null || deliveryMovement.quantity == null
                || deliveryMovement.quantity.compareTo(deliveredQuantity.negate()) != 0) {
            throw new IllegalStateException("Delivery movement must remove the complete delivered quantity.");
        }
        if (returnedQuantity.signum() == 0 && returnMovement != null) {
            throw new IllegalStateException("Zero return cannot have a return movement.");
        }
        if (returnedQuantity.signum() > 0 && (returnMovement == null || returnMovement.quantity == null
                || returnMovement.quantity.compareTo(returnedQuantity) != 0)) {
            throw new IllegalStateException("Returned quantity requires an equivalent positive return movement.");
        }
    }
}
