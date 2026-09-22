package com.weaponsregistration.purchase.repository;
import com.weaponsregistration.purchase.model.EquipmentReceiving;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EquipmentReceivingRepository extends JpaRepository<EquipmentReceiving,Long>{List<EquipmentReceiving> findByAcquisitionIdOrderByCreatedAtAsc(Long acquisitionId);}