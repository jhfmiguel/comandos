package com.weaponsregistration.purchase.repository;

import com.weaponsregistration.purchase.model.PurchasePlanningDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchasePlanningDocumentRepository extends JpaRepository<PurchasePlanningDocument, Long> {
    List<PurchasePlanningDocument> findByPlanningIdOrderByCreatedAtAsc(Long planningId);
}