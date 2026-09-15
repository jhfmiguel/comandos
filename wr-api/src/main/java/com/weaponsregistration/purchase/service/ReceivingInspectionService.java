package com.weaponsregistration.purchase.service;

import com.weaponsregistration.purchase.dto.ReceivingInspectionContract;
import com.weaponsregistration.purchase.model.EquipmentReceiving;
import com.weaponsregistration.purchase.model.ReceivingInspection;
import com.weaponsregistration.purchase.model.ReceivingStatus;
import com.weaponsregistration.purchase.repository.EquipmentReceivingRepository;
import com.weaponsregistration.purchase.repository.ReceivingInspectionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReceivingInspectionService {
    private final ReceivingInspectionRepository inspectionRepository;
    private final EquipmentReceivingRepository receivingRepository;

    public ReceivingInspectionService(
        ReceivingInspectionRepository inspectionRepository,
        EquipmentReceivingRepository receivingRepository
    ) {
        this.inspectionRepository = inspectionRepository;
        this.receivingRepository = receivingRepository;
    }

    public List<ReceivingInspection> list(Long receivingId) {
        if (receivingId == null) {
            return inspectionRepository.findAll();
        }
        return inspectionRepository.findAll().stream()
            .filter(item -> item.receiving != null && receivingId.equals(item.receiving.id))
            .toList();
    }

    @Transactional
    public ReceivingInspection create(Long receivingId, ReceivingInspectionContract.CreateRequest request) {
        EquipmentReceiving receiving = receivingRepository.findById(receivingId)
            .orElseThrow(() -> new IllegalArgumentException("Receiving not found: " + receivingId));

        ReceivingInspection inspection = new ReceivingInspection();
        inspection.receiving = receiving;
        inspection.inspectedAt = request.inspectedAt() == null ? LocalDateTime.now() : request.inspectedAt();
        inspection.inspector = request.inspector();
        inspection.provisionalReceipt = Boolean.TRUE.equals(request.provisionalReceipt());
        inspection.definitiveReceipt = Boolean.TRUE.equals(request.definitiveReceipt());
        inspection.approved = Boolean.TRUE.equals(request.approved());
        inspection.nonConformity = request.nonConformity();
        inspection.decisionNotes = request.decisionNotes();
        inspection = inspectionRepository.save(inspection);

        receiving.status = resolveStatus(inspection);
        receivingRepository.save(receiving);
        return inspection;
    }

    private ReceivingStatus resolveStatus(ReceivingInspection inspection) {
        if (!inspection.approved) {
            return ReceivingStatus.REJECTED;
        }
        if (inspection.definitiveReceipt) {
            return ReceivingStatus.DEFINITIVELY_ACCEPTED;
        }
        if (inspection.provisionalReceipt) {
            return ReceivingStatus.PROVISIONALLY_ACCEPTED;
        }
        return ReceivingStatus.UNDER_INSPECTION;
    }
}