package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "erp_ammunition_box",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ammunition_box_code", columnNames = {"box_code"}),
        @UniqueConstraint(name = "uk_ammunition_box_intake_seq", columnNames = {"intake_request_id", "sequence_number"})
    }
)
public class AmmunitionBox extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    public StockLot lot;

    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    public StockLocation location;

    @Column(name = "box_code", nullable = false, length = 80)
    public String boxCode;

    @Column(name = "intake_request_id", nullable = false, length = 36)
    public String intakeRequestId;

    @Column(name = "sequence_number", nullable = false)
    public Integer sequenceNumber;

    @Column(name = "nominal_quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal nominalQuantity = BigDecimal.ZERO;

    @Column(name = "available", nullable = false, precision = 19, scale = 4)
    public BigDecimal available = BigDecimal.ZERO;

    @Column(name = "reserved", nullable = false, precision = 19, scale = 4)
    public BigDecimal reserved = BigDecimal.ZERO;

    @Column(name = "blocked", nullable = false, precision = 19, scale = 4)
    public BigDecimal blocked = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 32)
    public String status = "SEALED";

    @Column(name = "opened_at")
    public LocalDateTime openedAt;
}
