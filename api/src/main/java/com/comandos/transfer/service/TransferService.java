package com.comandos.transfer.service;

import com.comandos.audit.service.AuditService;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.AssetStatus;
import com.comandos.inventory.model.ItemCategory;
import com.comandos.inventory.model.StockBalance;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.StockLot;
import com.comandos.inventory.model.StockMovement;
import com.comandos.inventory.model.StockMovementNature;
import com.comandos.security.service.AccessPolicy;
import com.comandos.transfer.dto.TransferContract.AcceptRequest;
import com.comandos.transfer.dto.TransferContract.FinalizeRequest;
import com.comandos.transfer.dto.TransferContract.LineRequest;
import com.comandos.transfer.dto.TransferContract.RejectRequest;
import com.comandos.transfer.dto.TransferContract.LineView;
import com.comandos.transfer.dto.TransferContract.Page;
import com.comandos.transfer.dto.TransferContract.StockOption;
import com.comandos.transfer.dto.TransferContract.TransferView;
import com.comandos.transfer.model.InventoryTransfer;
import com.comandos.transfer.model.InventoryTransferItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class TransferService {
    private static final int PAGE_SIZE = 20;
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;

    public TransferService(EntityManager em, AccessPolicy access, AuditService audit) {
        this.em = em;
        this.access = access;
        this.audit = audit;
    }

    public Page<StockOption> stock(long organizationId, long sourceUnitId, String kind, String search, int page) {
        access.requireScope("transfers", "READ", organizationId, sourceUnitId);
        selectedUnit(organizationId, sourceUnitId);
        pagination(page);
        boolean assets = "ASSET".equals(kind);
        if (!assets && !"LOT".equals(kind)) bad("Stock kind must be ASSET or LOT.");
        String model = assets ? "e.model" : "e.lot.model";
        String code = assets ? "e.assetCode" : "e.lot.lotNumber";
        String expiry = assets ? "e.validUntil" : "e.lot.validUntil";
        String from = " from " + (assets ? "AssetItem" : "StockBalance") + " e"
            + " where e.location.organization.id=:organization and e.location.unit.id=:unit"
            + (assets ? " and e.status='AVAILABLE'" : " and e.available>0")
            + " and (" + expiry + " is null or " + expiry + ">=:today)"
            + " and (lower(" + code + ") like :search escape '!' or lower(" + model + ".name) like :search escape '!'"
            + " or lower(" + model + ".sku) like :search escape '!')";
        var query = em.createQuery("select e" + from + " order by e.id");
        var count = em.createQuery("select count(e)" + from, Long.class);
        String term = escaped(search);
        for (Query candidate : List.of(query, count)) {
            candidate.setParameter("organization", organizationId).setParameter("unit", sourceUnitId)
                .setParameter("today", LocalDate.now()).setParameter("search", term);
        }
        List<StockOption> content = query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream()
            .map(row -> stockOption(row)).toList();
        return new Page<>(content, count.getSingleResult(), page, PAGE_SIZE);
    }

    @Transactional
    public TransferView finalize(FinalizeRequest request) {
        validate(request);
        access.requireScope("transfers", "CREATE", request.organizationId(), request.sourceUnitId());
        access.requireScope("transfers", "CREATE", request.organizationId(), request.destinationUnitId());
        lockCatalog();

        Organization organization = locked(Organization.class, request.organizationId());
        if (!organization.active) bad("Organization must be active.");
        OrganizationalUnit sourceUnit = selectedUnit(organization.id, request.sourceUnitId());
        OrganizationalUnit destinationUnit = selectedUnit(organization.id, request.destinationUnitId());
        if (sourceUnit.id.equals(destinationUnit.id)) bad("Source and destination units must be different.");
        StockLocation destinationLocation = locked(StockLocation.class, request.destinationLocationId());
        if (!Boolean.TRUE.equals(destinationLocation.active)) bad("Destination location must be active.");
        if (!destinationLocation.organization.id.equals(organization.id) || destinationLocation.unit == null
                || !destinationLocation.unit.id.equals(destinationUnit.id)) {
            bad("Destination location must belong to the selected destination unit.");
        }

        String fingerprint = fingerprint(request);
        var existing = em.createQuery("select t from InventoryTransfer t where t.requestId=:requestId", InventoryTransfer.class)
            .setParameter("requestId", request.requestId()).getResultStream().findFirst();
        if (existing.isPresent()) {
            requireTransferRead(existing.get());
            if (!existing.get().requestFingerprint.equals(fingerprint)) {
                conflict("This request ID was already used for a different transfer.");
            }
            return view(existing.get());
        }

        InventoryTransfer transfer = new InventoryTransfer();
        transfer.organization = organization;
        transfer.sourceUnit = sourceUnit;
        transfer.destinationUnit = destinationUnit;
        transfer.destinationLocation = destinationLocation;
        transfer.organizationName = organization.name;
        transfer.sourceUnitName = sourceUnit.name;
        transfer.destinationUnitName = destinationUnit.name;
        transfer.destinationLocationName = destinationLocation.name;
        transfer.purpose = request.purpose().trim();
        transfer.transferType = request.transferType() == null || request.transferType().isBlank() ? "INTERNAL" : request.transferType().trim().toUpperCase(Locale.ROOT);
        transfer.legalInstrument = request.legalInstrument() == null || request.legalInstrument().isBlank() ? null : request.legalInstrument().trim();
        transfer.documentReference = request.documentReference() == null || request.documentReference().isBlank() ? null : request.documentReference().trim();
        transfer.status = "PENDING_ACCEPTANCE";
        transfer.sentAt = LocalDateTime.now();
        var actor = audit.actor();
        transfer.finalizedById = actor.id();
        transfer.finalizedByLogin = actor.login();
        transfer.requestId = request.requestId();
        transfer.requestFingerprint = fingerprint;
        em.persist(transfer);

        List<Map<String, Object>> changes = new ArrayList<>();
        for (LineRequest line : sorted(request.items())) {
            if (line.assetId() != null) moveAsset(transfer, line, destinationLocation, changes);
            else moveLot(transfer, line, destinationLocation, changes);
        }
        em.flush();
        TransferView result = view(transfer);
        audit.record("transfers", transfer.id, "FINALIZE", null, Map.of("transfer", result, "stockChanges", changes));
        return result;
    }

    @Transactional
    public TransferView accept(long id, AcceptRequest request) {
        if (request == null) bad("Acceptance request is required.");
        uuid(request.requestId());

        InventoryTransfer transfer = em.find(InventoryTransfer.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (transfer == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found.");

        access.requireScope("transfers", "ACCEPT", transfer.organization.id, transfer.destinationUnit.id);

        if ("ACCEPTED".equals(transfer.status)) return view(transfer);
        if (!"PENDING_ACCEPTANCE".equals(transfer.status)) {
            conflict("Transfer is not pending acceptance.");
        }

        var actor = audit.actor();
        var before = view(transfer);
        releaseAtDestination(transfer);

        transfer.status = "ACCEPTED";
        transfer.approvedById = actor.id();
        transfer.approvedByLogin = actor.login();
        transfer.approvedAt = LocalDateTime.now();

        em.flush();

        var result = view(transfer);
        audit.record("transfers", transfer.id, "ACCEPT", before, Map.of("transfer", result));
        return result;
    }

    @Transactional
    public TransferView reject(long id, RejectRequest request) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            bad("Rejection reason is required.");
        }
        uuid(request.requestId());
        String reason = request.reason().trim();
        if (reason.length() > 1000) bad("Rejection reason must contain at most 1000 characters.");

        InventoryTransfer transfer = em.find(InventoryTransfer.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (transfer == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found.");
        access.requireScope("transfers", "REJECT", transfer.organization.id, transfer.destinationUnit.id);

        if ("REJECTED".equals(transfer.status)) return view(transfer);
        if (!"PENDING_ACCEPTANCE".equals(transfer.status)) conflict("Transfer is not pending acceptance.");

        var before = view(transfer);
        returnToSource(transfer);

        var actor = audit.actor();
        transfer.status = "REJECTED";
        transfer.rejectedById = actor.id();
        transfer.rejectedByLogin = actor.login();
        transfer.rejectedAt = LocalDateTime.now();
        transfer.rejectionReason = reason;
        em.flush();

        var result = view(transfer);
        audit.record("transfers", transfer.id, "REJECT", before, Map.of("transfer", result));
        return result;
    }

    public Page<TransferView> list(long organizationId, Long unitId, int page) {
        access.requireScope("transfers", "READ", organizationId, unitId);
        if (unitId != null) selectedUnit(organizationId, unitId);
        pagination(page);
        String from = " from InventoryTransfer t where t.organization.id=:organization"
            + (unitId == null ? "" : " and (t.sourceUnit.id=:unit or t.destinationUnit.id=:unit)");
        var query = em.createQuery("select t" + from + " order by t.id desc", InventoryTransfer.class);
        var count = em.createQuery("select count(t)" + from, Long.class);
        for (Query candidate : List.of(query, count)) {
            candidate.setParameter("organization", organizationId);
            if (unitId != null) candidate.setParameter("unit", unitId);
        }
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream()
            .map(this::view).toList(), count.getSingleResult(), page, PAGE_SIZE);
    }

    public TransferView get(long id) {
        access.requireAny("transfers", "READ");
        InventoryTransfer transfer = em.find(InventoryTransfer.class, id);
        if (transfer == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found.");
        requireTransferRead(transfer);
        return view(transfer);
    }

    private void moveAsset(InventoryTransfer transfer, LineRequest line, StockLocation destination,
            List<Map<String, Object>> changes) {
        AssetItem asset = locked(AssetItem.class, line.assetId());
        requireSource(asset.location, transfer);
        if (!AssetStatus.AVAILABLE.name().equals(asset.status)) conflict("Asset " + asset.assetCode + " is no longer available.");
        notExpired(asset.validUntil);
        if (line.quantity().compareTo(BigDecimal.ONE) != 0) bad("Individual assets require quantity 1.");
        StockLocation source = asset.location;
        StockMovement out = movement(asset, null, source, StockMovementNature.TRANSFER_OUT.name(), BigDecimal.ONE.negate(), transfer.sentAt, transfer.id);
        StockMovement in = movement(asset, null, destination, StockMovementNature.TRANSFER_IN.name(), BigDecimal.ONE, transfer.sentAt, transfer.id);
        InventoryTransferItem item = item(transfer, asset.model, source, destination, asset.assetCode, line.quantity(), out, in);
        item.asset = asset;
        em.persist(item);
        changes.add(change("inventory/assets", asset.id, source.name, destination.name));
        asset.location = destination;
        asset.status = AssetStatus.TRANSFER_PENDING.name();
    }

    private void moveLot(InventoryTransfer transfer, LineRequest line, StockLocation destination,
            List<Map<String, Object>> changes) {
        StockBalance sourceBalance = locked(StockBalance.class, line.balanceId());
        requireSource(sourceBalance.location, transfer);
        StockLot lot = locked(StockLot.class, sourceBalance.lot.id);
        notExpired(lot.validUntil);
        if (sourceBalance.available.compareTo(line.quantity()) < 0) {
            conflict("Insufficient available stock for lot " + lot.lotNumber + ".");
        }
        StockBalance destinationBalance = destinationBalance(lot, destination);
        BigDecimal sourceBefore = sourceBalance.available;
        BigDecimal destinationBefore = destinationBalance.available;
        BigDecimal destinationBlockedBefore = destinationBalance.blocked;
        sourceBalance.available = sourceBefore.subtract(line.quantity());
        destinationBalance.blocked = destinationBlockedBefore.add(line.quantity());
        StockMovement out = movement(null, lot, sourceBalance.location, StockMovementNature.TRANSFER_OUT.name(), line.quantity().negate(), transfer.sentAt, transfer.id);
        StockMovement in = movement(null, lot, destination, StockMovementNature.TRANSFER_IN.name(), line.quantity(), transfer.sentAt, transfer.id);
        InventoryTransferItem item = item(transfer, lot.model, sourceBalance.location, destination, lot.lotNumber,
            line.quantity(), out, in);
        item.lot = lot;
        item.sourceBalance = sourceBalance;
        item.destinationBalance = destinationBalance;
        em.persist(item);
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("resource", "inventory/balances");
        change.put("sourceBalanceId", sourceBalance.id);
        change.put("destinationBalanceId", destinationBalance.id);
        change.put("sourceBefore", decimal(sourceBefore));
        change.put("sourceAfter", decimal(sourceBalance.available));
        change.put("destinationBefore", decimal(destinationBefore));
        change.put("destinationAfter", decimal(destinationBalance.available));
        change.put("destinationBlockedBefore", decimal(destinationBlockedBefore));
        change.put("destinationBlockedAfter", decimal(destinationBalance.blocked));
        changes.add(change);
    }

    private void releaseAtDestination(InventoryTransfer transfer) {
        var items = em.createQuery("select i from InventoryTransferItem i where i.transfer.id=:id order by i.id",
                InventoryTransferItem.class).setParameter("id", transfer.id)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        for (var item : items) {
            if (item.asset != null) {
                var asset = locked(AssetItem.class, item.asset.id);
                if (!AssetStatus.TRANSFER_PENDING.name().equals(asset.status) || !asset.location.id.equals(item.destinationLocation.id)) {
                    conflict("Transferred asset is no longer pending at the destination.");
                }
                asset.status = AssetStatus.AVAILABLE.name();
            } else {
                var destination = locked(StockBalance.class, item.destinationBalance.id);
                if (destination.blocked.compareTo(item.quantity) < 0) {
                    conflict("Transferred lot is no longer pending at the destination.");
                }
                destination.blocked = destination.blocked.subtract(item.quantity);
                destination.available = destination.available.add(item.quantity);
            }
        }
    }

    private void returnToSource(InventoryTransfer transfer) {
        var items = em.createQuery("select i from InventoryTransferItem i where i.transfer.id=:id order by i.id",
                InventoryTransferItem.class).setParameter("id", transfer.id)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        LocalDateTime now = LocalDateTime.now();
        for (var item : items) {
            if (item.asset != null) {
                var asset = locked(AssetItem.class, item.asset.id);
                if (!AssetStatus.TRANSFER_PENDING.name().equals(asset.status) || !asset.location.id.equals(item.destinationLocation.id)) {
                    conflict("Transferred asset is no longer pending at the destination.");
                }
                movement(asset, null, item.destinationLocation, StockMovementNature.TRANSFER_REJECT_OUT.name(), BigDecimal.ONE.negate(), now, transfer.id);
                movement(asset, null, item.sourceLocation, StockMovementNature.TRANSFER_REJECT_RETURN.name(), BigDecimal.ONE, now, transfer.id);
                asset.location = item.sourceLocation;
                asset.status = AssetStatus.AVAILABLE.name();
            } else {
                var source = locked(StockBalance.class, item.sourceBalance.id);
                var destination = locked(StockBalance.class, item.destinationBalance.id);
                if (destination.blocked.compareTo(item.quantity) < 0) {
                    conflict("Transferred lot is no longer pending at the destination.");
                }
                destination.blocked = destination.blocked.subtract(item.quantity);
                source.available = source.available.add(item.quantity);
                movement(null, item.lot, item.destinationLocation, StockMovementNature.TRANSFER_REJECT_OUT.name(), item.quantity.negate(), now, transfer.id);
                movement(null, item.lot, item.sourceLocation, StockMovementNature.TRANSFER_REJECT_RETURN.name(), item.quantity, now, transfer.id);
            }
        }
    }

    private StockBalance destinationBalance(StockLot lot, StockLocation destination) {
        var result = em.createQuery("select b from StockBalance b where b.lot.id=:lot and b.location.id=:location",
                StockBalance.class).setParameter("lot", lot.id).setParameter("location", destination.id)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
        if (result.isPresent()) return result.get();
        StockBalance balance = new StockBalance();
        balance.lot = lot;
        balance.location = destination;
        em.persist(balance);
        em.flush();
        return balance;
    }

    private InventoryTransferItem item(InventoryTransfer transfer, com.comandos.inventory.model.ItemModel model,
            StockLocation source, StockLocation destination, String code, BigDecimal quantity,
            StockMovement out, StockMovement in) {
        InventoryTransferItem item = new InventoryTransferItem();
        item.transfer = transfer;
        item.model = model;
        item.sourceLocation = source;
        item.destinationLocation = destination;
        item.outMovement = out;
        item.inMovement = in;
        item.modelName = model.name;
        item.sku = model.sku;
        item.stockCode = code;
        item.sourceLocationName = source.name;
        item.destinationLocationName = destination.name;
        item.unitOfMeasure = model.unitOfMeasure;
        item.quantity = quantity;
        return item;
    }

    private StockMovement movement(AssetItem asset, StockLot lot, StockLocation location, String nature,
            BigDecimal quantity, LocalDateTime movedAt, Long transferId) {
        StockMovement movement = new StockMovement();
        movement.asset = asset;
        movement.lot = lot;
        movement.location = location;
        movement.nature = nature;
        movement.referenceType = "TRANSFER";
        movement.referenceId = transferId;
        movement.quantity = quantity;
        movement.movedAt = movedAt;
        movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement);
        return movement;
    }

    private TransferView view(InventoryTransfer transfer) {
        List<LineView> items = em.createQuery(
                "select i from InventoryTransferItem i where i.transfer.id=:id order by i.id", InventoryTransferItem.class)
            .setParameter("id", transfer.id).getResultList().stream().map(item -> new LineView(item.id,
                item.asset == null ? null : item.asset.id, item.lot == null ? null : item.lot.id, item.modelName,
                item.sku, item.stockCode, item.sourceLocationName, item.destinationLocationName, item.unitOfMeasure,
                decimal(item.quantity), item.outMovement.id, item.inMovement.id)).toList();
        return new TransferView(transfer.id, transfer.organization.id, transfer.organizationName, transfer.sourceUnit.id,
            transfer.sourceUnitName, transfer.destinationUnit.id, transfer.destinationUnitName,
            transfer.destinationLocation.id, transfer.destinationLocationName, transfer.purpose, transfer.status,
            transfer.sentAt.toString(), transfer.finalizedById, transfer.finalizedByLogin,
            transfer.approvedById, transfer.approvedByLogin, transfer.approvedAt == null ? null : transfer.approvedAt.toString(),
            transfer.rejectedById, transfer.rejectedByLogin, transfer.rejectedAt == null ? null : transfer.rejectedAt.toString(),
            transfer.rejectionReason, items);
    }

    private void requireTransferRead(InventoryTransfer transfer) {
        if (!access.canScope("transfers", "READ", transfer.organization.id, transfer.sourceUnit.id)
                && !access.canScope("transfers", "READ", transfer.organization.id, transfer.destinationUnit.id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You do not have permission for this operation or scope.");
        }
    }

    private void requireSource(StockLocation source, InventoryTransfer transfer) {
        access.requireScope("transfers", "CREATE", source.organization.id, source.unit == null ? null : source.unit.id);
        if (!source.organization.id.equals(transfer.organization.id) || source.unit == null
                || !source.unit.id.equals(transfer.sourceUnit.id)) {
            bad("Every item must belong to the selected source unit.");
        }
    }

    private OrganizationalUnit selectedUnit(long organizationId, Long unitId) {
        if (unitId == null) bad("Source and destination units are required.");
        OrganizationalUnit unit = em.find(OrganizationalUnit.class, unitId);
        if (unit == null || !unit.organization.id.equals(organizationId)) {
            bad("Select a unit in the selected organization.");
        }
        if (!Boolean.TRUE.equals(unit.active)) bad("Selected unit must be active.");
        return unit;
    }

    private void validate(FinalizeRequest request) {
        if (request == null || request.organizationId() == null || request.sourceUnitId() == null
                || request.destinationUnitId() == null || request.destinationLocationId() == null
                || request.purpose() == null || request.purpose().isBlank()) {
            bad("Organization, source unit, destination unit, destination location and purpose are required.");
        }
        uuid(request.requestId());
        if (request.purpose().trim().length() > 255) bad("Purpose must contain at most 255 characters.");
        if (request.items() == null || request.items().isEmpty() || request.items().size() > 100) {
            bad("A transfer requires 1 to 100 items.");
        }
        Set<String> keys = new HashSet<>();
        for (LineRequest line : request.items()) {
            if (line == null || (line.assetId() == null) == (line.balanceId() == null)) {
                bad("Select exactly one asset or stock balance per item.");
            }
            amount(line.quantity());
            String key = line.assetId() != null ? "A" + line.assetId() : "B" + line.balanceId();
            if (!keys.add(key)) bad("Duplicate stock selection.");
        }
    }

    private static List<LineRequest> sorted(List<LineRequest> items) {
        return items.stream().sorted(Comparator.comparing(line -> line.assetId() != null
            ? "A" + String.format("%020d", line.assetId()) : "B" + String.format("%020d", line.balanceId()))).toList();
    }

    private static String fingerprint(FinalizeRequest request) {
        String lines = sorted(request.items()).stream().map(line -> line.assetId() + ":" + line.balanceId() + ":"
            + line.quantity().stripTrailingZeros().toPlainString()).toList().toString();
        return hash(request.organizationId() + "|" + request.sourceUnitId() + "|" + request.destinationUnitId() + "|"
            + request.destinationLocationId() + "|" + request.purpose().trim() + "|" + lines);
    }

    private StockOption stockOption(Object row) {
        if (row instanceof AssetItem asset) return new StockOption("ASSET", asset.id, asset.assetCode,
            asset.model.name, asset.model.sku, asset.location.name, asset.model.unitOfMeasure, "1");
        StockBalance balance = (StockBalance) row;
        return new StockOption("LOT", balance.id, balance.lot.lotNumber, balance.lot.model.name,
            balance.lot.model.sku, balance.location.name, balance.lot.model.unitOfMeasure, decimal(balance.available));
    }

    private static Map<String, Object> change(String resource, long recordId, String before, String after) {
        return Map.of("resource", resource, "recordId", recordId, "before", before, "after", after);
    }

    private void lockCatalog() {
        em.createQuery("select c from ItemCategory c order by c.id", ItemCategory.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }

    private <T> T locked(Class<T> type, Long id) {
        if (id == null || id <= 0) bad("A valid record ID is required.");
        T value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) bad(type.getSimpleName() + " not found.");
        return value;
    }

    private static void amount(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.stripTrailingZeros().scale() > 4
                || value.precision() - value.scale() > 15) {
            bad("Quantities must be positive with at most 15 integer and 4 decimal digits.");
        }
    }

    private static void notExpired(LocalDate validUntil) {
        if (validUntil != null && validUntil.isBefore(LocalDate.now())) bad("Expired stock cannot be transferred.");
    }

    private static String escaped(String value) {
        return "%" + (value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
            .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void uuid(String value) {
        try {
            if (value == null || !UUID.fromString(value).toString().equals(value)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            bad("A canonical UUID request ID is required.");
        }
    }

    private static String decimal(BigDecimal value) {
        return value.setScale(4, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static void pagination(int page) {
        if (page < 0 || page > 100000) bad("Invalid page.");
    }

    private static void bad(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static void conflict(String message) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
