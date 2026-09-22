package com.weaponsregistration.maintenance.model;
import com.weaponsregistration.core.model.*;import jakarta.persistence.*;
@Entity @Table(name="erp_maintenance_plan") public class MaintenancePlan extends CoreEntity{
 @ManyToOne(optional=false)@JoinColumn(name="organization_id",nullable=false)public Organization organization;@ManyToOne@JoinColumn(name="unit_id")public OrganizationalUnit unit;@Column(nullable=false)public String name;@Column(nullable=false)public String type;@Column(nullable=false)public Integer periodicityDays;@Column(nullable=false)public Boolean active=true;
}
