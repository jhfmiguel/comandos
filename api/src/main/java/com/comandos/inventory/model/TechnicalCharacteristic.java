package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
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
