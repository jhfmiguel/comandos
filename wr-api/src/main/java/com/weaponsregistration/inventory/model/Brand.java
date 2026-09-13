package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_brand", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "manufacturer"})})
public class Brand extends CoreEntity {
    @Column(name = "name", nullable = false, length = 255)
    public String name;
    @Column(name = "manufacturer", nullable = false, length = 255)
    public String manufacturer;
}
