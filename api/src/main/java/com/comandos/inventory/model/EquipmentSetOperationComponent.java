package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_equipment_set_operation_component", uniqueConstraints = {
    @UniqueConstraint(name = "uk_equipment_set_operation_component", columnNames = {"operation_id", "source_component_id"})
})
public class EquipmentSetOperationComponent extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    public EquipmentSetOperation operation;

    @Column(name = "source_component_id", nullable = false)
    public Long sourceComponentId;

    @Column(name = "component_kind", nullable = false, length = 20)
    public String componentKind;

    @Column(name = "stock_record_id", nullable = false)
    public Long stockRecordId;

    @Column(name = "role", nullable = false, length = 255)
    public String role;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    public BigDecimal quantity;
}
