package com.weaponsregistration.purchase.repository;
import com.weaponsregistration.purchase.model.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {}
