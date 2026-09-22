package com.weaponsregistration.purchase.repository;

import com.weaponsregistration.purchase.model.PurchaseItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PurchaseItem i where i.id = :id")
    Optional<PurchaseItem> findByIdForUpdate(@Param("id") Long id);
}
