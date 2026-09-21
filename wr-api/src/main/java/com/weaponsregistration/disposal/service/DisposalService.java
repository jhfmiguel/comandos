package com.weaponsregistration.disposal.service;

import com.weaponsregistration.audit.service.AuditService;
import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.OrganizationalUnit;
import com.weaponsregistration.disposal.dto.DisposalContract.*;
import com.weaponsregistration.disposal.model.*;
import com.weaponsregistration.inventory.model.*;
import com.weaponsregistration.security.service.AccessPolicy;
import jakarta.persistence.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DisposalService {
    private static final int PAGE_SIZE = 20;
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;

    public DisposalService(EntityManager em, AccessPolicy access, AuditService audit) {
        this.em = em; this.access = access; this.audit = audit;
    }

    public Page<StockOption> stock(long organizationId, Long unitId, String kind, String search, int page) {
        access.requireScope("disposals", "READ", organizationId, unitId);
        selectedUnit(organizationId, unitId); pagination(page);
        boolean assets = "ASSET".equals(kind);
        if (!assets && !"LOT".equals(kind)) bad("Stock kind must be ASSET or LOT.");
        String model = assets ? "e.model" : "e.lot.model";
        String code = assets ? "e.assetCode" : "e.lot.lotNumber";
        String from = " from " + (assets ? "AssetItem" : "StockBalance") + " e where e.location.organization.id=:organization"
            + (unitId == null ? "" : " and e.location.unit.id=:unit")
            + (assets ? " and e.status='AVAILABLE'" : " and e.available>0")
            + (assets ? "" : " and e.reserved=0")
            + " and not exists (select c.id from InventoryCount c where c.location.id=e.location.id and c.status.code in ('OPEN','COUNTED'))"
            + " and (lower(" + code + ") like :search escape '!' or lower(" + model + ".name) like :search escape '!'"
            + " or lower(" + model + ".sku) like :search escape '!')";
        var query = em.createQuery("select e" + from + " order by e.id", CoreEntity.class);
        var count = em.createQuery("select count(e)" + from, Long.class);
        for (Query candidate : List.of(query, count)) {
            candidate.setParameter("organization", organizationId).setParameter("search", escaped(search));
            if (unitId != null) candidate.setParameter("unit", unitId);
        }
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(row -> {
            if (row instanceof AssetItem asset) return new StockOption("ASSET", asset.id, asset.assetCode, asset.model.name,
                asset.model.sku, asset.location.name, asset.model.unitOfMeasure, "1");
            StockBalance balance = (StockBalance) row;
            return new StockOption("LOT", balance.id, balance.lot.lotNumber, balance.lot.model.name, balance.lot.model.sku,
                balance.location.name, balance.lot.model.unitOfMeasure, decimal(balance.available));
        }).toList(), count.getSingleResult(), page, PAGE_SIZE);
    }

    @Transactional
    public DisposalView finalize(FinalizeRequest request) {
        validate(request);
        access.requireScope("disposals", "CREATE", request.organizationId(), request.unitId());
        access.requireScope("disposals", "APPROVE", request.organizationId(), request.unitId());
        lockCatalog();
        Organization organization = locked(Organization.class, request.organizationId());
        if (!organization.active) bad("Organization must be active.");
        OrganizationalUnit unit = selectedUnit(organization.id, request.unitId());
        String fingerprint = fingerprint(request);
        var existing = em.createQuery("select p from DisposalProcess p where p.requestId=:requestId", DisposalProcess.class)
            .setParameter("requestId", request.requestId()).getResultStream().findFirst();
        if (existing.isPresent()) {
            access.requireEntity("disposals", "READ", existing.get());
            if (!existing.get().requestFingerprint.equals(fingerprint)) conflict("This request ID was already used for a different disposal.");
            return view(existing.get());
        }
        boolean duplicate = !em.createQuery("select p.id from DisposalProcess p where p.organization.id=:organization and p.processNumber=:number", Long.class)
            .setParameter("organization", organization.id).setParameter("number", request.processNumber().trim()).setMaxResults(1).getResultList().isEmpty();
        if (duplicate) conflict("A disposal process with this number already exists in the organization.");

        DisposalProcess process = new DisposalProcess();
        process.organization = organization; process.unit = unit; process.organizationName = organization.name;
        process.unitName = unit == null ? null : unit.name; process.processNumber = request.processNumber().trim();
        process.reason = request.reason().trim(); process.finalizedAt = LocalDateTime.now();
        var actor = audit.actor(); process.finalizedById = actor.id(); process.finalizedByLogin = actor.login();
        process.requestId = request.requestId(); process.requestFingerprint = fingerprint; em.persist(process);

        if (destructionRequested(request)) {
            Destruction destruction = new Destruction(); destruction.process = process;
            destruction.method = request.destructionMethod().trim();
            destruction.destroyedAt = parseDateTime(request.destroyedAt());
            destruction.certificate = request.destructionCertificate().trim(); em.persist(destruction);
        }
        List<Map<String, Object>> changes = new ArrayList<>();
        for (LineRequest line : sorted(request.items())) dispose(process, line, changes);
        em.flush(); DisposalView result = view(process);
        audit.record("disposals", process.id, "FINALIZE", null, Map.of("disposal", result, "stockChanges", changes));
        return result;
    }

    public Page<DisposalView> list(long organizationId, Long unitId, int page) {
        access.requireScope("disposals", "READ", organizationId, unitId); selectedUnit(organizationId, unitId); pagination(page);
        String from = " from DisposalProcess p where p.organization.id=:organization" + (unitId == null ? "" : " and p.unit.id=:unit");
        var query = em.createQuery("select p" + from + " order by p.id desc", DisposalProcess.class);
        var count = em.createQuery("select count(p)" + from, Long.class);
        for (Query candidate : List.of(query, count)) { candidate.setParameter("organization", organizationId); if (unitId != null) candidate.setParameter("unit", unitId); }
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(this::view).toList(),
            count.getSingleResult(), page, PAGE_SIZE);
    }

    public DisposalView get(long id) {
        access.requireAny("disposals", "READ"); DisposalProcess process = em.find(DisposalProcess.class, id);
        if (process == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Disposal process not found.");
        access.requireEntity("disposals", "READ", process); return view(process);
    }

    private void dispose(DisposalProcess process, LineRequest line, List<Map<String, Object>> changes) {
        DisposalItem item = new DisposalItem(); item.process = process; item.quantity = line.quantity();
        if (line.assetId() != null) {
            AssetItem asset = locked(AssetItem.class, line.assetId()); scope(asset.location, process);
            if (!"AVAILABLE".equals(asset.status)) conflict("Asset " + asset.assetCode + " is no longer available.");
            if (line.quantity().compareTo(BigDecimal.ONE) != 0) bad("Individual assets require quantity 1.");
            item.asset = asset; item.model = asset.model; item.location = asset.location; item.stockCode = asset.assetCode;
            changes.add(Map.of("resource", "inventory/assets", "recordId", asset.id, "before", asset.status, "after", "DISPOSED"));
            asset.status = "DISPOSED";
        } else {
            StockBalance balance = locked(StockBalance.class, line.balanceId()); scope(balance.location, process);
            if (balance.reserved.signum() > 0) conflict("Release active reservations before disposing this stock balance.");
            StockLot lot = locked(StockLot.class, balance.lot.id);
            if (balance.available.compareTo(line.quantity()) < 0 || lot.availableQuantity.compareTo(line.quantity()) < 0)
                conflict("Insufficient available stock for lot " + lot.lotNumber + ".");
            item.lot = lot; item.model = lot.model; item.location = balance.location; item.stockCode = lot.lotNumber;
            changes.add(Map.of("resource", "inventory/balances", "recordId", balance.id,
                "before", decimal(balance.available), "after", decimal(balance.available.subtract(line.quantity()))));
            balance.available = balance.available.subtract(line.quantity()); lot.availableQuantity = lot.availableQuantity.subtract(line.quantity());
        }
        item.modelName = item.model.name; item.sku = item.model.sku; item.locationName = item.location.name;
        item.unitOfMeasure = item.model.unitOfMeasure;
        StockMovement movement = new StockMovement(); movement.asset = item.asset; movement.lot = item.lot;
        movement.location = item.location; movement.nature = "DISPOSAL"; movement.quantity = item.quantity.negate();
        movement.movedAt = process.finalizedAt; movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement); item.movement = movement; em.persist(item);
    }

    private DisposalView view(DisposalProcess process) {
        var destruction = em.createQuery("select d from Destruction d where d.process.id=:id", Destruction.class)
            .setParameter("id", process.id).getResultStream().findFirst();
        List<LineView> items = em.createQuery("select i from DisposalItem i where i.process.id=:id order by i.id", DisposalItem.class)
            .setParameter("id", process.id).getResultList().stream().map(item -> new LineView(item.id,
                item.asset == null ? null : item.asset.id, item.lot == null ? null : item.lot.id, item.modelName, item.sku,
                item.stockCode, item.locationName, item.unitOfMeasure, decimal(item.quantity), item.movement.id)).toList();
        return new DisposalView(process.id, process.organization.id, process.organizationName,
            process.unit == null ? null : process.unit.id, process.unitName, process.processNumber, process.reason,
            process.status, process.finalizedAt.toString(), process.finalizedById, process.finalizedByLogin,
            destruction.map(value -> value.method).orElse(null), destruction.map(value -> value.destroyedAt.toString()).orElse(null),
            destruction.map(value -> value.certificate).orElse(null), items);
    }

    private void scope(StockLocation location, DisposalProcess process) {
        access.requireScope("disposals", "CREATE", location.organization.id, location.unit == null ? null : location.unit.id);
        access.requireScope("disposals", "APPROVE", location.organization.id, location.unit == null ? null : location.unit.id);
        if (!location.organization.id.equals(process.organization.id) || process.unit != null
                && (location.unit == null || !location.unit.id.equals(process.unit.id)))
            bad("Every item must belong to the selected organization and unit.");
        // Opening a count locks the same organization, so its snapshot cannot race this check.
        long activeCounts = em.createQuery("select count(c) from InventoryCount c where c.location.id=:location"
                + " and c.status.code in ('OPEN','COUNTED')", Long.class)
            .setParameter("location", location.id).getSingleResult();
        if (activeCounts > 0) conflict("Finish or cancel the active inventory count before disposing stock at this location.");
    }

    private OrganizationalUnit selectedUnit(long organizationId, Long unitId) {
        if (unitId == null) return null; OrganizationalUnit unit = em.find(OrganizationalUnit.class, unitId);
        if (unit == null || !unit.organization.id.equals(organizationId)) bad("Select a unit in the selected organization.");
        return unit;
    }

    private void validate(FinalizeRequest request) {
        if (request == null || request.organizationId() == null || blank(request.processNumber()) || blank(request.reason()))
            bad("Organization, process number and reason are required.");
        if (!Boolean.TRUE.equals(request.confirmed())) bad("Explicit confirmation is required for irreversible disposal.");
        uuid(request.requestId()); limit(request.processNumber(), "Process number"); limit(request.reason(), "Reason");
        int destructionFields = (blank(request.destructionMethod()) ? 0 : 1) + (blank(request.destroyedAt()) ? 0 : 1)
            + (blank(request.destructionCertificate()) ? 0 : 1);
        if (destructionFields != 0 && destructionFields != 3)
            bad("Destruction method, date and certificate must be provided together.");
        if (destructionFields == 3) { limit(request.destructionMethod(), "Destruction method"); limit(request.destructionCertificate(), "Destruction certificate"); parseDateTime(request.destroyedAt()); }
        if (request.items() == null || request.items().isEmpty() || request.items().size() > 100)
            bad("A disposal requires 1 to 100 items.");
        Set<String> keys = new HashSet<>();
        for (LineRequest line : request.items()) {
            if (line == null || (line.assetId() == null) == (line.balanceId() == null)) bad("Select exactly one asset or stock balance per item.");
            amount(line.quantity()); String key = line.assetId() != null ? "A" + line.assetId() : "B" + line.balanceId();
            if (!keys.add(key)) bad("Duplicate stock selection.");
        }
    }

    private static boolean destructionRequested(FinalizeRequest request) { return !blank(request.destructionMethod()); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void limit(String value, String label) { if (value.trim().length() > 255) bad(label + " must contain at most 255 characters."); }
    private static LocalDateTime parseDateTime(String value) { try { return LocalDateTime.parse(value); } catch (RuntimeException exception) { bad("Destruction date and time is invalid."); return null; } }
    private static List<LineRequest> sorted(List<LineRequest> items) { return items.stream().sorted(Comparator.comparing(line -> line.assetId() != null ? "A" + line.assetId() : "B" + line.balanceId())).toList(); }
    private static String fingerprint(FinalizeRequest request) {
        String lines = sorted(request.items()).stream().map(line -> line.assetId() + ":" + line.balanceId() + ":" + line.quantity().stripTrailingZeros().toPlainString()).toList().toString();
        return hash(request.organizationId() + "|" + request.unitId() + "|" + request.processNumber().trim() + "|" + request.reason().trim()
            + "|" + request.destructionMethod() + "|" + request.destroyedAt() + "|" + request.destructionCertificate() + "|" + lines);
    }
    private void lockCatalog() { em.createQuery("select c from ItemCategory c order by c.id", ItemCategory.class).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList(); }
    private <T> T locked(Class<T> type, Long id) { if (id == null || id <= 0) bad("A valid record ID is required."); T value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE); if (value == null) bad(type.getSimpleName() + " not found."); return value; }
    private static void amount(BigDecimal value) { if (value == null || value.signum() <= 0 || value.stripTrailingZeros().scale() > 4 || value.precision() - value.scale() > 15) bad("Quantities must be positive with at most 15 integer and 4 decimal digits."); }
    private static String escaped(String value) { return "%" + (value == null ? "" : value.trim().toLowerCase(Locale.ROOT)).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%"; }
    private static String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); } }
    private static void uuid(String value) { try { if (value == null || !UUID.fromString(value).toString().equals(value)) throw new IllegalArgumentException(); } catch (IllegalArgumentException exception) { bad("A canonical UUID request ID is required."); } }
    private static String decimal(BigDecimal value) { return value.setScale(4, RoundingMode.UNNECESSARY).toPlainString(); }
    private static void pagination(int page) { if (page < 0 || page > 100000) bad("Invalid page."); }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
