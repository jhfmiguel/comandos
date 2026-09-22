package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_stock_location")
public class StockLocation extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    public Organization organization;
    @ManyToOne(optional = true)
    @JoinColumn(name = "unit_id", nullable = true)
    public OrganizationalUnit unit;
    @Column(name = "name", nullable = false, length = 255)
    public String name;
    @Column(name = "type", nullable = false, length = 255)
    public String type;
    @Column(name = "controlled", nullable = false)
    public Boolean controlled = false;
    @Column(name = "code", unique = true, length = 100) public String code;
    @Column(name = "warehouse_type", length = 100) public String warehouseType;
    @Column(name = "address", length = 500) public String address;
    @Column(name = "active", nullable = false) public Boolean active = true;
}
