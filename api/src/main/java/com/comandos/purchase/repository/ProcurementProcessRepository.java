package com.comandos.purchase.repository;
import com.comandos.purchase.model.ProcurementProcess;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProcurementProcessRepository extends JpaRepository<ProcurementProcess, Long> {}