package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_category_characteristic", uniqueConstraints = {@UniqueConstraint(columnNames = {"category_id", "characteristic_id"})})
public class CategoryCharacteristic extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    public ItemCategory category;
    @ManyToOne(optional = false)
    @JoinColumn(name = "characteristic_id", nullable = false)
    public TechnicalCharacteristic characteristic;
    @Column(name = "required_value", nullable = false)
    public Boolean requiredValue = false;
    @Column(name = "per_item", nullable = false)
    public Boolean perItem = false;
}
