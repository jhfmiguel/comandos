package com.comandos.inventory.model;

import jakarta.persistence.*;

@Entity
@Table(name = "erp_brand", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "manufacturer"})})
public class Brand extends InventoryBrandBase {
}
