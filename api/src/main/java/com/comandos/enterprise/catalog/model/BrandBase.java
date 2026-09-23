package com.comandos.enterprise.catalog.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BrandBase extends CoreEntity {

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "manufacturer", nullable = false, length = 255)
    public String manufacturer;

    @Column(name = "manufacturing_country_code", length = 2)
    public String manufacturingCountryCode;
}
