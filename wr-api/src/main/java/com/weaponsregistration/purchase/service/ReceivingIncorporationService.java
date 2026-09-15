package com.weaponsregistration.purchase.service;

import com.weaponsregistration.purchase.dto.ReceivingIncorporationContract;
import com.weaponsregistration.purchase.model.*;
import com.weaponsregistration.purchase.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReceivingIncorporationService {
    private final ReceivingIncorporationRepository repository;
    private final EquipmentReceivingRepository receivingRepository;
    private final EquipmentReceivingItemRepository itemRepository;
    private final ReceivingSerialRepository serialRepository;

    public ReceivingIncorporationService(
        ReceivingIncorporationRepository repository,
        EquipmentReceivingRepository receivingRepository,
        EquipmentReceivingItemRepository itemRepository,
        ReceivingSerialRepository serialRepository
    ) {
        this.repository = repository;
        this.receivingRepository = receivingRepository;
        this.itemRepository = itemRepository;
        this.serialRepository = serialRepository;
    }

    public List<ReceivingIncorporation> list(Long receivingId) {
        return receivingId == null
            ? repository.findAll()
            : repository.findByReceivingIdOrderByCreatedAtAsc(receivingId);
    }

    @Transactional
    public ReceivingIncorporation create(ReceivingIncorporationContract.CreateRequest request) {
        if (request.receivingId() == null || request.receivingItemId() == null) {
            throw new IllegalArgumentException("Receiving and receiving item are required.");
        }
        if (request.stockLocationId() == null) {
            throw new IllegalArgumentException("Stock location is required.");
        }

        EquipmentReceiving receiving = receivingRepository.findById(request.receivingId())
            .orElseThrow(() -> new IllegalArgumentException("Receiving not found: " + request.receivingId()));

        if (receiving.status != ReceivingStatus.DEFINITIVELY_ACCEPTED) {
            throw new IllegalStateException(
                "Only definitively accepted receiving records can be incorporated."
            );
        }

        EquipmentReceivingItem item = itemRepository.findById(request.receivingItemId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Receiving item not found: " + request.receivingItemId()
            ));

        if (item.receiving == null || !receiving.id.equals(item.receiving.id)) {
            throw new IllegalArgumentException("The item does not belong to the selected receiving.");
        }

        ReceivingSerial serial = null;
        if (request.receivingSerialId() != null) {
            serial = serialRepository.findById(request.receivingSerialId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Receiving serial not found: " + request.receivingSerialId()
                ));

            if (serial.receivingItem == null || !item.id.equals(serial.receivingItem.id)) {
                throw new IllegalArgumentException("The serial does not belong to the selected item.");
            }
            if (!serial.accepted) {
                throw new IllegalStateException("Rejected serial numbers cannot be incorporated.");
            }
            if (repository.existsByReceivingSerialId(serial.id)) {
                throw new IllegalStateException("This serial number has already been incorporated.");
            }
        }

        ReceivingIncorporation entity = new ReceivingIncorporation();
        entity.receiving = receiving;
        entity.receivingItem = item;
        entity.receivingSerial = serial;
        entity.stockLocationId = request.stockLocationId();
        entity.assetCode = normalize(request.assetCode());
        entity.lotNumber = normalize(request.lotNumber());
        entity.incorporatedBy = normalize(request.incorporatedBy());
        entity.incorporatedAt = request.incorporatedAt() == null
            ? LocalDateTime.now()
            : request.incorporatedAt();
        entity.notes = normalize(request.notes());
        return repository.save(entity);
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}