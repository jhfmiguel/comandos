package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Product persistence fields for COMANDOS controlled-asset brands.
 *
 * <p>This is not a shared catalog contract. Shared catalog semantics belong to
 * Faria Miguel Enterprise Core; this class only preserves the current COMANDOS
 * JPA schema while product-specific inventory is retired/migrated.</p>
 */
@MappedSuperclass
public abstract class InventoryBrandBase extends CoreEntity {

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "manufacturer", nullable = false, length = 255)
    public String manufacturer;

    @Column(name = "manufacturing_country_code", length = 2)
    public String manufacturingCountryCode;
}
