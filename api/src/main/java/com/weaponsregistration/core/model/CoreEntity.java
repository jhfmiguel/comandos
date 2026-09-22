package com.weaponsregistration.core.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class CoreEntity {
    
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
   
	@Version @Column(nullable = false)
    public Long version;
    
	@Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;
   
	@Column(nullable = false)
    public LocalDateTime updatedAt;

    @PrePersist
    void created() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void updated() {
        updatedAt = LocalDateTime.now();
    }
    
}
