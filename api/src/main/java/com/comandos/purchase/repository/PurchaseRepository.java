package com.comandos.purchase.repository;
import com.comandos.purchase.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByBuyerOrganizationIdOrderByCreatedAtDesc(Long buyerOrganizationId);
}