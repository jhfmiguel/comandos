package com.comandos.inventory.model;

import com.comandos.enterprise.catalog.model.BrandBase;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_brand", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "manufacturer"})})
public class Brand extends BrandBase {
}
