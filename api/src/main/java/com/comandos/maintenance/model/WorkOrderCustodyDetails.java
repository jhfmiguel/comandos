package com.comandos.maintenance.model;
import com.comandos.custody.model.CustodyReturnItem;
import jakarta.persistence.*;

/** Custody origin stored in the existing work-order row. */
@Entity
@Table(name = "erp_work_order")
public class WorkOrderCustodyDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") public Long id;
    @OneToOne @JoinColumn(name = "custody_return_item_id", unique = true)
    public CustodyReturnItem custodyReturnItem;
}