package com.weaponsregistration.transfer.model;
import com.weaponsregistration.core.model.*;import com.weaponsregistration.inventory.model.StockLocation;import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="erp_inventory_transfer") public class InventoryTransfer extends CoreEntity {
 @ManyToOne(optional=false)@JoinColumn(name="organization_id",nullable=false)public Organization organization;
 @ManyToOne(optional=false)@JoinColumn(name="source_unit_id",nullable=false)public OrganizationalUnit sourceUnit;
 @ManyToOne(optional=false)@JoinColumn(name="destination_unit_id",nullable=false)public OrganizationalUnit destinationUnit;
 @ManyToOne(optional=false)@JoinColumn(name="destination_location_id",nullable=false)public StockLocation destinationLocation;
 @Column(nullable=false)public String organizationName;@Column(nullable=false)public String sourceUnitName;@Column(nullable=false)public String destinationUnitName;@Column(nullable=false)public String destinationLocationName;
 @Column(nullable=false,length=255)public String purpose;@Column(nullable=false,length=30)public String status="FINALIZED";@Column(nullable=false)public LocalDateTime sentAt;@Column public Long finalizedById;@Column public String finalizedByLogin;@Column(nullable=false,unique=true,length=36)public String requestId;@Column(nullable=false,length=64)public String requestFingerprint;
}
