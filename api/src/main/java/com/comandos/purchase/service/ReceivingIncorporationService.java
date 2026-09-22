package com.comandos.purchase.service;

import com.comandos.inventory.model.ItemModel;
import com.comandos.inventory.service.InventoryService;
import com.comandos.purchase.dto.ReceivingIncorporationContract;
import com.comandos.purchase.model.EquipmentReceiving;
import com.comandos.purchase.model.EquipmentReceivingItem;
import com.comandos.purchase.model.ReceivingIncorporation;
import com.comandos.purchase.model.ReceivingSerial;
import com.comandos.purchase.model.ReceivingStatus;
import com.comandos.purchase.repository.EquipmentReceivingRepository;
import com.comandos.purchase.repository.ReceivingIncorporationRepository;
import com.comandos.purchase.repository.ReceivingSerialRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
public class ReceivingIncorporationService {
    private final ReceivingIncorporationRepository repository;
    private final EquipmentReceivingRepository receivingRepository;
    private final ReceivingSerialRepository serialRepository;
    private final InventoryService inventoryService;
    private final EntityManager entityManager;

    public ReceivingIncorporationService(
        ReceivingIncorporationRepository repository,
        EquipmentReceivingRepository receivingRepository,
        ReceivingSerialRepository serialRepository,
        InventoryService inventoryService,
        EntityManager entityManager
    ) {
        this.repository = repository;
        this.receivingRepository = receivingRepository;
        this.serialRepository = serialRepository;
        this.inventoryService = inventoryService;
        this.entityManager = entityManager;
    }

    @Transactional
    public List<ReceivingIncorporationContract.View> list(Long receivingId) {
        List<ReceivingIncorporation> rows = receivingId == null
            ? repository.findAll()
            : repository.findByReceivingIdOrderByCreatedAtAsc(receivingId);
        return rows.stream().map(this::view).toList();
    }

    @Transactional
    public ReceivingIncorporationContract.View create(ReceivingIncorporationContract.CreateRequest request) {
        validateRequest(request);

        EquipmentReceiving receiving = entityManager.find(
            EquipmentReceiving.class, request.receivingId(), LockModeType.PESSIMISTIC_WRITE
        );
        if (receiving == null) throw new IllegalArgumentException("Receiving not found.");
        if (receiving.status != ReceivingStatus.DEFINITIVELY_ACCEPTED
            && receiving.status != ReceivingStatus.DEFINITIVELY_PARTIALLY_ACCEPTED) {
            throw new IllegalStateException(
                "Only definitively accepted quantities can be incorporated."
            );
        }

        EquipmentReceivingItem receivingItem = entityManager.find(
            EquipmentReceivingItem.class, request.receivingItemId(), LockModeType.PESSIMISTIC_WRITE
        );
        if (receivingItem == null || receivingItem.receiving == null || !receiving.id.equals(receivingItem.receiving.id)) {
            throw new IllegalArgumentException("Receiving item is invalid.");
        }

        ItemModel model = entityManager.find(ItemModel.class, receivingItem.itemModelId);
        if (model == null) throw new IllegalStateException("Item model no longer exists.");
        if (model.category == null) throw new IllegalStateException("Item model has no equipment category.");

        BigDecimal acceptedQuantity = nz(receivingItem.acceptedQuantity);
        BigDecimal incorporatedQuantity = incorporated(receivingItem.id);
        BigDecimal remainingQuantity = acceptedQuantity.subtract(incorporatedQuantity);
        if (remainingQuantity.signum() <= 0) {
            throw new IllegalStateException("Receiving item is already fully incorporated.");
        }

        BigDecimal incorporationValue = request.incorporationValue() == null
            ? receivingItem.acquisitionItem == null ? BigDecimal.ZERO : nz(receivingItem.acquisitionItem.unitPrice)
            : request.incorporationValue();
        if (incorporationValue.signum() < 0) {
            throw new IllegalArgumentException("Incorporation value cannot be negative.");
        }

        String initialCondition = clean(request.initialCondition());
        if (initialCondition == null) initialCondition = clean(receivingItem.conditionDescription);
        if (initialCondition == null) initialCondition = "GOOD";

        boolean serialized = Boolean.TRUE.equals(model.category.serialized);
        boolean consumable = Boolean.TRUE.equals(model.category.consumable);
        boolean lotControlled = Boolean.TRUE.equals(model.category.lotControlled);
        if (serialized && consumable) throw new IllegalStateException("Equipment category cannot be both serialized and consumable.");
        if (serialized && lotControlled) throw new IllegalStateException("Serialized equipment cannot use lot-controlled incorporation.");

        ReceivingIncorporation result = (consumable || lotControlled)
            ? incorporateLot(request, receiving, receivingItem, model, remainingQuantity, incorporationValue, initialCondition)
            : incorporateAsset(request, receiving, receivingItem, model, serialized, incorporatedQuantity, incorporationValue, initialCondition);

        entityManager.flush();
        return view(result);
    }

