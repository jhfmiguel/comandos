package com.weaponsregistration.audit.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/** Historical scalar references; never join to the current business record. */
@Entity
@Immutable
@Table(name = "erp_audit_reference", indexes = @Index(name = "idx_audit_reference_lookup", columnList = "kind,target_id,event_id"),
    uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "kind", "target_id"}))
public class AuditReference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, updatable = false) public Long eventId;
    @Column(nullable = false, updatable = false) public String kind;
    @Column(nullable = false, updatable = false) public Long targetId;

    @PreRemove
    private void preventRemoval() { throw new IllegalStateException("Audit references cannot be deleted."); }
}
