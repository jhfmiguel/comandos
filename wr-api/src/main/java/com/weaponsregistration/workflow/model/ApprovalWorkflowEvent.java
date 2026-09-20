package com.weaponsregistration.workflow.model;
import com.weaponsregistration.core.model.CoreEntity;import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="erp_approval_workflow_event")public class ApprovalWorkflowEvent extends CoreEntity{
 @ManyToOne(optional=false)@JoinColumn(name="workflow_id",nullable=false)public ApprovalWorkflow workflow;
 @Column(nullable=false,length=30)public String fromStatus;@Column(nullable=false,length=30)public String toStatus;
 @Column(nullable=false,length=2000)public String justification;@Column(nullable=false)public LocalDateTime occurredAt;
 @Column public Long actorId;@Column public String actorLogin;
 @PreUpdate private void noUpdate(){throw new IllegalStateException("Workflow event history is immutable.");}
 @PreRemove private void noDelete(){throw new IllegalStateException("Workflow event history is immutable.");}
}