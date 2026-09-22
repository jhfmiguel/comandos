package com.comandos.purchase.repository;
import com.comandos.purchase.model.EquipmentReceiving;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EquipmentReceivingRepository extends JpaRepository<EquipmentReceiving,Long>{List<EquipmentReceiving> findByAcquisitionIdOrderByCreatedAtAsc(Long acquisitionId);}