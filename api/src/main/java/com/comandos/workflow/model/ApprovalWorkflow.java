package com.comandos.workflow.model;
import com.comandos.core.model.*;import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="erp_approval_workflow",indexes=@Index(name="idx_workflow_resource_record",columnList="resource,record_id"))
public class ApprovalWorkflow extends CoreEntity{
 @ManyToOne(optional=false)@JoinColumn(name="organization_id",nullable=false)public Organization organization;
 @ManyToOne@JoinColumn(name="unit_id")public OrganizationalUnit unit;
 @Column(nullable=false,length=100)public String operationType;@Column(nullable=false,length=100)public String resource;
 @Column(name="record_id")public Long recordId;@Column(nullable=false,length=30)public String status="REQUESTED";
 @Column(nullable=false,length=2000)public String justification;@Column(nullable=false)public LocalDateTime requestedAt;
 @Column public Long requestedById;@Column public String requestedByLogin;@Column public Long authorityId;@Column public String authorityLogin;
 @Column public LocalDateTime authorizedAt;@Column public LocalDateTime executedAt;@Column public LocalDateTime concludedAt;@Column public LocalDateTime cancelledAt;
}