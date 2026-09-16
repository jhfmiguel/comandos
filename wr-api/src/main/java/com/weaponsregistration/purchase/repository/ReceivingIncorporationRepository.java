package com.weaponsregistration.purchase.repository;
import com.weaponsregistration.purchase.model.ReceivingIncorporation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReceivingIncorporationRepository extends JpaRepository<ReceivingIncorporation,Long>{boolean existsByReceivingSerialId(Long id);List<ReceivingIncorporation> findByReceivingIdOrderByCreatedAtAsc(Long id);List<ReceivingIncorporation> findByReceivingItemIdOrderByCreatedAtAsc(Long id);}
