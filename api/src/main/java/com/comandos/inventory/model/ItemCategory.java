package com.comandos.inventory.model;

import com.comandos.enterprise.catalog.model.CatalogCategoryBase;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_item_category")
public class ItemCategory extends CatalogCategoryBase {
    @ManyToOne
    @JoinColumn(name = "parent_category_id")
    public ItemCategory parentCategory;
}
