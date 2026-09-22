package com.comandos.purchase.repository;

import com.comandos.purchase.model.ReceivingSerial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceivingSerialRepository extends JpaRepository<ReceivingSerial, Long> {
    List<ReceivingSerial> findByReceivingItemIdOrderByCreatedAtAsc(Long receivingItemId);
    boolean existsBySerialNumberIgnoreCase(String serialNumber);
}
