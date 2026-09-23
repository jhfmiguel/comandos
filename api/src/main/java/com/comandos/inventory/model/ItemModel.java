package com.comandos.inventory.model;

import com.comandos.enterprise.catalog.model.CatalogItemBase;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_item_model", uniqueConstraints = {@UniqueConstraint(columnNames = {"sku"})})
public class ItemModel extends CatalogItemBase {
    @ManyToOne
    @JoinColumn(name = "armament_type_id")
    public ArmamentType armamentType;

    @ManyToOne
    @JoinColumn(name = "armament_classification_id")
    public ArmamentClassification armamentClassification;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    public ItemCategory category;

    @ManyToOne(optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    public Brand brand;
}
