package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_item_category")
public class ItemCategory extends CoreEntity {
    @ManyToOne(optional = true)
    @JoinColumn(name = "parent_category_id", nullable = true)
    public ItemCategory parentCategory;
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
