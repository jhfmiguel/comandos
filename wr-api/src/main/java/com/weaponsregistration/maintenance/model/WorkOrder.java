package com.weaponsregistration.maintenance.model;
import com.weaponsregistration.core.model.*;import com.weaponsregistration.custody.model.CustodyReturnItem;import com.weaponsregistration.inventory.model.*;import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="erp_work_order") public class WorkOrder extends CoreEntity{
 @ManyToOne(optional=false)@JoinColumn(name="organization_id",nullable=false)public Organization organization;@ManyToOne@JoinColumn(name="unit_id")public OrganizationalUnit unit;@ManyToOne@JoinColumn(name="plan_id")public MaintenancePlan plan;@ManyToOne(optional=false)@JoinColumn(name="asset_id",nullable=false)public AssetItem asset;@OneToOne(optional=false)@JoinColumn(name="issue_movement_id",nullable=false,unique=true)public StockMovement issueMovement;@OneToOne@JoinColumn(name="return_movement_id",unique=true)public StockMovement returnMovement;
 @OneToOne@JoinColumn(name="custody_return_item_id",unique=true)public CustodyReturnItem custodyReturnItem;
 @Column(nullable=false)public String organizationName;@Column public String unitName;@Column(nullable=false)public String assetCode;@Column(nullable=false)public String modelName;@Column(nullable=false)public String locationName;@Column(nullable=false)public String reason;
 @Column(name="maintenance_type",nullable=false,length=30)public String maintenanceType="CORRECTIVE";
 @Column(name="workshop",length=255)public String workshop;
 @Column(name="gunsmith",length=255)public String gunsmith;
 @Column(name="sent_at")public LocalDateTime sentAt;
 @Column(name="returned_at")public LocalDateTime returnedAt;
 @Column(name="next_maintenance_at")public LocalDateTime nextMaintenanceAt;
 @Column(name="total_cost",precision=19,scale=4)public java.math.BigDecimal totalCost=java.math.BigDecimal.ZERO;
 @Column(nullable=false)public String status="OPEN";@Column(nullable=false)public LocalDateTime openedAt;@Column public LocalDateTime completedAt;@Column public Long openedById;@Column public String openedByLogin;@Column public Long completedById;@Column public String completedByLogin;@Column(nullable=false,unique=true,length=36)public String requestId;@Column(nullable=false,length=64)public String requestFingerprint;@Column(unique=true,length=36)public String completionRequestId;@Column(length=64)public String completionFingerprint;
}
