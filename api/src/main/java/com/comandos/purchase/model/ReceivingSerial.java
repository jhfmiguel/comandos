package com.comandos.purchase.model;
import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
@Entity
@Table(name="erp_equipment_receiving_serial",uniqueConstraints=@UniqueConstraint(name="uk_receiving_serial_number",columnNames={"receiving_item_id","serial_number"}))
public class ReceivingSerial extends CoreEntity {
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="receiving_item_id",nullable=false) public EquipmentReceivingItem receivingItem;
 @Column(name="serial_number",nullable=false,length=255) public String serialNumber;
 @Column(name="manufacturer_code",length=255) public String manufacturerCode;
 @Column(name="asset_code",length=255) public String assetCode;
 @Column(name="accepted",nullable=false) public Boolean accepted=true;
 @Column(name="rejection_reason",length=2000) public String rejectionReason;
}