package com.comandos.lifecycle.model;
import com.comandos.core.model.CoreEntity;import jakarta.persistence.*;
@Entity
@Table(name="erp_periodic_inspection_item",uniqueConstraints=@UniqueConstraint(name="uk_periodic_inspection_item_code",columnNames={"inspection_id","item_code"}))
public class PeriodicInspectionItem extends CoreEntity{
 @ManyToOne(optional=false)@JoinColumn(name="inspection_id",nullable=false)public PeriodicInspection inspection;
 @Column(name="item_order",nullable=false)public Integer itemOrder;
 @Column(name="item_code",nullable=false,length=80)public String code;
 @Column(nullable=false,length=255)public String label;
 @Column(name="required_flag",nullable=false)public Boolean required=true;
 @Column(nullable=false,length=30)public String result;
 @Column(length=1000)public String observation;
}
