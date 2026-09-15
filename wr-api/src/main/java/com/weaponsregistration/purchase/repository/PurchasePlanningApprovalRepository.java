package com.weaponsregistration.purchase.repository;

import com.weaponsregistration.purchase.model.PurchasePlanningApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchasePlanningApprovalRepository extends JpaRepository<PurchasePlanningApproval, Long> {
    List<PurchasePlanningApproval> findByPlanningIdOrderByCreatedAtAsc(Long planningId);
}