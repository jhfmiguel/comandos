package com.weaponsregistration.donation.model;

import com.weaponsregistration.core.model.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "erp_donation")
public class Donation extends CoreEntity {
    @ManyToOne(optional=false) @JoinColumn(name="organization_id", nullable=false) public Organization organization;
    @ManyToOne @JoinColumn(name="unit_id") public OrganizationalUnit unit;
    @ManyToOne(optional=false) @JoinColumn(name="donor_id", nullable=false) public Person donor;
    @ManyToOne(optional=false) @JoinColumn(name="donee_id", nullable=false) public Person donee;
    @Column(nullable=false) public String organizationName;
    @Column public String unitName;
    @Column(nullable=false) public String donorName;
    @Column(nullable=false) public String doneeName;
    @Column(nullable=false, length=255) public String term;
    @Column(nullable=false, length=30) public String status = "FINALIZED";
    @Column(nullable=false) public LocalDateTime finalizedAt;
    @Column public Long finalizedById;
    @Column public String finalizedByLogin;
    @Column(nullable=false, unique=true, length=36) public String requestId;
    @Column(nullable=false, length=64) public String requestFingerprint;
}
