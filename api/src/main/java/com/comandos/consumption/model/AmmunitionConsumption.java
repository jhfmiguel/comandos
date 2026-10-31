package com.comandos.consumption.model;

import com.comandos.core.model.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_ammunition_consumption")
public class AmmunitionConsumption extends CoreEntity {
    @Column(name = "organization_id", nullable = false) public Long organizationLegacyId;
    @Column(name = "unit_id") public Long unitLegacyId;
    @Column(name = "responsible_id", nullable = false) public Long responsibleLegacyId;
    @Column(name = "authorizer_id", nullable = false) public Long authorizerLegacyId;
    @Column(name = "organization_canonical_id", length = 128) public String organizationCanonicalId;
    @Column(name = "unit_canonical_id", length = 128) public String unitCanonicalId;
    @Column(name = "responsible_canonical_id", length = 128) public String responsibleCanonicalId;
    @Column(name = "authorizer_canonical_id", length = 128) public String authorizerCanonicalId;
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
