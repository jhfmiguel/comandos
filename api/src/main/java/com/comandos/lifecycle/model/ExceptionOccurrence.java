package com.comandos.lifecycle.model;
import com.comandos.core.model.*;import com.comandos.inventory.model.*;import jakarta.persistence.*;import java.math.BigDecimal;import java.time.LocalDateTime;
@Entity @Table(name="erp_exception_occurrence") public class ExceptionOccurrence extends CoreEntity{
 @ManyToOne(optional=false)@JoinColumn(name="organization_id")public Organization organization;@ManyToOne@JoinColumn(name="unit_id")public OrganizationalUnit unit;
 @ManyToOne@JoinColumn(name="asset_id")public AssetItem asset;@ManyToOne@JoinColumn(name="lot_id")public StockLot lot;@ManyToOne@JoinColumn(name="balance_id")public StockBalance balance;
 @Column(nullable=false,length=40)public String type;@Column(nullable=false,length=30)public String status="OPEN";@Column(precision=19,scale=4)public BigDecimal quantity;
 @Column(nullable=false,length=2000)public String description;@Column(length=2000)public String investigation;@Column(length=1000)public String documentReference;
 @Column(nullable=false)public LocalDateTime occurredAt;@Column(nullable=false)public Long responsibleId;@Column(nullable=false)public String responsibleLogin;
 @Column public LocalDateTime resolvedAt;@Column public Long resolvedById;@Column public String resolvedByLogin;
}