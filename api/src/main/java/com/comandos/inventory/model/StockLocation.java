package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_stock_location")
public class StockLocation extends CoreEntity {
    @Column(name = "organization_id", nullable = false)
    public Long organizationLegacyId;
    @Column(name = "unit_id")
    public Long unitLegacyId;
    @ManyToOne
    @JoinColumn(name = "organization_id", insertable = false, updatable = false)
    public Organization organization;
    @ManyToOne
    @JoinColumn(name = "unit_id", insertable = false, updatable = false)
    public OrganizationalUnit unit;

    @Column(name = "organization_canonical_id", length = 128)
    public String organizationCanonicalId;

    @Column(name = "unit_canonical_id", length = 128)
    public String unitCanonicalId;

    public boolean matchesCanonicalScope(String organizationId, String unitId) {
        if (!java.util.Objects.equals(organizationCanonicalId, organizationId)) {
            return false;
        }
        return unitId == null || java.util.Objects.equals(unitCanonicalId, unitId);
    }

    public void requireCanonicalScope(String organizationId, String unitId) {
        if (!matchesCanonicalScope(organizationId, unitId)) {
            throw new IllegalStateException(
                "Stock location does not belong to the requested canonical scope."
            );
        }
    }
    @Column(name = "name", nullable = false, length = 255)
    public String name;
    @Column(name = "type", nullable = false, length = 255)
    public String type;
    @Column(name = "controlled", nullable = false)
    public Boolean controlled = false;
    @Column(name = "code", unique = true, length = 100) public String code;
    @Column(name = "warehouse_type", length = 100) public String warehouseType;
    @Column(name = "address", length = 500) public String address;
    @Column(name = "active", nullable = false) public Boolean active = true;
}