    private ReceivingIncorporation incorporateAsset(
        ReceivingIncorporationContract.CreateRequest request,
        EquipmentReceiving receiving,
        EquipmentReceivingItem receivingItem,
        ItemModel model,
        boolean serialized,
        BigDecimal incorporatedQuantity,
        BigDecimal incorporationValue,
        String initialCondition
    ) {
        ReceivingSerial receivingSerial = null;
        if (serialized) {
            if (request.receivingSerialId() == null) throw new IllegalArgumentException("Serial is required for serialized equipment.");
            receivingSerial = entityManager.find(ReceivingSerial.class, request.receivingSerialId(), LockModeType.PESSIMISTIC_WRITE);
            if (receivingSerial == null) throw new IllegalArgumentException("Receiving serial not found.");
            if (receivingSerial.receivingItem == null || !receivingItem.id.equals(receivingSerial.receivingItem.id)) {
                throw new IllegalArgumentException("Serial does not belong to receiving item.");
            }
            if (!Boolean.TRUE.equals(receivingSerial.accepted)) throw new IllegalStateException("Rejected serial cannot be incorporated.");
            if (repository.existsByReceivingSerialId(receivingSerial.id)) throw new IllegalStateException("Serial is already incorporated.");
        } else if (request.receivingSerialId() != null) {
            throw new IllegalArgumentException("Receiving serial cannot be informed for non-serialized equipment.");
        }

        BigDecimal requestedQuantity = request.quantity() == null ? BigDecimal.ONE : request.quantity();
        if (requestedQuantity.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("Durable assets must be incorporated individually with quantity equal to one.");
        }
        if (incorporatedQuantity.add(BigDecimal.ONE).compareTo(nz(receivingItem.acceptedQuantity)) > 0) {
            throw new IllegalStateException("Incorporation exceeds accepted quantity.");
        }

        long ordinal = incorporatedQuantity.longValueExact() + 1L;
        String assetCode = clean(request.assetCode());
        if (assetCode == null) {
            assetCode = String.format("PAT-%08d-%06d-%04d", receiving.id, receivingItem.id, ordinal);
        }

        var data = new HashMap<String, Object>();
        data.put("modelId", model.id);
        data.put("locationId", request.stockLocationId());
        data.put("assetCode", assetCode);
        data.put("serialNumber", receivingSerial == null ? null : receivingSerial.serialNumber);
        data.put("condition", initialCondition);
        data.put("status", "AVAILABLE");
        data.put("validUntil", receivingItem.expirationDate);
        data.put("currentValue", incorporationValue);

        Long inventoryRecordId = ((Number) inventoryService.save("assets", null, data).get("id")).longValue();
        if (receivingSerial != null) {
            receivingSerial.assetCode = assetCode;
            serialRepository.save(receivingSerial);
        }
        return saveIncorporation(request, receiving, receivingItem, receivingSerial, assetCode, null,
            BigDecimal.ONE, incorporationValue, initialCondition, "assets", inventoryRecordId);
    }

