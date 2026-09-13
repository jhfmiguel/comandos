package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_technical_characteristic")
public class TechnicalCharacteristic extends CoreEntity {
    @Column(name = "name", nullable = false, length = 255)
    public String name;
    @Column(name = "data_type", nullable = false, length = 255)
    public String dataType;
    @Column(name = "unit_of_measure", nullable = true, length = 255)
    public String unitOfMeasure;
}
