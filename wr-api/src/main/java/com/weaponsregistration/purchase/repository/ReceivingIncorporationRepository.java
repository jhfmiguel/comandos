package com.weaponsregistration.purchase.repository;

import com.weaponsregistration.purchase.model.ReceivingIncorporation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReceivingIncorporationRepository extends JpaRepository<ReceivingIncorporation, Long> {
    boolean existsByReceivingSerialId(Long receivingSerialId);
    List<ReceivingIncorporation> findByReceivingIdOrderByCreatedAtAsc(Long receivingId);
}