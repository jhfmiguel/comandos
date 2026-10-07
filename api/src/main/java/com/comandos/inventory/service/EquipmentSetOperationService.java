package com.comandos.inventory.service;

import com.comandos.audit.service.AuditService;
import com.comandos.consumption.service.ConsumableUsageService;
import com.comandos.disposal.service.DisposalService;
import com.comandos.donation.service.DonationService;
import com.comandos.inventory.dto.EquipmentSetOperationContract.*;
import com.comandos.inventory.model.EquipmentSet;
import com.comandos.inventory.model.EquipmentSetComponent;
import com.comandos.inventory.model.EquipmentSetOperation;
import com.comandos.inventory.model.EquipmentSetOperationComponent;
import com.comandos.inventory.model.StockBalance;
import com.comandos.maintenance.service.MaintenanceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EquipmentSetOperationService {
    private final EntityManager em;
    private final DonationService donations;
    private final DisposalService disposals;
    private final ConsumableUsageService consumptions;
    private final MaintenanceService maintenance;
    private final AuditService audit;

    public EquipmentSetOperationService(EntityManager em, DonationService donations, DisposalService disposals,
            ConsumableUsageService consumptions, MaintenanceService maintenance, AuditService audit) {
        this.em = em;
        this.donations = donations;
        this.disposals = disposals;
        this.consumptions = consumptions;
        this.maintenance = maintenance;
        this.audit = audit;
    }

    @Transactional
    public AggregateOperationView donate(DonationSetRequest request) {
        requireBase(request == null ? null : request.requestId(), request == null ? null : request.equipmentSetId(),
            request == null ? null : request.organizationId());
        String fingerprint = fingerprint("DONATION", request.organizationId(), request.unitId(), request.equipmentSetId(),
            request.donorId(), request.doneeId(), request.term(), request.documentReference());
        var replay = replay(request.requestId(), fingerprint);
        if (replay != null) return replay;
        var set = lockedSet(request.equipmentSetId(), request.organizationId(), request.unitId());
        var selected = components(set);
        requireSize(selected);
        var lines = selected.stream().map(component -> {
            validateComponent(component);
            return component.asset != null
                ? new com.comandos.donation.dto.DonationContract.LineRequest(component.asset.id, null, BigDecimal.ONE)
                : new com.comandos.donation.dto.DonationContract.LineRequest(null, component.balance.id, component.quantity);
        }).toList();
        var result = donations.finalize(new com.comandos.donation.dto.DonationContract.FinalizeRequest(
            request.requestId(), request.organizationId(), request.unitId(), request.donorId(), request.doneeId(),
            request.term(), lines, "OUTGOING", request.documentReference()));
        return record(set, selected, "DONATION", "donations", List.of(result.id()), request.requestId(), fingerprint);
    }

    @Transactional
    public AggregateOperationView dispose(DisposalSetRequest request) {
        requireBase(request == null ? null : request.requestId(), request == null ? null : request.equipmentSetId(),
            request == null ? null : request.organizationId());
        String fingerprint = fingerprint("DISPOSAL", request.organizationId(), request.unitId(), request.equipmentSetId(),
            request.processNumber(), request.reason(), request.destructionMethod(), request.destroyedAt(),
            request.destructionCertificate(), request.confirmed());
        var replay = replay(request.requestId(), fingerprint);
        if (replay != null) return replay;
        var set = lockedSet(request.equipmentSetId(), request.organizationId(), request.unitId());
        var selected = components(set);
        requireSize(selected);
        var lines = selected.stream().map(component -> {
            validateComponent(component);
            return component.asset != null
                ? new com.comandos.disposal.dto.DisposalContract.LineRequest(component.asset.id, null, BigDecimal.ONE)
                : new com.comandos.disposal.dto.DisposalContract.LineRequest(null, component.balance.id, component.quantity);
        }).toList();
        var result = disposals.finalize(new com.comandos.disposal.dto.DisposalContract.FinalizeRequest(
            request.requestId(), request.organizationId(), request.unitId(), request.processNumber(), request.reason(),
            request.destructionMethod(), request.destroyedAt(), request.destructionCertificate(), lines, request.confirmed()));
        return record(set, selected, "DISPOSAL", "disposals", List.of(result.id()), request.requestId(), fingerprint);
    }

    @Transactional
    public AggregateOperationView consume(ConsumableSetRequest request) {
        requireBase(request == null ? null : request.requestId(), request == null ? null : request.equipmentSetId(),
            request == null ? null : request.organizationId());
        Map<Long, BigDecimal> returned = request.returnedByComponentId() == null ? Map.of() : new TreeMap<>(request.returnedByComponentId());
        String fingerprint = fingerprint("CONSUMPTION", request.organizationId(), request.unitId(), request.equipmentSetId(),
            request.responsibleId(), request.authorizerId(), request.purpose(), request.activityType(),
            request.operationTraining(), returned, request.result());
        var replay = replay(request.requestId(), fingerprint);
        if (replay != null) return replay;
        var set = lockedSet(request.equipmentSetId(), request.organizationId(), request.unitId());
        var selected = components(set).stream().filter(component -> component.balance != null && isConsumable(component.balance)).toList();
        if (selected.isEmpty()) bad("Equipment set has no lot-controlled consumable components.");
        requireSize(selected);
        Set<Long> selectedIds = selected.stream().map(component -> component.id).collect(java.util.stream.Collectors.toSet());
        if (!selectedIds.containsAll(returned.keySet())) bad("Returned quantities can reference only consumable components of the selected equipment set.");
        var lines = new ArrayList<com.comandos.consumption.dto.ConsumableUsageContract.LineRequest>();
        for (var component : selected) {
            validateComponent(component);
            BigDecimal delivered = component.quantity;
            BigDecimal returnedQty = returned.getOrDefault(component.id, BigDecimal.ZERO);
            if (returnedQty.signum() < 0 || returnedQty.compareTo(delivered) > 0) bad("Returned quantity must be between zero and the equipment set component quantity.");
            BigDecimal used = delivered.subtract(returnedQty);
            lines.add(new com.comandos.consumption.dto.ConsumableUsageContract.LineRequest(
                component.balance.id, delivered, used, returnedQty,
                request.result() == null || request.result().isBlank() ? "USED" : request.result().trim()));
        }
        var result = consumptions.finalizeUsage(new com.comandos.consumption.dto.ConsumableUsageContract.FinalizeRequest(
            request.requestId(), request.organizationId(), request.unitId(), request.responsibleId(), request.authorizerId(),
            request.purpose(), request.activityType(), request.operationTraining(), lines));
        return record(set, selected, "CONSUMPTION", "consumable-usages", List.of(result.id()), request.requestId(), fingerprint);
    }

    @Transactional
    public AggregateOperationView maintain(MaintenanceSetRequest request) {
        requireBase(request == null ? null : request.requestId(), request == null ? null : request.equipmentSetId(),
            request == null ? null : request.organizationId());
        String fingerprint = fingerprint("MAINTENANCE", request.organizationId(), request.unitId(), request.equipmentSetId(),
            request.planId(), request.reason(), request.maintenanceType(), request.workshop(), request.gunsmith());
        var replay = replay(request.requestId(), fingerprint);
        if (replay != null) return replay;
        var set = lockedSet(request.equipmentSetId(), request.organizationId(), request.unitId());
        var selected = components(set).stream().filter(component -> component.asset != null).toList();
        if (selected.isEmpty()) bad("Equipment set has no serialized components eligible for maintenance.");
        requireSize(selected);
        List<Long> orderIds = new ArrayList<>();
        for (var component : selected) {
            validateComponent(component);
            String childRequestId = UUID.nameUUIDFromBytes((request.requestId() + "|maintenance|" + component.id)
                .getBytes(StandardCharsets.UTF_8)).toString();
            var order = maintenance.open(new com.comandos.maintenance.dto.MaintenanceContract.OpenRequest(
                childRequestId, request.organizationId(), request.unitId(), request.planId(), component.asset.id,
                request.reason(), null, request.maintenanceType(), request.workshop(), request.gunsmith()));
            orderIds.add(order.id());
        }
        return record(set, selected, "MAINTENANCE", "maintenance/work-orders", orderIds, request.requestId(), fingerprint);
    }

    private EquipmentSet lockedSet(Long id, Long organizationId, Long unitId) {
        var set = em.find(EquipmentSet.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (set == null) bad("Equipment set not found.");
        if (!set.active) conflict("Equipment set is inactive.");
        if (!Objects.equals(set.organizationLegacyId, organizationId)) {
            bad("Equipment set does not belong to the selected organization.");
        }
        if (unitId != null && !Objects.equals(set.unitLegacyId, unitId)) {
            bad("Equipment set does not belong to the selected unit.");
        }
        return set;
    }

    private List<EquipmentSetComponent> components(EquipmentSet set) {
        var result = em.createQuery("select c from EquipmentSetComponent c where c.equipmentSet.id=:id order by c.id", EquipmentSetComponent.class)
            .setParameter("id", set.id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        if (result.isEmpty()) bad("Equipment set has no components.");
        return result;
    }

    private static void validateComponent(EquipmentSetComponent component) {
        if ((component.asset == null) == (component.balance == null)) bad("Every equipment set component must reference exactly one asset or stock balance.");
        if (component.quantity == null || component.quantity.signum() <= 0) bad("Every equipment set component requires a positive quantity.");
        if (component.asset != null && component.quantity.compareTo(BigDecimal.ONE) != 0) bad("Serialized equipment set components require quantity one.");
    }

    private static boolean isConsumable(StockBalance balance) {
        var category = balance.lot.model.category;
        return Boolean.TRUE.equals(category.consumable) && Boolean.TRUE.equals(category.lotControlled) && !Boolean.TRUE.equals(category.serialized);
    }

    private AggregateOperationView replay(String requestId, String fingerprint) {
        var existing = em.createQuery("select o from EquipmentSetOperation o where o.requestId=:id", EquipmentSetOperation.class)
            .setParameter("id", requestId).getResultStream().findFirst();
        if (existing.isEmpty()) return null;
        if (!existing.get().requestFingerprint.equals(fingerprint)) conflict("Equipment set operation request ID was already used with different data.");
        return view(existing.get());
    }

    private AggregateOperationView record(EquipmentSet set, List<EquipmentSetComponent> selected, String operationType,
            String resource, List<Long> recordIds, String requestId, String fingerprint) {
        var actor = audit.actor();
        var operation = new EquipmentSetOperation();
        operation.equipmentSet = set;
        operation.organizationLegacyId = set.organizationLegacyId;
        operation.unitLegacyId = set.unitLegacyId;
        operation.organizationCanonicalId = set.organizationCanonicalId;
        operation.unitCanonicalId = set.unitCanonicalId;
        operation.setCode = set.code;
        operation.setName = set.name;
        operation.operationType = operationType;
        operation.aggregateResource = resource;
        operation.aggregateRecordIds = recordIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        operation.componentCount = selected.size();
        operation.executedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        operation.operatorId = actor.id();
        operation.operatorLogin = actor.login();
        operation.requestId = requestId;
        operation.requestFingerprint = fingerprint;
        em.persist(operation);
        for (var source : selected) {
            var snapshot = new EquipmentSetOperationComponent();
            snapshot.operation = operation;
            snapshot.sourceComponentId = source.id;
            snapshot.componentKind = source.asset != null ? "ASSET" : "BALANCE";
            snapshot.stockRecordId = source.asset != null ? source.asset.id : source.balance.id;
            snapshot.role = source.role;
            snapshot.quantity = source.quantity;
            em.persist(snapshot);
        }
        em.flush();
        var result = view(operation);
        audit.record("equipment-sets/operations", operation.id, operationType, null, result);
        return result;
    }

    private static AggregateOperationView view(EquipmentSetOperation operation) {
        List<Long> ids = operation.aggregateRecordIds == null || operation.aggregateRecordIds.isBlank() ? List.of()
            : java.util.Arrays.stream(operation.aggregateRecordIds.split(",")).map(Long::valueOf).toList();
        return new AggregateOperationView(operation.id, operation.equipmentSet.id, operation.setCode, operation.setName,
            operation.operationType, operation.aggregateResource, ids, operation.componentCount, operation.status,
            operation.executedAt.toString(), operation.operatorId, operation.operatorLogin);
    }

    private static void requireBase(String requestId, Long equipmentSetId, Long organizationId) {
        if (equipmentSetId == null || organizationId == null) bad("Equipment set and organization are required.");
        try {
            if (requestId == null || !UUID.fromString(requestId).toString().equals(requestId)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            bad("A canonical UUID request ID is required.");
        }
    }

    private static void requireSize(List<?> components) {
        if (components.size() > 100) bad("Aggregate equipment set operations support at most 100 applicable components.");
    }

    private static String fingerprint(Object... values) {
        String canonical = java.util.Arrays.stream(values).map(String::valueOf).collect(java.util.stream.Collectors.joining("|"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
