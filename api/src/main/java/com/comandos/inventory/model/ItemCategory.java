package com.comandos.inventory.model;

import jakarta.persistence.*;

@Entity
@Table(name = "erp_item_category")
public class ItemCategory extends InventoryCategoryBase {
    @ManyToOne
    @JoinColumn(name = "parent_category_id")
    public ItemCategory parentCategory;
}
