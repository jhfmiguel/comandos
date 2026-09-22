package com.comandos.purchase.repository;

import com.comandos.purchase.model.PurchasePlanning;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchasePlanningRepository extends JpaRepository<PurchasePlanning, Long> {
    List<PurchasePlanning> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
}