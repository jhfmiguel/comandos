package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_regulatory_control", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"asset_id", "external_system"}),
    @UniqueConstraint(columnNames = {"external_system", "registration_number"})
})
public class RegulatoryControl extends CoreEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    public AssetItem asset;

    @Column(name = "external_system", nullable = false, length = 100)
    public String externalSystem;

    @Column(name = "registration_number", nullable = false, length = 100)
    public String registrationNumber;

    @Column(nullable = false, length = 30)
    public String status;

    @Column(name = "valid_until")
    public LocalDate validUntil;
}
