package com.weaponsregistration.purchase.repository;
import com.weaponsregistration.purchase.model.EquipmentReceivingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EquipmentReceivingItemRepository extends JpaRepository<EquipmentReceivingItem,Long>{List<EquipmentReceivingItem> findByReceivingIdOrderByCreatedAtAsc(Long receivingId);}