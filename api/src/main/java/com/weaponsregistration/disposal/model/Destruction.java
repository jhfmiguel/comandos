package com.weaponsregistration.disposal.model;

import com.weaponsregistration.core.model.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_destruction")
public class Destruction extends CoreEntity {
    @OneToOne(optional = false) @JoinColumn(name = "process_id", nullable = false, unique = true) public DisposalProcess process;
    @Column(nullable = false) public String method;
    @Column(nullable = false) public LocalDateTime destroyedAt;
    @Column(nullable = false) public String certificate;
}
