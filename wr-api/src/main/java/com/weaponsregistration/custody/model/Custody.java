package com.weaponsregistration.custody.model;

import com.weaponsregistration.core.model.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_custody")
public class Custody extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false)
    public Organization organization;
    @ManyToOne @JoinColumn(name = "unit_id")
    public OrganizationalUnit unit;
    @ManyToOne @JoinColumn(name = "recipient_id")
    public Person recipient;
    @ManyToOne @JoinColumn(name = "recipient_unit_id")
    public OrganizationalUnit recipientUnit;
    @ManyToOne(optional = false) @JoinColumn(name = "authorizer_id", nullable = false)
    public Person authorizer;
    @Column(nullable = false) public String organizationName;
    @Column public String unitName;
    @Column(nullable = false) public String recipientName;
    @Column(nullable = false) public String authorizerName;
    @Column(nullable = false, length = 255) public String purpose;
    @Column(name = "recipient_type", nullable = false, length = 30) public String recipientType = "PERSON";
    @Column(name = "custody_scope", nullable = false, length = 30) public String custodyScope = "INDIVIDUAL";
    @Column(name = "duration_type", nullable = false, length = 30) public String durationType = "TEMPORARY";
    @Column(name = "team_operation", length = 255) public String teamOperation;
    @Column(name = "responsibility_term", length = 1000) public String responsibilityTerm;
    @Column(name = "delivery_condition", length = 500) public String deliveryCondition;
    @Column(nullable = false, length = 30) public String status = "ACTIVE";
    @Column(nullable = false) public LocalDateTime deliveredAt;
    @Column public LocalDateTime dueAt;
    @Column public LocalDateTime completedAt;
    @Column public Long issuedById;
    @Column public String issuedByLogin;
    @Column(nullable = false, unique = true, length = 36) public String requestId;
    @Column(nullable = false, length = 64) public String requestFingerprint;
}
