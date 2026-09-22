package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_model_characteristic_value", uniqueConstraints = {@UniqueConstraint(columnNames = {"model_id", "characteristic_id"})})
public class ModelCharacteristicValue extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    public ItemModel model;
    @ManyToOne(optional = false)
    @JoinColumn(name = "characteristic_id", nullable = false)
    public TechnicalCharacteristic characteristic;
    @Column(name = "characteristic_value", nullable = false, length = 255)
    public String value;
}
