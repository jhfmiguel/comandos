package com.weaponsregistration.purchase.repository;
import com.weaponsregistration.purchase.model.ReceivingInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReceivingInspectionRepository extends JpaRepository<ReceivingInspection,Long>{List<ReceivingInspection> findByReceivingIdOrderByCreatedAtAsc(Long receivingId);}