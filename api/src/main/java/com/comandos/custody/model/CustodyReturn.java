package com.comandos.custody.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erp_custody_return")
public class CustodyReturn extends CoreEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "custody_id", nullable = false)
    public Custody custody;
    @Column(nullable = false, unique = true, length = 36) public String requestId;
    @Column(nullable = false, length = 64) public String requestFingerprint;
    @Column(nullable = false) public LocalDateTime returnedAt;
    @Column public Long returnedById;
    @Column public String returnedByLogin;
}
