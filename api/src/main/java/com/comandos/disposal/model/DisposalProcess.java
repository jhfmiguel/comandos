package com.comandos.disposal.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_disposal_process", uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "process_number"}))
public class DisposalProcess extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) public Organization organization;
    @ManyToOne @JoinColumn(name = "unit_id") public OrganizationalUnit unit;
    @Column(nullable = false) public String organizationName;
    @Column public String unitName;
    @Column(name = "process_number", nullable = false) public String processNumber;
    @Column(nullable = false, length = 255) public String reason;
    @Column(nullable = false, length = 30) public String status = "FINALIZED";
    @Column(nullable = false) public LocalDateTime finalizedAt;
    @Column public Long finalizedById;
    @Column public String finalizedByLogin;
    @Column(nullable = false, unique = true, length = 36) public String requestId;
    @Column(nullable = false, length = 64) public String requestFingerprint;
}
