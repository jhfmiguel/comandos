package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_expiration_record")
public class ExpirationRecord extends CoreEntity {
    @Column(name = "organization_id", nullable = false)
    public Long organizationLegacyId;

    @Column(name = "unit_id")
    public Long unitLegacyId;

    @Column(name = "organization_canonical_id", length = 128)
    public String organizationCanonicalId;

    @Column(name = "unit_canonical_id", length = 128)
    public String unitCanonicalId;

    @ManyToOne
    @JoinColumn(name = "asset_id")
    public AssetItem asset;

    @ManyToOne
    @JoinColumn(name = "lot_id")
    public StockLot lot;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public LocalDate expirationDate;

    @Column(nullable = false)
    public String status;
}
