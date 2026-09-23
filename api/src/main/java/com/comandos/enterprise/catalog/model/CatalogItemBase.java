package com.comandos.enterprise.catalog.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;

@MappedSuperclass
public abstract class CatalogItemBase extends CoreEntity {

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "unit_of_measure", nullable = false, length = 255)
    public String unitOfMeasure;

    @Column(name = "manufacturer_code", length = 255)
    public String manufacturerCode;

    @Column(name = "sku", nullable = false, length = 255)
    public String sku;

    @Column(name = "description", length = 255)
    public String description;

    @Column(name = "list_price", nullable = false, precision = 19, scale = 4)
    public BigDecimal listPrice = BigDecimal.ZERO;
}
