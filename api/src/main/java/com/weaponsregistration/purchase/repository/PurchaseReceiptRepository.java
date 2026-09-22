package com.weaponsregistration.purchase.repository;
import com.weaponsregistration.purchase.model.PurchaseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long> {
    List<PurchaseReceipt> findByPurchaseIdOrderByReceiptDateDesc(Long purchaseId);
}