package com.weaponsregistration.custody.service;

import com.weaponsregistration.audit.service.AuditService;
import com.weaponsregistration.core.model.*;
import com.weaponsregistration.custody.dto.CustodyContract.*;
import com.weaponsregistration.custody.model.*;
import com.weaponsregistration.inventory.model.*;
import com.weaponsregistration.security.service.AccessPolicy;
import jakarta.persistence.*;
import java.math.BigDecimal;
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
public class CustodyService {
    private static final int PAGE_SIZE = 20;
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;

    public CustodyService(EntityManager em, AccessPolicy access, AuditService audit) {
        this.em = em; this.access = access; this.audit = audit;
    }

    public Page<StockOption> stock(long organizationId, Long unitId, String search, int page) {
        access.requireScope("custodies", "READ", organizationId, unitId);
        selectedUnit(organizationId, unitId); pagination(page);
        String from = " from AssetItem a where a.location.organization.id = :organization"
            + (unitId == null ? "" : " and a.location.unit.id = :unit")
            + " and a.status = 'AVAILABLE'"
            + " and not exists (select c.id from EquipmentSetComponent c where c.asset = a and c.equipmentSet.active = true)"
            + " and (a.validUntil is null or a.validUntil >= :today)"
            + " and (lower(a.assetCode) like :search escape '!' or lower(a.serialNumber) like :search escape '!'"
            + " or lower(a.model.name) like :search escape '!')";
        var query = em.createQuery("select a" + from + " order by a.id", AssetItem.class);
        var count = em.createQuery("select count(a)" + from, Long.class);
        String term = escaped(search);
        for (var q : List.of(query, count)) {
            q.setParameter("organization", organizationId).setParameter("today", LocalDate.now()).setParameter("search", term);
            if (unitId != null) q.setParameter("unit", unitId);
        }
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream()
            .map(a -> new StockOption(a.id, a.assetCode, a.serialNumber, a.model.name, a.location.name)).toList(),
            count.getSingleResult(), page, PAGE_SIZE);
    }

    public Page<EquipmentSetOption> equipmentSets(long organizationId, Long unitId, String search, int page) {
        access.requireScope("custodies", "READ", organizationId, unitId);
        selectedUnit(organizationId, unitId); pagination(page);
        String from = " from EquipmentSet s where s.organization.id = :organization and s.active = true"
            + (unitId == null ? "" : " and s.unit.id = :unit")
            + " and (lower(s.code) like :search escape '!' or lower(s.name) like :search escape '!')";
        var candidates = em.createQuery("select s" + from + " order by s.id", EquipmentSet.class);
        for (var q : List.of(candidates)) {
            q.setParameter("organization", organizationId).setParameter("search", escaped(search));
            if (unitId != null) q.setParameter("unit", unitId);
        }
        var available = candidates.getResultList().stream().filter(this::setCurrentlyAvailable).toList();
        var content = available.stream().skip((long) page * PAGE_SIZE).limit(PAGE_SIZE).map(set -> new EquipmentSetOption(set.id,
            set.code, set.name, Math.toIntExact(countComponents(set.id)))).toList();
        return new Page<>(content, available.size(), page, PAGE_SIZE);
    }

    @Transactional
    public CustodyView issue(IssueRequest request) {
        validateIssue(request);
        access.requireScope("custodies", "CREATE", request.organizationId(), request.unitId());
        lockCatalog();
        var organization = locked(Organization.class, request.organizationId());
        var unit = selectedUnit(organization.id, request.unitId());
        String fingerprint = issueFingerprint(request);
        var existing = em.createQuery("select c from Custody c where c.requestId = :requestId", Custody.class)
            .setParameter("requestId", request.requestId()).getResultStream().findFirst();
        if (existing.isPresent()) {
            access.requireEntity("custodies", "READ", existing.get());
            if (!existing.get().requestFingerprint.equals(fingerprint)) conflict("This request ID was already used for a different custody.");
            return view(existing.get());
        }
        var recipient = request.recipientId() == null ? null : locked(Person.class, request.recipientId());
        var recipientUnit = request.recipientUnitId() == null ? null : locked(OrganizationalUnit.class, request.recipientUnitId());
        var authorizer = locked(Person.class, request.authorizerId());
        if (recipient != null) access.requireEntity("core/people", "READ", recipient);
        if (recipientUnit != null) {
            access.requireEntity("core/units", "READ", recipientUnit);
            if (!recipientUnit.organization.id.equals(organization.id)) bad("The receiving unit must belong to the selected organization.");
        }
        access.requireEntity("core/people", "READ", authorizer);
        if (!organization.active || recipient != null && !recipient.active || !authorizer.active) bad("Organization, recipient and authorizer must be active.");
        var deliveredAt = LocalDateTime.now();
        var dueAt = parseDueAt(request.dueAt(), deliveredAt);
        var custody = new Custody();
        custody.organization = organization; custody.unit = unit; custody.recipient = recipient; custody.authorizer = authorizer;
        custody.recipientUnit = recipientUnit;
        custody.organizationName = organization.name; custody.unitName = unit == null ? null : unit.name;
        custody.recipientName = recipient == null ? recipientUnit.name : recipient.fullName; custody.authorizerName = authorizer.fullName;
        custody.purpose = request.purpose().trim(); custody.deliveredAt = deliveredAt; custody.dueAt = dueAt;
        custody.recipientType = request.recipientType() == null || request.recipientType().isBlank() ? (recipient == null ? "UNIT" : "PERSON") : request.recipientType().trim().toUpperCase(Locale.ROOT);
        custody.custodyScope = request.custodyScope() == null || request.custodyScope().isBlank() ? "INDIVIDUAL" : request.custodyScope().trim().toUpperCase(Locale.ROOT);
        custody.durationType = request.durationType() == null || request.durationType().isBlank() ? (dueAt == null ? "PERMANENT" : "TEMPORARY") : request.durationType().trim().toUpperCase(Locale.ROOT);
        custody.teamOperation = request.teamOperation() == null || request.teamOperation().isBlank() ? null : request.teamOperation().trim();
        custody.responsibilityTerm = request.responsibilityTerm() == null || request.responsibilityTerm().isBlank() ? null : request.responsibilityTerm().trim();
        custody.deliveryCondition = request.deliveryCondition() == null || request.deliveryCondition().isBlank() ? null : request.deliveryCondition().trim();
        custody.requestId = request.requestId(); custody.requestFingerprint = fingerprint;
        var actor = audit.actor(); custody.issuedById = actor.id(); custody.issuedByLogin = actor.login();
        em.persist(custody);
        List<Map<String, Object>> stockChanges = new ArrayList<>();
        for (Long assetId : values(request.assetIds()).stream().sorted().toList()) {
            var asset = locked(AssetItem.class, assetId);
            validateAsset(asset, organization.id, request.unitId(), false);
            createAssetItem(custody, asset, null, null, deliveredAt);
            stockChanges.add(Map.of("resource", "inventory/assets", "recordId", asset.id,
                "before", Map.of("status", asset.status), "after", Map.of("status", "CUSTODIED")));
            asset.status = "CUSTODIED";
        }
        Set<Long> selectedAssets = new HashSet<>(values(request.assetIds()));
        int expanded = selectedAssets.size();
        for (Long setId : values(request.equipmentSetIds()).stream().sorted().toList()) {
            var set = locked(EquipmentSet.class, setId);
            validateSet(set, organization.id, request.unitId());
            var components = em.createQuery("select c from EquipmentSetComponent c where c.equipmentSet.id = :set order by c.id", EquipmentSetComponent.class)
                .setParameter("set", set.id).getResultList();
            if (components.isEmpty()) bad("Equipment set " + set.code + " has no components.");
            expanded += components.size();
            if (expanded > 100) bad("A custody can contain at most 100 expanded items.");
            for (var component : components) {
                if (component.asset != null) {
                    if (!selectedAssets.add(component.asset.id)) bad("The same individual asset was selected more than once.");
                    var asset = locked(AssetItem.class, component.asset.id); validateAsset(asset, organization.id, request.unitId(), true);
                    createAssetItem(custody, asset, set, component.role, deliveredAt);
                    stockChanges.add(Map.of("resource", "inventory/assets", "recordId", asset.id,
                        "before", Map.of("status", asset.status), "after", Map.of("status", "CUSTODIED")));
                    asset.status = "CUSTODIED";
                } else {
                    var balance = locked(StockBalance.class, component.balance.id);
                    validateBalance(balance, component.quantity, organization.id, request.unitId());
                    createBalanceItem(custody, balance, set, component.role, component.quantity, deliveredAt);
                    balance.available = balance.available.subtract(component.quantity);
                    balance.lot.availableQuantity = balance.lot.availableQuantity.subtract(component.quantity);
                    stockChanges.add(Map.of("resource", "inventory/balances", "recordId", balance.id,
                        "quantity", component.quantity.toPlainString()));
                }
            }
        }
        em.flush();
        var result = view(custody);
        audit.record("custodies", custody.id, "ISSUE", null, Map.of("custody", result, "stockChanges", stockChanges));
        return result;
    }

    @Transactional
    public CustodyView returnItems(long custodyId, ReturnRequest request) {
        validateReturn(request);
        lockCatalog();
        var custody = em.find(Custody.class, custodyId, LockModeType.PESSIMISTIC_WRITE);
        if (custody == null) notFound();
        access.requireEntity("custodies", "RETURN", custody);
        String fingerprint = returnFingerprint(custodyId, request);
        var existing = em.createQuery("select r from CustodyReturn r where r.requestId = :requestId", CustodyReturn.class)
            .setParameter("requestId", request.requestId()).getResultStream().findFirst();
        if (existing.isPresent()) {
            boolean legacyRetry = request.conditionTypeId() == null && cleanNotes(request.inspectionNotes()) == null
                && existing.get().requestFingerprint.equals(legacyReturnFingerprint(custodyId, request));
            if (!existing.get().custody.id.equals(custodyId)
                    || !existing.get().requestFingerprint.equals(fingerprint) && !legacyRetry)
                conflict("This request ID was already used for a different return.");
            return view(custody);
        }
        var condition = returnCondition(request.conditionTypeId());
        var items = em.createQuery("select i from CustodyItem i where i.custody.id = :custody and i.id in :ids order by i.id", CustodyItem.class)
            .setParameter("custody", custodyId).setParameter("ids", request.itemIds()).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        if (items.size() != new HashSet<>(request.itemIds()).size()) bad("Every return item must belong to the selected custody.");
        validateCompleteSets(custodyId, items);
        var returnedAt = LocalDateTime.now();
        var returned = new CustodyReturn(); returned.custody = custody; returned.requestId = request.requestId();
        returned.requestFingerprint = fingerprint; returned.returnedAt = returnedAt;
        var actor = audit.actor(); returned.returnedById = actor.id(); returned.returnedByLogin = actor.login(); em.persist(returned);
        List<Map<String, Object>> stockChanges = new ArrayList<>();
        for (var item : items) {
            if (item.returnedAt != null) conflict("Asset " + item.assetCode + " was already returned.");
            AssetItem asset = item.asset == null ? null : locked(AssetItem.class, item.asset.id);
            StockBalance balance = item.balance == null ? null : locked(StockBalance.class, item.balance.id);
            String nextStatus = null;
            StockMovement movement;
            if (asset != null) {
                if (!"CUSTODIED".equals(asset.status)) conflict("Asset " + item.assetCode + " is no longer marked as custodied.");
                nextStatus = condition.blocksAvailability || asset.validUntil != null && asset.validUntil.isBefore(LocalDate.now())
                    ? "BLOCKED" : "AVAILABLE";
                movement = movement(asset, "CUSTODY_RETURN", BigDecimal.ONE, returnedAt);
                asset.status = nextStatus;
                if (Set.of("GOOD", "NEEDS_INSPECTION", "DAMAGED").contains(condition.code)) asset.condition = condition.code;
            } else {
                movement = movement(balance, "CUSTODY_RETURN", item.quantity, returnedAt);
                if (condition.blocksAvailability) balance.blocked = balance.blocked.add(item.quantity);
                else balance.available = balance.available.add(item.quantity);
                balance.lot.availableQuantity = balance.lot.availableQuantity.add(item.quantity);
            }
            var returnItem = new CustodyReturnItem(); returnItem.custodyReturn = returned; returnItem.custodyItem = item;
            returnItem.movement = movement; returnItem.conditionType = condition;
            returnItem.inspectionNotes = cleanNotes(request.inspectionNotes());
            returnItem.inspectedById = actor.id(); returnItem.inspectedByLogin = actor.login(); em.persist(returnItem);
            item.returnedAt = returnedAt;
            if (asset != null) stockChanges.add(Map.of("resource", "inventory/assets", "recordId", asset.id,
                "before", Map.of("status", "CUSTODIED"), "after", Map.of("status", nextStatus)));
            else stockChanges.add(Map.of("resource", "inventory/balances", "recordId", balance.id,
                "quantity", item.quantity.toPlainString()));
        }
        long remaining = em.createQuery("select count(i) from CustodyItem i where i.custody.id = :custody and i.returnedAt is null", Long.class)
            .setParameter("custody", custodyId).getSingleResult();
        custody.status = remaining == 0 ? "RETURNED" : "PARTIALLY_RETURNED";
        if (remaining == 0) custody.completedAt = returnedAt;
        em.flush();
        var result = view(custody);
        audit.record("custodies", custody.id, "RETURN", null, Map.of("custody", result, "stockChanges", stockChanges,
            "returnId", returned.id));
        return result;
    }

    public Page<CustodyView> list(long organizationId, Long unitId, int page) {
        access.requireScope("custodies", "READ", organizationId, unitId);
        selectedUnit(organizationId, unitId); pagination(page);
        String from = " from Custody c where c.organization.id = :organization" + (unitId == null ? "" : " and c.unit.id = :unit");
        var query = em.createQuery("select c" + from + " order by c.id desc", Custody.class);
        var count = em.createQuery("select count(c)" + from, Long.class);
        for (var q : List.of(query, count)) { q.setParameter("organization", organizationId); if (unitId != null) q.setParameter("unit", unitId); }
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(this::view).toList(),
            count.getSingleResult(), page, PAGE_SIZE);
    }

    public CustodyView get(long id) {
        access.requireAny("custodies", "READ");
        var custody = em.find(Custody.class, id); if (custody == null) notFound();
        access.requireEntity("custodies", "READ", custody); return view(custody);
    }

    private CustodyView view(Custody custody) {
        var items = em.createQuery("select i from CustodyItem i where i.custody.id = :custody order by i.id", CustodyItem.class)
            .setParameter("custody", custody.id).getResultList().stream().map(i -> { var inspection = em.createQuery(
                "select r from CustodyReturnItem r where r.custodyItem.id = :item", CustodyReturnItem.class)
                .setParameter("item", i.id).getResultStream().findFirst().orElse(null); return new ItemView(i.id,
                i.asset == null ? null : i.asset.id, i.balance == null ? null : i.balance.id,
                i.equipmentSet == null ? null : i.equipmentSet.id, i.equipmentSetCode, i.equipmentSetName,
                i.componentRole, i.assetCode, i.serialNumber, i.modelName, i.locationName,
                i.quantity.toPlainString(), i.issueMovement.id, text(i.returnedAt),
                inspection == null || inspection.conditionType == null ? null : inspection.conditionType.id,
                inspection == null || inspection.conditionType == null ? null : inspection.conditionType.name,
                inspection == null || inspection.conditionType == null ? null : inspection.conditionType.blocksAvailability,
                inspection == null ? null : inspection.inspectionNotes,
                inspection == null ? null : inspection.id,
                inspection == null ? null : em.createQuery("select w.id from WorkOrder w where w.custodyReturnItem.id = :source", Long.class)
                    .setParameter("source", inspection.id).getResultStream().findFirst().orElse(null)); }).toList();
        var returns = em.createQuery("select r from CustodyReturn r where r.custody.id = :custody order by r.id", CustodyReturn.class)
            .setParameter("custody", custody.id).getResultList().stream().map(r -> new ReturnView(r.id, text(r.returnedAt),
                r.returnedById, r.returnedByLogin, em.createQuery("select i.custodyItem.id from CustodyReturnItem i where i.custodyReturn.id = :return order by i.id", Long.class)
                    .setParameter("return", r.id).getResultList())).toList();
        return new CustodyView(custody.id, custody.organization.id, custody.organizationName,
            custody.unit == null ? null : custody.unit.id, custody.unitName,
            custody.recipient == null ? null : custody.recipient.id,
            custody.recipientUnit == null ? null : custody.recipientUnit.id,
            custody.recipientUnit == null ? "PERSON" : "UNIT", custody.recipientName,
            custody.authorizer.id, custody.authorizerName, custody.purpose, custody.status, text(custody.deliveredAt),
            text(custody.dueAt), text(custody.completedAt), custody.issuedById, custody.issuedByLogin, items, returns);
    }

    private void validateAsset(AssetItem asset, long organizationId, Long unitId, boolean equipmentSetIssue) {
        access.requireScope("custodies", "CREATE", asset.location.organization.id, asset.location.unit == null ? null : asset.location.unit.id);
        if (!asset.location.organization.id.equals(organizationId) || unitId != null && (asset.location.unit == null || !unitId.equals(asset.location.unit.id)))
            bad("Every asset must belong to the selected organization and unit.");
        if (!"AVAILABLE".equals(asset.status)) conflict("Asset " + asset.assetCode + " is no longer available.");
        if (asset.validUntil != null && asset.validUntil.isBefore(LocalDate.now())) bad("Expired assets cannot be issued in custody.");
        if (!equipmentSetIssue && countActiveSets(asset.id) > 0)
            conflict("Asset " + asset.assetCode + " belongs to an active equipment set and must be issued with that set.");
    }

    private void validateSet(EquipmentSet set, long organizationId, Long unitId) {
        access.requireScope("custodies", "CREATE", set.organization.id, set.unit == null ? null : set.unit.id);
        if (!set.organization.id.equals(organizationId) || unitId != null && (set.unit == null || !unitId.equals(set.unit.id)))
            bad("Every equipment set must belong to the selected organization and unit.");
        if (!set.active) conflict("Equipment set " + set.code + " is no longer active.");
    }

    private void validateBalance(StockBalance balance, BigDecimal quantity, long organizationId, Long unitId) {
        access.requireScope("custodies", "CREATE", balance.location.organization.id,
            balance.location.unit == null ? null : balance.location.unit.id);
        if (!balance.location.organization.id.equals(organizationId) || unitId != null
                && (balance.location.unit == null || !unitId.equals(balance.location.unit.id)))
            bad("Every stock balance must belong to the selected organization and unit.");
        if (balance.lot.validUntil != null && balance.lot.validUntil.isBefore(LocalDate.now()))
            bad("Expired stock cannot be issued in custody.");
        if (balance.available.compareTo(quantity) < 0) conflict("The equipment set quantity is no longer available.");
    }

    private void createAssetItem(Custody custody, AssetItem asset, EquipmentSet set, String role, LocalDateTime at) {
        var item = new CustodyItem(); item.custody = custody; item.asset = asset; item.equipmentSet = set;
        item.location = asset.location; item.issueMovement = movement(asset, "CUSTODY_ISSUE", BigDecimal.ONE.negate(), at);
        item.modelName = asset.model.name; item.assetCode = asset.assetCode; item.serialNumber = asset.serialNumber;
        item.locationName = asset.location.name; item.equipmentSetCode = set == null ? null : set.code;
        item.equipmentSetName = set == null ? null : set.name; item.componentRole = role; item.quantity = BigDecimal.ONE; em.persist(item);
    }

    private void createBalanceItem(Custody custody, StockBalance balance, EquipmentSet set, String role,
            BigDecimal quantity, LocalDateTime at) {
        var item = new CustodyItem(); item.custody = custody; item.balance = balance; item.equipmentSet = set;
        item.location = balance.location; item.issueMovement = movement(balance, "CUSTODY_ISSUE", quantity.negate(), at);
        item.modelName = balance.lot.model.name; item.assetCode = balance.lot.lotNumber; item.locationName = balance.location.name;
        item.equipmentSetCode = set.code; item.equipmentSetName = set.name; item.componentRole = role;
        item.quantity = quantity; em.persist(item);
    }

    private StockMovement movement(AssetItem asset, String nature, BigDecimal quantity, LocalDateTime at) {
        var movement = new StockMovement(); movement.asset = asset; movement.location = asset.location;
        movement.nature = nature; movement.quantity = quantity; movement.movedAt = at; movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement); return movement;
    }
    private StockMovement movement(StockBalance balance, String nature, BigDecimal quantity, LocalDateTime at) {
        var movement = new StockMovement(); movement.lot = balance.lot; movement.location = balance.location;
        movement.nature = nature; movement.quantity = quantity; movement.movedAt = at; movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement); return movement;
    }

    private boolean setCurrentlyAvailable(EquipmentSet set) {
        var components = em.createQuery("select c from EquipmentSetComponent c where c.equipmentSet.id = :set", EquipmentSetComponent.class)
            .setParameter("set", set.id).getResultList();
        return !components.isEmpty() && components.stream().allMatch(c -> c.asset != null
            ? "AVAILABLE".equals(c.asset.status) && (c.asset.validUntil == null || !c.asset.validUntil.isBefore(LocalDate.now()))
            : c.balance.available.compareTo(c.quantity) >= 0
                && (c.balance.lot.validUntil == null || !c.balance.lot.validUntil.isBefore(LocalDate.now())));
    }
    private long countComponents(long setId) { return em.createQuery(
        "select count(c) from EquipmentSetComponent c where c.equipmentSet.id = :id", Long.class)
        .setParameter("id", setId).getSingleResult(); }
    private long countActiveSets(long assetId) { return em.createQuery(
        "select count(c) from EquipmentSetComponent c where c.asset.id = :asset and c.equipmentSet.active = true", Long.class)
        .setParameter("asset", assetId).getSingleResult(); }

    private void validateCompleteSets(long custodyId, List<CustodyItem> selected) {
        Set<Long> ids = selected.stream().map(i -> i.id).collect(java.util.stream.Collectors.toSet());
        Set<Long> sets = selected.stream().filter(i -> i.equipmentSet != null).map(i -> i.equipmentSet.id)
            .collect(java.util.stream.Collectors.toSet());
        for (Long setId : sets) {
            var pending = em.createQuery("select i.id from CustodyItem i where i.custody.id = :custody and i.equipmentSet.id = :set and i.returnedAt is null", Long.class)
                .setParameter("custody", custodyId).setParameter("set", setId).getResultList();
            if (!ids.containsAll(pending)) bad("Return every pending component of an equipment set together.");
        }
    }
    private void lockCatalog() { em.createQuery("select c from ItemCategory c order by c.id", ItemCategory.class)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList(); }
    private OrganizationalUnit selectedUnit(long organizationId, Long unitId) {
        if (unitId == null) return null;
        var unit = em.find(OrganizationalUnit.class, unitId);
        if (unit == null || !unit.organization.id.equals(organizationId)) bad("Select a unit in the selected organization.");
        return unit;
    }
    private void validateIssue(IssueRequest request) {
        if (request == null || request.organizationId() == null || request.authorizerId() == null
                || request.purpose() == null || request.purpose().isBlank()) bad("Organization, recipient, authorizer and purpose are required.");
        if ((request.recipientId() == null) == (request.recipientUnitId() == null))
            bad("Select exactly one recipient: a person or an organizational unit.");
        uuid(request.requestId());
        if (request.purpose().trim().length() > 255) bad("Purpose must contain at most 255 characters.");
        var assets = values(request.assetIds()); var sets = values(request.equipmentSetIds());
        if (assets.isEmpty() && sets.isEmpty() || assets.size() + sets.size() > 100
                || assets.stream().anyMatch(id -> id == null || id <= 0) || sets.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(assets).size() != assets.size() || new HashSet<>(sets).size() != sets.size())
            bad("Select 1 to 100 distinct individual assets or equipment sets.");
    }
    private void validateReturn(ReturnRequest request) {
        if (request == null) bad("Return data is required."); uuid(request.requestId());
        if (request.itemIds() == null || request.itemIds().isEmpty() || request.itemIds().size() > 100
                || request.itemIds().stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(request.itemIds()).size() != request.itemIds().size()) bad("Select 1 to 100 distinct custody items.");
        if (request.inspectionNotes() != null && request.inspectionNotes().trim().length() > 500)
            bad("Inspection notes must contain at most 500 characters.");
    }
    private LocalDateTime parseDueAt(String value, LocalDateTime deliveredAt) {
        if (value == null || value.isBlank()) return null;
        try { var result = LocalDateTime.parse(value); if (!result.isAfter(deliveredAt)) throw new IllegalArgumentException(); return result; }
        catch (RuntimeException ex) { bad("Due date must be a valid future date and time."); return null; }
    }
    private static String issueFingerprint(IssueRequest request) {
        // Keep the existing fingerprint for person recipients so pre-upgrade retries still work.
        String recipientKey = request.recipientUnitId() == null ? String.valueOf(request.recipientId()) : "unit:" + request.recipientUnitId();
        return hash(request.organizationId() + "|" + request.unitId() + "|" + recipientKey + "|" + request.authorizerId()
            + "|" + request.purpose().trim() + "|" + request.dueAt() + "|" + values(request.assetIds()).stream().sorted().toList()
            + "|" + values(request.equipmentSetIds()).stream().sorted().toList());
    }
    private static String returnFingerprint(long custodyId, ReturnRequest request) {
        return hash(custodyId + "|" + request.itemIds().stream().sorted().toList() + "|" + request.conditionTypeId()
            + "|" + cleanNotes(request.inspectionNotes()));
    }
    private static String legacyReturnFingerprint(long custodyId, ReturnRequest request) {
        return hash(custodyId + "|" + request.itemIds().stream().sorted().toList());
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static List<Long> values(List<Long> values) { return values == null ? List.of() : values; }
    private CustodyReturnConditionType returnCondition(Long id) {
        CustodyReturnConditionType condition;
        if (id == null) condition = em.createQuery("select t from CustodyReturnConditionType t where t.code = 'GOOD'", CustodyReturnConditionType.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().orElse(null);
        else condition = em.find(CustodyReturnConditionType.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (condition == null || !condition.active) conflict("Select an active custody return condition.");
        return condition;
    }
    private static String cleanNotes(String notes) { return notes == null || notes.isBlank() ? null : notes.trim(); }
    private static void uuid(String value) {
        try { if (value == null || !UUID.fromString(value).toString().equals(value)) throw new IllegalArgumentException(); }
        catch (IllegalArgumentException ex) { bad("A canonical UUID request ID is required."); }
    }
    private <T> T locked(Class<T> type, Long id) {
        if (id == null || id <= 0) bad("A valid record ID is required.");
        var entity = em.find(type, id, LockModeType.PESSIMISTIC_WRITE); if (entity == null) bad(type.getSimpleName() + " not found."); return entity;
    }
    private static String escaped(String value) { return "%" + (value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%"; }
    private static String text(LocalDateTime value) { return value == null ? null : value.toString(); }
    private static void pagination(int page) { if (page < 0 || page > 100000) bad("Invalid page."); }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private static void notFound() { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Custody not found."); }
}
