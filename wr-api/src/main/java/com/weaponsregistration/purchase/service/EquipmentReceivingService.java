package com.weaponsregistration.purchase.service;

import com.weaponsregistration.core.model.Organization;
import jakarta.persistence.EntityManager;
import com.weaponsregistration.purchase.dto.EquipmentReceivingContract;
import com.weaponsregistration.purchase.model.*;
import com.weaponsregistration.purchase.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipmentReceivingService {
    private final EquipmentReceivingRepository receivingRepository;
    private final EquipmentReceivingItemRepository itemRepository;
    private final ReceivingSerialRepository serialRepository;
    private final PurchaseRepository purchaseRepository;
    private final EntityManager entityManager;

    public EquipmentReceivingService(
        EquipmentReceivingRepository receivingRepository,
        EquipmentReceivingItemRepository itemRepository,
        ReceivingSerialRepository serialRepository,
        PurchaseRepository purchaseRepository,
        EntityManager entityManager
    ) {
        this.receivingRepository = receivingRepository;
        this.itemRepository = itemRepository;
        this.serialRepository = serialRepository;
        this.purchaseRepository = purchaseRepository;
        this.entityManager = entityManager;
    }

    public List<EquipmentReceiving> list(Long acquisitionId) {
        return acquisitionId == null
            ? receivingRepository.findAll()
            : receivingRepository.findByAcquisitionIdOrderByCreatedAtAsc(acquisitionId);
    }

    public EquipmentReceiving get(Long id) {
        return receivingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Receiving not found: " + id));
    }

    @Transactional
    public EquipmentReceiving create(EquipmentReceivingContract.CreateRequest request) {
        ReceivingSourceType source = request.sourceType() == null ? ReceivingSourceType.ACQUISITION : request.sourceType();
        if (source == ReceivingSourceType.ACQUISITION && request.acquisitionId() == null) {
            throw new IllegalArgumentException("Acquisition is required when receiving source is ACQUISITION.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("At least one receiving item is required.");
        }

        EquipmentReceiving entity = new EquipmentReceiving();
        entity.sourceType = source;
        if (request.acquisitionId() != null) {
            entity.acquisition = purchaseRepository.findById(request.acquisitionId())
                .orElseThrow(() -> new IllegalArgumentException("Acquisition not found: " + request.acquisitionId()));
        }
        if (request.receivingOrganizationId() != null) {
            Organization organization = entityManager.find(Organization.class, request.receivingOrganizationId());
            if (organization == null) {
                throw new IllegalArgumentException("Organization not found: " + request.receivingOrganizationId());
            }
            entity.receivingOrganization = organization;
        }
        entity.receivingUnit = request.receivingUnit();
        entity.receivingLocation = request.receivingLocation();
        entity.deliveryDocumentNumber = request.deliveryDocumentNumber();
        entity.invoiceNumber = request.invoiceNumber();
        entity.receivedAt = request.receivedAt() == null ? LocalDateTime.now() : request.receivedAt();
        entity.receivedBy = request.receivedBy();
        entity.notes = request.notes();
        entity.status = ReceivingStatus.RECEIVED;
        entity = receivingRepository.save(entity);

        for (EquipmentReceivingContract.ItemRequest input : request.items()) {
            if (input.receivedQuantity() == null || input.receivedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Received quantity must be greater than zero.");
            }
            EquipmentReceivingItem item = new EquipmentReceivingItem();
            item.receiving = entity;
            if (input.acquisitionItemId() != null) {
                item.acquisitionItem = null; // linked after legacy PurchaseItem repository is normalized
            }
            item.itemModelId = input.itemModelId();
            item.expectedQuantity = input.expectedQuantity();
            item.receivedQuantity = input.receivedQuantity();
            item.acceptedQuantity = BigDecimal.ZERO;
            item.rejectedQuantity = BigDecimal.ZERO;
            item.lotNumber = input.lotNumber();
            item.manufactureDate = input.manufactureDate();
            item.expirationDate = input.expirationDate();
            item.conditionDescription = input.conditionDescription();
            item.notes = input.notes();
            item = itemRepository.save(item);

            if (input.serialNumbers() != null) {
                for (String value : input.serialNumbers()) {
                    if (value == null || value.isBlank()) continue;
                    ReceivingSerial serial = new ReceivingSerial();
                    serial.receivingItem = item;
                    serial.serialNumber = value.trim();
                    serialRepository.save(serial);
                }
            }
        }
        return entity;
    }

    @Transactional
    public EquipmentReceiving changeStatus(Long id, EquipmentReceivingContract.StatusRequest request) {
        EquipmentReceiving entity = get(id);
        if (request.status() == null) throw new IllegalArgumentException("Status is required.");
        entity.status = request.status();
        if (request.notes() != null && !request.notes().isBlank()) entity.notes = request.notes();
        return receivingRepository.save(entity);
    }
}