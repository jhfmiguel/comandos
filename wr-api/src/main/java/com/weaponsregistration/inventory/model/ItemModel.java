package com.weaponsregistration.inventory.model;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_item_model", uniqueConstraints = {@UniqueConstraint(columnNames = {"sku"})})
public class ItemModel extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    public ItemCategory category;
    @ManyToOne(optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    public Brand brand;
    @Column(name = "name", nullable = false, length = 255)
    public String name;
    @Column(name = "unit_of_measure", nullable = false, length = 255)
    public String unitOfMeasure;
    @Column(name = "manufacturer_code", nullable = true, length = 255)
    public String manufacturerCode;
    @Column(name = "sku", nullable = false, length = 255)
    public String sku;
    @Column(name = "description", nullable = true, length = 255)
    public String description;
    @Column(name = "list_price", nullable = false, precision = 19, scale = 4)
    public BigDecimal listPrice = BigDecimal.ZERO;
}
