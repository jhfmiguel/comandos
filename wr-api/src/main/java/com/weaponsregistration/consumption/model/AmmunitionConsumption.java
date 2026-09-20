package com.weaponsregistration.consumption.model;

import com.weaponsregistration.core.model.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_ammunition_consumption")
public class AmmunitionConsumption extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) public Organization organization;
    @ManyToOne @JoinColumn(name = "unit_id") public OrganizationalUnit unit;
    @ManyToOne(optional = false) @JoinColumn(name = "responsible_id", nullable = false) public Person responsible;
    @ManyToOne(optional = false) @JoinColumn(name = "authorizer_id", nullable = false) public Person authorizer;
    @Column(nullable = false) public String organizationName;
    @Column public String unitName;
    @Column(nullable = false) public String responsibleName;
    @Column(nullable = false) public String authorizerName;
    @Column(nullable = false, length = 255) public String purpose;
    @Column(name = "activity_type", nullable = false, length = 40) public String activityType = "OPERATION";
    @Column(name = "operation_training", length = 255) public String operationTraining;
    @Column(nullable = false, length = 30) public String status = "FINALIZED";
    @Column(nullable = false) public LocalDateTime consumedAt;
    @Column public Long finalizedById;
    @Column public String finalizedByLogin;
    @Column(nullable = false, unique = true, length = 36) public String requestId;
    @Column(nullable = false, length = 64) public String requestFingerprint;
}
