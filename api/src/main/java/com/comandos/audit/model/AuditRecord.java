package com.comandos.audit.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

@Entity
@Immutable
@Table(name = "erp_audit_record", indexes = {
    @Index(name = "idx_audit_resource_record", columnList = "resource_name,record_id"),
    @Index(name = "idx_audit_occurred_at", columnList = "occurred_at")
})
public class AuditRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, updatable = false) public Instant occurredAt;
    @Column(updatable = false) public Long actorId;
    @Column(updatable = false) public String actorLogin;
    @Column(nullable = false, updatable = false) public String actorType;
    @Column(name = "resource_name", nullable = false, updatable = false) public String resource;
    @Column(nullable = false, updatable = false) public Long recordId;
    @Column(nullable = false, updatable = false) public String action;
    @Lob @Column(updatable = false) public String beforeJson;
    @Lob @Column(updatable = false) public String afterJson;

    @PreRemove
    private void preventRemoval() { throw new IllegalStateException("Audit records cannot be deleted."); }
}
