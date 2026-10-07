package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "erp_certification_record",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"organization_id", "certification_type", "certificate_number"}
    )
)
public class CertificationRecord extends CoreEntity {
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

    @Column(name = "certification_type", nullable = false)
    public String type;

    @Column(name = "certificate_number", nullable = false)
    public String number;

    @Column(nullable = false)
    public LocalDate validUntil;

    @Column(nullable = false)
    public String status;
}
