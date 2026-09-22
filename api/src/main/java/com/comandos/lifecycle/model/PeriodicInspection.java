package com.comandos.lifecycle.model;
import com.comandos.core.model.*;import com.comandos.inventory.model.*;import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="erp_periodic_inspection") public class PeriodicInspection extends CoreEntity{
 @ManyToOne(optional=false)@JoinColumn(name="organization_id")public Organization organization;@ManyToOne@JoinColumn(name="unit_id")public OrganizationalUnit unit;
 @ManyToOne(optional=false)@JoinColumn(name="asset_id")public AssetItem asset;@Column(nullable=false,length=2000)public String checklist;
 @Column(nullable=false,length=30)public String result;@Column(length=2000)public String damages;@Column(nullable=false)public LocalDateTime inspectedAt;
 @Column(nullable=false)public Long responsibleId;@Column(nullable=false)public String responsibleLogin;@Column public Long approvedById;@Column public String approvedByLogin;
 @Column public LocalDateTime approvedAt;@Column public Long generatedWorkOrderId;
}