    private ReceivingIncorporation incorporateLot(
        ReceivingIncorporationContract.CreateRequest request,
        EquipmentReceiving receiving,
        EquipmentReceivingItem receivingItem,
        ItemModel model,
        BigDecimal remainingQuantity,
        BigDecimal incorporationValue,
        String initialCondition
    ) {
        if (request.receivingSerialId() != null) throw new IllegalArgumentException("Serial cannot be informed for consumable or lot-controlled equipment.");
        if (clean(request.assetCode()) != null) throw new IllegalArgumentException("Asset code cannot be informed for consumable or lot-controlled equipment.");

        BigDecimal quantity = request.quantity() == null ? remainingQuantity : request.quantity();
        if (quantity.signum() <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        if (quantity.compareTo(remainingQuantity) > 0) throw new IllegalStateException("Incorporation exceeds accepted quantity.");

        String lotNumber = clean(request.lotNumber());
        if (lotNumber == null) lotNumber = clean(receivingItem.lotNumber);
        if (lotNumber == null) lotNumber = "REC-" + receiving.id + "-" + receivingItem.id;

        var data = new HashMap<String, Object>();
        data.put("modelId", model.id);
        data.put("openingLocationId", request.stockLocationId());
        data.put("lotNumber", lotNumber);
        data.put("initialQuantity", quantity);
        data.put("validUntil", receivingItem.expirationDate);

        Long inventoryRecordId = ((Number) inventoryService.save("lots", null, data).get("id")).longValue();
        return saveIncorporation(request, receiving, receivingItem, null, null, lotNumber,
            quantity, incorporationValue, initialCondition, "lots", inventoryRecordId);
    }

    private ReceivingIncorporation saveIncorporation(
        ReceivingIncorporationContract.CreateRequest request,
        EquipmentReceiving receiving,
        EquipmentReceivingItem receivingItem,
        ReceivingSerial receivingSerial,
        String assetCode,
        String lotNumber,
        BigDecimal quantity,
        BigDecimal incorporationValue,
        String initialCondition,
        String inventoryResource,
        Long inventoryRecordId
    ) {
        ReceivingIncorporation x = new ReceivingIncorporation();
        x.receiving = receiving;
        x.receivingItem = receivingItem;
        x.receivingSerial = receivingSerial;
        x.stockLocationId = request.stockLocationId();
        x.assetCode = assetCode;
        x.lotNumber = lotNumber;
        x.quantity = quantity;
        x.incorporationValue = incorporationValue;
        x.initialCondition = initialCondition;
        x.inventoryResource = inventoryResource;
        x.inventoryRecordId = inventoryRecordId;
        x.incorporatedBy = clean(request.incorporatedBy());
        x.incorporatedAt = request.incorporatedAt() == null ? LocalDateTime.now() : request.incorporatedAt();
        x.notes = clean(request.notes());
        return repository.save(x);
    }

    private BigDecimal incorporated(Long receivingItemId) {
        return repository.findByReceivingItemIdOrderByCreatedAtAsc(receivingItemId).stream()
            .map(row -> nz(row.quantity)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ReceivingIncorporationContract.View view(ReceivingIncorporation x) {
        return new ReceivingIncorporationContract.View(
            x.id,
            x.receiving == null ? null : x.receiving.id,
            x.receivingItem == null ? null : x.receivingItem.id,
            x.receivingSerial == null ? null : x.receivingSerial.id,
            x.receivingSerial == null ? null : x.receivingSerial.serialNumber,
            x.stockLocationId,
            x.assetCode,
            x.lotNumber,
            x.quantity,
            x.incorporationValue,
            x.initialCondition,
            x.inventoryResource,
            x.inventoryRecordId,
            x.incorporatedBy,
            x.incorporatedAt,
            x.notes
        );
    }

    private static void validateRequest(ReceivingIncorporationContract.CreateRequest request) {
        if (request == null || request.receivingId() == null || request.receivingItemId() == null) {
            throw new IllegalArgumentException("Receiving and receiving item are required.");
        }
        if (request.stockLocationId() == null) throw new IllegalArgumentException("Stock location is required.");
    }

    private static BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static String clean(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
