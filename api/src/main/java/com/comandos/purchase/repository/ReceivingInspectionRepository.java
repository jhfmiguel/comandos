package com.comandos.purchase.repository;
import com.comandos.purchase.model.ReceivingInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReceivingInspectionRepository extends JpaRepository<ReceivingInspection,Long>{List<ReceivingInspection> findByReceivingIdOrderByCreatedAtAsc(Long receivingId);}