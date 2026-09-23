package com.comandos.enterprise.catalog.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class CatalogCategoryBase extends CoreEntity {

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "family", nullable = false, length = 255)
    public String family;

    @Column(name = "serialized", nullable = false)
    public Boolean serialized = false;

    @Column(name = "lot_controlled", nullable = false)
    public Boolean lotControlled = false;

    @Column(name = "consumable", nullable = false)
    public Boolean consumable = false;
}
