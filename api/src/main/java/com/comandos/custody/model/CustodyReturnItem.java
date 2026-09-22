package com.comandos.custody.model;

import com.comandos.core.model.CoreEntity;
import com.comandos.inventory.model.StockMovement;
import jakarta.persistence.*;

@Entity
@Table(name = "erp_custody_return_item", uniqueConstraints = @UniqueConstraint(columnNames = {"return_id", "custody_item_id"}))
public class CustodyReturnItem extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "return_id", nullable = false)
    public CustodyReturn custodyReturn;
    @ManyToOne(optional = false) @JoinColumn(name = "custody_item_id", nullable = false)
    public CustodyItem custodyItem;
    @OneToOne(optional = false) @JoinColumn(name = "movement_id", nullable = false, unique = true)
    public StockMovement movement;
    @ManyToOne @JoinColumn(name = "condition_type_id")
    public CustodyReturnConditionType conditionType;
    @Column(length = 500) public String inspectionNotes;
    @Column public Long inspectedById;
    @Column public String inspectedByLogin;
}
