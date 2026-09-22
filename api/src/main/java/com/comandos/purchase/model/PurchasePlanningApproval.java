package com.comandos.purchase.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_purchase_planning_approval")
public class PurchasePlanningApproval extends CoreEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "planning_id", nullable = false)
    public PurchasePlanning planning;

    @Column(name = "step_name", nullable = false, length = 160)
    public String stepName;

    @Column(name = "responsible_login", length = 160)
    public String responsibleLogin;

    @Column(name = "decision", length = 40)
    public String decision;

    @Column(name = "decided_at")
    public LocalDateTime decidedAt;

    @Column(name = "justification", length = 4000)
    public String justification;
}