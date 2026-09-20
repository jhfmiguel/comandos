package com.weaponsregistration.purchase.service;

import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.inventory.model.ItemModel;
import com.weaponsregistration.purchase.dto.EquipmentReceivingContract;
import com.weaponsregistration.purchase.model.*;
import com.weaponsregistration.purchase.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EquipmentReceivingService {
    private final EquipmentReceivingRepository receivingRepository;
    private final EquipmentReceivingItemRepository itemRepository;
    private final ReceivingSerialRepository serialRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final EntityManager em;

    public EquipmentReceivingService(
        EquipmentReceivingRepository receivingRepository,
        EquipmentReceivingItemRepository itemRepository,
        ReceivingSerialRepository serialRepository,
        PurchaseRepository purchaseRepository,
        PurchaseItemRepository purchaseItemRepository,
        EntityManager em
    ) {
        this.receivingRepository = receivingRepository;
        this.itemRepository = itemRepository;
        this.serialRepository = serialRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.em = em;
    }

    public List<EquipmentReceivingContract.View> list(Long acquisitionId) {
        List<EquipmentReceiving> values = acquisitionId == null
            ? receivingRepository.findAll()
            : receivingRepository.findByAcquisitionIdOrderByCreatedAtAsc(acquisitionId);
        return values.stream().map(this::view).toList();
    }

    public EquipmentReceiving getEntity(Long id) {
        return receivingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Receiving not found: " + id));
    }

    public EquipmentReceivingContract.View get(Long id) {
        return view(getEntity(id));
    }

    @Transactional
    public EquipmentReceivingContract.View create(EquipmentReceivingContract.CreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Receiving request is required.");
        }

        ReceivingSourceType source = request.sourceType() == null
            ? ReceivingSourceType.ACQUISITION
            : request.sourceType();

        if (source == ReceivingSourceType.ACQUISITION && request.acquisitionId() == null) {
            throw new IllegalArgumentException("Acquisition is required when receiving source is ACQUISITION.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("At least one receiving item is required.");
        }

        Purchase acquisition = request.acquisitionId() == null
            ? null
            : purchaseRepository.findById(request.acquisitionId())
                .orElseThrow(() -> new IllegalArgumentException("Acquisition not found: " + request.acquisitionId()));

        if (acquisition != null && acquisition.status == PurchaseStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled acquisition cannot receive items.");
        }

        EquipmentReceiving receiving = new EquipmentReceiving();
        receiving.sourceType = source;
        receiving.acquisition = acquisition;

        if (request.receivingOrganizationId() != null) {
            Organization organization = em.find(Organization.class, request.receivingOrganizationId());
            if (organization == null) {
                throw new IllegalArgumentException("Organization not found: " + request.receivingOrganizationId());
            }
            receiving.receivingOrganization = organization;
        }

        receiving.receivingUnit = clean(request.receivingUnit());
        receiving.receivingLocation = clean(request.receivingLocation());
        receiving.deliveryDocumentNumber = clean(request.deliveryDocumentNumber());
        receiving.invoiceNumber = clean(request.invoiceNumber());
        receiving.receivedAt = request.receivedAt() == null ? LocalDateTime.now() : request.receivedAt();
        receiving.receivedBy = clean(request.receivedBy());
        receiving.physicalChecked = Boolean.TRUE.equals(request.physicalChecked());
        receiving.documentsChecked = Boolean.TRUE.equals(request.documentsChecked());
        receiving.notes = clean(request.notes());
        receiving.status = ReceivingStatus.RECEIVED;
        receiving = receivingRepository.save(receiving);

        boolean thisDeliveryHasQuantityDivergence = false;
        Set<Long> acquisitionItemsInRequest = new HashSet<>();

        for (EquipmentReceivingContract.ItemRequest input : request.items()) {
            if (input.receivedQuantity() == null || input.receivedQuantity().signum() <= 0) {
                throw new IllegalArgumentException("Received quantity must be greater than zero.");
            }

            PurchaseItem acquisitionItem = null;
            ItemModel model;
            BigDecimal expected;

            if (source == ReceivingSourceType.ACQUISITION) {
                if (input.acquisitionItemId() == null) {
                    throw new IllegalArgumentException("Acquisition item is required.");
                }
                if (!acquisitionItemsInRequest.add(input.acquisitionItemId())) {
                    throw new IllegalArgumentException("Acquisition item cannot be repeated in the same receiving.");
                }

                acquisitionItem = purchaseItemRepository.findByIdForUpdate(input.acquisitionItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Acquisition item not found: " + input.acquisitionItemId()
                    ));

                if (acquisitionItem.purchase == null || acquisition == null
                    || !acquisition.id.equals(acquisitionItem.purchase.id)) {
                    throw new IllegalArgumentException(
                        "Acquisition item does not belong to selected acquisition."
                    );
                }

                model = acquisitionItem.itemModel;
                expected = acquisitionItem.pendingQuantity();

                if (expected.signum() <= 0) {
                    throw new IllegalStateException("Acquisition item has no pending quantity.");
                }
                if (input.receivedQuantity().compareTo(expected) > 0) {
                    throw new IllegalArgumentException(
                        "Received quantity exceeds acquisition pending quantity."
                    );
                }
                if (input.itemModelId() != null && !model.id.equals(input.itemModelId())) {
                    throw new IllegalArgumentException(
                        "Received model differs from acquired item model."
                    );
                }
            } else {
                if (input.itemModelId() == null) {
                    throw new IllegalArgumentException("Item model is required.");
                }
                model = em.find(ItemModel.class, input.itemModelId());
                if (model == null) {
                    throw new IllegalArgumentException("Item model not found: " + input.itemModelId());
                }
                expected = input.expectedQuantity();
            }

            List<String> normalizedSerials = input.serialNumbers() == null
                ? List.of()
                : input.serialNumbers().stream()
                    .map(EquipmentReceivingService::clean)
                    .filter(v -> v != null)
                    .toList();

            if (Boolean.TRUE.equals(model.category.serialized)) {
                if (input.receivedQuantity().stripTrailingZeros().scale() > 0
                    || input.receivedQuantity().longValueExact() != normalizedSerials.size()) {
                    throw new IllegalArgumentException(
                        "Serialized item quantity must equal serial count."
                    );
                }
            }

            Set<String> localSerials = new HashSet<>();
            for (String serial : normalizedSerials) {
                String key = serial.toLowerCase();
                if (!localSerials.add(key)) {
                    throw new IllegalArgumentException(
                        "Duplicate serial number in receiving: " + serial
                    );
                }
                if (serialRepository.existsBySerialNumberIgnoreCase(serial)) {
                    throw new IllegalArgumentException(
                        "Serial number was already received: " + serial
                    );
                }
            }

            EquipmentReceivingItem item = new EquipmentReceivingItem();
            item.receiving = receiving;
            item.acquisitionItem = acquisitionItem;
            item.itemModelId = model.id;
            item.expectedQuantity = expected;
            item.receivedQuantity = input.receivedQuantity();
            item.acceptedQuantity = BigDecimal.ZERO;
            item.rejectedQuantity = BigDecimal.ZERO;
            item.lotNumber = clean(input.lotNumber());
            item.manufactureDate = input.manufactureDate();
            item.expirationDate = input.expirationDate();
            item.conditionDescription = clean(input.conditionDescription());
            item.notes = clean(input.notes());

            if (expected != null && input.receivedQuantity().compareTo(expected) != 0) {
                item.divergenceDescription =
                    "Expected " + expected.toPlainString()
                    + ", received " + input.receivedQuantity().toPlainString() + ".";
                thisDeliveryHasQuantityDivergence = true;
            }

            item = itemRepository.save(item);

            for (String serial : normalizedSerials) {
                ReceivingSerial receivingSerial = new ReceivingSerial();
                receivingSerial.receivingItem = item;
                receivingSerial.serialNumber = serial;
                receivingSerial.accepted = true;
                serialRepository.save(receivingSerial);
            }

            if (acquisitionItem != null) {
                acquisitionItem.receivedQuantity =
                    nz(acquisitionItem.receivedQuantity).add(input.receivedQuantity());
                purchaseItemRepository.save(acquisitionItem);
            }
        }

        if (source == ReceivingSourceType.ACQUISITION) {
            updateAcquisitionReceivingStatus(acquisition);
            receiving.status = hasPendingQuantity(acquisition)
                ? ReceivingStatus.PARTIALLY_RECEIVED
                : ReceivingStatus.RECEIVED;
        } else {
            receiving.status = thisDeliveryHasQuantityDivergence
                ? ReceivingStatus.PARTIALLY_RECEIVED
                : ReceivingStatus.RECEIVED;
        }

        return view(receivingRepository.save(receiving));
    }

    @Transactional
    public EquipmentReceivingContract.View changeStatus(
        Long id,
        EquipmentReceivingContract.StatusRequest request
    ) {
        EquipmentReceiving receiving = getEntity(id);
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("Status is required.");
        }

        if (request.status() == ReceivingStatus.PROVISIONALLY_ACCEPTED
            || request.status() == ReceivingStatus.DEFINITIVELY_ACCEPTED
            || request.status() == ReceivingStatus.PARTIALLY_REJECTED
            || request.status() == ReceivingStatus.REJECTED) {
            throw new IllegalArgumentException(
                "Acceptance or rejection status must be defined by inspection."
            );
        }

        receiving.status = request.status();
        if (request.notes() != null && !request.notes().isBlank()) {
            receiving.notes = request.notes().trim();
        }
        return view(receivingRepository.save(receiving));
    }

    @Transactional
    public void updateAcquisitionReceivingStatus(Purchase acquisition) {
        if (acquisition == null) {
            return;
        }
        acquisition.status = hasPendingQuantity(acquisition)
            ? PurchaseStatus.PARTIALLY_RECEIVED
            : PurchaseStatus.RECEIVED;
        purchaseRepository.save(acquisition);
    }

    private boolean hasPendingQuantity(Purchase acquisition) {
        return acquisition.items.stream().anyMatch(
            item -> item.pendingQuantity().signum() > 0
        );
    }

    private EquipmentReceivingContract.View view(EquipmentReceiving receiving) {
        List<EquipmentReceivingContract.ItemView> itemViews =
            itemRepository.findByReceivingIdOrderByCreatedAtAsc(receiving.id).stream()
                .map(item -> {
                    List<EquipmentReceivingContract.SerialView> serials =
                        serialRepository.findByReceivingItemIdOrderByCreatedAtAsc(item.id).stream()
                            .map(serial -> new EquipmentReceivingContract.SerialView(
                                serial.id,
                                serial.serialNumber,
                                serial.manufacturerCode,
                                serial.assetCode,
                                serial.accepted,
                                serial.rejectionReason
                            ))
                            .toList();

                    PurchaseItem acquisitionItem = item.acquisitionItem;
                    return new EquipmentReceivingContract.ItemView(
                        item.id,
                        acquisitionItem == null ? null : acquisitionItem.id,
                        item.itemModelId,
                        acquisitionItem == null || acquisitionItem.itemModel == null
                            ? null : acquisitionItem.itemModel.name,
                        acquisitionItem == null ? null : acquisitionItem.quantity,
                        item.expectedQuantity,
                        item.receivedQuantity,
                        nz(item.acceptedQuantity),
                        nz(item.rejectedQuantity),
                        acquisitionItem == null ? null : acquisitionItem.pendingQuantity(),
                        item.lotNumber,
                        item.manufactureDate,
                        item.expirationDate,
                        item.conditionDescription,
                        item.divergenceDescription,
                        item.notes,
                        serials
                    );
                })
                .toList();

        return new EquipmentReceivingContract.View(
            receiving.id,
            receiving.sourceType,
            receiving.acquisition == null ? null : receiving.acquisition.id,
            receiving.acquisition == null ? null : receiving.acquisition.purchaseNumber,
            receiving.receivingOrganization == null ? null : receiving.receivingOrganization.id,
            receiving.receivingOrganization == null ? null : receiving.receivingOrganization.name,
            receiving.receivingUnit,
            receiving.receivingLocation,
            receiving.deliveryDocumentNumber,
            receiving.invoiceNumber,
            receiving.receivedAt,
            receiving.receivedBy,
            receiving.physicalChecked,
            receiving.documentsChecked,
            receiving.status,
            receiving.notes,
            itemViews
        );
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
