package com.comandos.custody.service;

import com.comandos.audit.service.AuditService;
import com.comandos.core.service.ProductMasterDataReferenceSynchronizer;
import com.comandos.core.service.CanonicalMasterDataDirectory;
import com.comandos.core.service.ProductCanonicalScopeResolver;
import com.comandos.custody.dto.CustodyContract.*;
import com.comandos.custody.model.*;
import com.comandos.inventory.model.*;
import com.comandos.security.service.AccessPolicy;
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
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;

@Service
@Transactional(readOnly = true)
public class CustodyService {
    private static final int PAGE_SIZE = 20;
    private static final TenantId TENANT = TenantId.of("comandos");
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;
    private final ProductMasterDataReferenceSynchronizer masterDataReferences;
    private final ProductCanonicalScopeResolver canonicalScope;
    private final CanonicalMasterDataDirectory masterData;

    @org.springframework.beans.factory.annotation.Autowired
    public CustodyService(
            EntityManager em,
            AccessPolicy access,
            AuditService audit,
            ProductMasterDataReferenceSynchronizer masterDataReferences,
            ProductCanonicalScopeResolver canonicalScope,
            CanonicalMasterDataDirectory masterData) {
        this.em = em;
        this.access = access;
        this.audit = audit;
        this.masterDataReferences = masterDataReferences;
        this.canonicalScope = canonicalScope;
        this.masterData = masterData;
    }

    public Page<StockOption> stock(long organizationId, Long unitId, String search, int page) {
        access.requireScope("custodies", "READ", organizationId, unitId);
        selectedUnit(organizationId, unitId); pagination(page);
        boolean canonical = canonicalScope != null && canonicalScope.enabled();
        var canonicalIds = canonical ? canonicalScope.scope(organizationId, unitId) : null;
        String from = " from AssetItem a where "
            + (canonical ? "a.location.organizationCanonicalId" : "a.location.organizationLegacyId")
            + " = :organization"
            + (unitId == null ? "" : " and "
                + (canonical ? "a.location.unitCanonicalId" : "a.location.unitLegacyId")
                + " = :unit")
            + " and a.status = 'AVAILABLE'"
            + " and not exists (select c.id from EquipmentSetComponent c where c.asset = a and c.equipmentSet.active = true)"
            + " and (a.validUntil is null or a.validUntil >= :today)"
            + " and (lower(a.assetCode) like :search escape '!' or lower(a.serialNumber) like :search escape '!'"
            + " or lower(a.model.name) like :search escape '!')";
        var query = em.createQuery("select a" + from + " order by a.id", AssetItem.class);
        var count = em.createQuery("select count(a)" + from, Long.class);
        String term = escaped(search);
        for (var q : List.of(query, count)) {
            q.setParameter("organization", canonical ? canonicalIds.organizationId() : organizationId).setParameter("today", LocalDate.now()).setParameter("search", term);
            if (unitId != null) q.setParameter("unit", canonical ? canonicalIds.unitId() : unitId);
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
        var organization = organization(request.organizationId());
        var unit = selectedUnit(organization.id(), request.unitId());
        String fingerprint = issueFingerprint(request);
        var existing = em.createQuery("select c from Custody c where c.requestId = :requestId", Custody.class)
            .setParameter("requestId", request.requestId()).getResultStream().findFirst();
        if (existing.isPresent()) {
            access.requireEntity("custodies", "READ", existing.get());
            if (!existing.get().requestFingerprint.equals(fingerprint)) conflict("This request ID was already used for a different custody.");
            return view(existing.get());
        }
        var recipient = request.recipientId() == null ? null : person(request.recipientId());
        var recipientUnit = request.recipientUnitId() == null
            ? null
            : selectedUnit(organization.id(), request.recipientUnitId());
        var authorizer = person(request.authorizerId());
        access.requireAny("core/people", "READ");
        if (!organization.active()
                || unit != null && !unit.active()
                || recipient != null && !recipient.active()
                || !authorizer.active()) {
            bad("Organization, unit, recipient and authorizer must be active.");
        }
        var deliveredAt = LocalDateTime.now();
        var dueAt = parseDueAt(request.dueAt(), deliveredAt);
        var custody = new Custody();
        custody.organizationLegacyId = organization.id();
        custody.unitLegacyId = unit == null ? null : unit.id();
        custody.recipientLegacyId = recipient == null ? null : recipient.id();
        custody.recipientUnitLegacyId = recipientUnit == null ? null : recipientUnit.id();
        custody.authorizerLegacyId = authorizer.id();
        if (canonicalScope != null) {
            custody.organizationCanonicalId = canonicalScope.organization(organization.id());
            custody.unitCanonicalId = canonicalScope.unit(unit == null ? null : unit.id());
            custody.recipientCanonicalId = recipient == null ? null : canonicalScope.person(recipient.id());
            custody.recipientUnitCanonicalId = recipientUnit == null ? null : canonicalScope.unit(recipientUnit.id());
            custody.authorizerCanonicalId = canonicalScope.person(authorizer.id());
        }
        custody.organizationName = organization.name();
        custody.unitName = unit == null ? null : unit.name();
        custody.recipientName = recipient == null ? recipientUnit.name() : recipient.name();
        custody.authorizerName = authorizer.name();
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
                "before", Map.of("status", asset.status), "after", Map.of("status", AssetStatus.CUSTODIED.name())));
            asset.status = AssetStatus.CUSTODIED.name();
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
                        "before", Map.of("status", asset.status), "after", Map.of("status", AssetStatus.CUSTODIED.name())));
                    asset.status = AssetStatus.CUSTODIED.name();
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
                if (!AssetStatus.CUSTODIED.name().equals(asset.status)) conflict("Asset " + item.assetCode + " is no longer marked as custodied.");
                nextStatus = condition.blocksAvailability || asset.validUntil != null && asset.validUntil.isBefore(LocalDate.now())
                    ? AssetStatus.BLOCKED.name() : AssetStatus.AVAILABLE.name();
                movement = movement(asset, StockMovementNature.CUSTODY_RETURN.name(), BigDecimal.ONE, returnedAt, StockMovementReferenceType.CUSTODY_RETURN.name(), returned.id);
                asset.status = nextStatus;
                if (Set.of("GOOD", "NEEDS_INSPECTION", "DAMAGED").contains(condition.code)) asset.condition = condition.code;
            } else {
                movement = movement(balance, StockMovementNature.CUSTODY_RETURN.name(), item.quantity, returnedAt, StockMovementReferenceType.CUSTODY_RETURN.name(), returned.id);
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
                "before", Map.of("status", AssetStatus.CUSTODIED.name()), "after", Map.of("status", nextStatus)));
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
        boolean canonical = canonicalScope != null && canonicalScope.enabled();
        var canonicalIds = canonical ? canonicalScope.scope(organizationId, unitId) : null;
        String from = " from Custody c where "
            + (canonical ? "c.organizationCanonicalId" : "c.organizationLegacyId")
            + " = :organization"
            + (unitId == null ? "" : " and "
                + (canonical ? "c.unitCanonicalId" : "c.unitLegacyId")
                + " = :unit");
        var query = em.createQuery("select c" + from + " order by c.id desc", Custody.class);
        var count = em.createQuery("select count(c)" + from, Long.class);
        for (var q : List.of(query, count)) {
            q.setParameter("organization", canonical ? canonicalIds.organizationId() : organizationId);
            if (unitId != null) q.setParameter("unit", canonical ? canonicalIds.unitId() : unitId);
        }
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(this::view).toList(),
            count.getSingleResult(), page, PAGE_SIZE);
    }

    public CustodyView get(long id) {
        access.requireAny("custodies", "READ");
        var custody = em.find(Custody.class, id); if (custody == null) notFound();
        access.requireEntity("custodies", "READ", custody); return view(custody);
    }

    public Page<CustodyView> byAsset(long assetId, int page) {
        access.requireAny("custodies", "READ");
        pagination(page);
        String from = " from Custody c where exists (select i.id from CustodyItem i where i.custody = c and i.asset.id = :asset) and "
            + access.predicate("custodies", "READ", "c");
        var query = em.createQuery("select c" + from + " order by c.id desc", Custody.class).setParameter("asset", assetId);
        var count = em.createQuery("select count(c)" + from, Long.class).setParameter("asset", assetId);
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(this::view).toList(),
            count.getSingleResult(), page, PAGE_SIZE);
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
        return new CustodyView(custody.id, custody.organizationLegacyId, custody.organizationName,
            custody.unitLegacyId, custody.unitName,
            custody.recipientLegacyId,
            custody.recipientUnitLegacyId,
            custody.recipientUnitLegacyId == null ? "PERSON" : "UNIT", custody.recipientName,
            custody.authorizerLegacyId, custody.authorizerName, custody.purpose, custody.status, text(custody.deliveredAt),
            text(custody.dueAt), text(custody.completedAt), custody.issuedById, custody.issuedByLogin, items, returns);
    }

    private void validateAsset(AssetItem asset, long organizationId, Long unitId, boolean equipmentSetIssue) {
        access.requireScope("custodies", "CREATE", organizationId, unitId);
        if (canonicalScope != null && canonicalScope.enabled()) {
            var ids = canonicalScope.scope(organizationId, unitId);
            if (!asset.location.matchesCanonicalScope(ids.organizationId(), ids.unitId())) {
                bad("Every asset must belong to the selected organization and unit.");
            }
        } else if (!asset.location.organizationLegacyId.equals(organizationId)
                || unitId != null && (asset.location.unitLegacyId == null || !unitId.equals(asset.location.unitLegacyId))) {
            bad("Every asset must belong to the selected organization and unit.");
        }
        if (!AssetStatus.AVAILABLE.name().equals(asset.status)) conflict("Asset " + asset.assetCode + " is no longer available.");
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
        access.requireScope("custodies", "CREATE", organizationId, unitId);
        if (canonicalScope != null && canonicalScope.enabled()) {
            var ids = canonicalScope.scope(organizationId, unitId);
            if (!balance.location.matchesCanonicalScope(ids.organizationId(), ids.unitId())) {
                bad("Every stock balance must belong to the selected organization and unit.");
            }
        } else if (!balance.location.organizationLegacyId.equals(organizationId) || unitId != null
                && (balance.location.unitLegacyId == null || !unitId.equals(balance.location.unitLegacyId))) {
            bad("Every stock balance must belong to the selected organization and unit.");
        }
        if (balance.lot.validUntil != null && balance.lot.validUntil.isBefore(LocalDate.now()))
            bad("Expired stock cannot be issued in custody.");
        if (balance.available.compareTo(quantity) < 0) conflict("The equipment set quantity is no longer available.");
    }

    private void createAssetItem(Custody custody, AssetItem asset, EquipmentSet set, String role, LocalDateTime at) {
        var item = new CustodyItem(); item.custody = custody; item.asset = asset; item.equipmentSet = set;
        item.location = asset.location; item.issueMovement = movement(asset, StockMovementNature.CUSTODY_ISSUE.name(), BigDecimal.ONE.negate(), at, StockMovementReferenceType.CUSTODY.name(), custody.id);
        item.modelName = asset.model.name; item.assetCode = asset.assetCode; item.serialNumber = asset.serialNumber;
        item.locationName = asset.location.name; item.equipmentSetCode = set == null ? null : set.code;
        item.equipmentSetName = set == null ? null : set.name; item.componentRole = role; item.quantity = BigDecimal.ONE; em.persist(item);
    }

    private void createBalanceItem(Custody custody, StockBalance balance, EquipmentSet set, String role,
            BigDecimal quantity, LocalDateTime at) {
        var item = new CustodyItem(); item.custody = custody; item.balance = balance; item.equipmentSet = set;
        item.location = balance.location; item.issueMovement = movement(balance, StockMovementNature.CUSTODY_ISSUE.name(), quantity.negate(), at, StockMovementReferenceType.CUSTODY.name(), custody.id);
        item.modelName = balance.lot.model.name; item.assetCode = balance.lot.lotNumber; item.locationName = balance.location.name;
        item.equipmentSetCode = set.code; item.equipmentSetName = set.name; item.componentRole = role;
        item.quantity = quantity; em.persist(item);
    }

    private StockMovement movement(AssetItem asset, String nature, BigDecimal quantity, LocalDateTime at, String referenceType, Long referenceId) {
        var movement = new StockMovement(); movement.asset = asset; movement.location = asset.location;
        movement.nature = nature; movement.referenceType = referenceType; movement.referenceId = referenceId; movement.quantity = quantity; movement.movedAt = at; movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement); return movement;
    }
    private StockMovement movement(StockBalance balance, String nature, BigDecimal quantity, LocalDateTime at, String referenceType, Long referenceId) {
        var movement = new StockMovement(); movement.lot = balance.lot; movement.location = balance.location;
        movement.nature = nature; movement.referenceType = referenceType; movement.referenceId = referenceId; movement.quantity = quantity; movement.movedAt = at; movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement); return movement;
    }

    private boolean setCurrentlyAvailable(EquipmentSet set) {
        var components = em.createQuery("select c from EquipmentSetComponent c where c.equipmentSet.id = :set", EquipmentSetComponent.class)
            .setParameter("set", set.id).getResultList();
        return !components.isEmpty() && components.stream().allMatch(c -> c.asset != null
            ? AssetStatus.AVAILABLE.name().equals(c.asset.status) && (c.asset.validUntil == null || !c.asset.validUntil.isBefore(LocalDate.now()))
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
    private UnitSnapshot selectedUnit(long organizationId, Long unitId) {
        if (unitId == null) return null;
        var unit = masterData.findUnit(unitId, TENANT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Select a unit in the selected organization."
            ));
        if (!unit.active()
                || !CompanyId.of("comandos:organization:" + organizationId)
                    .equals(unit.companyId())) {
            bad("Select a unit in the selected organization.");
        }
        return new UnitSnapshot(unitId, unit.name(), true);
    }

    private OrganizationSnapshot organization(Long id) {
        if (id == null || id <= 0) bad("A valid organization is required.");
        var value = masterData.findOrganization(id, TENANT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Organization not found."
            ));
        return new OrganizationSnapshot(
            id,
            value.legalName(),
            value.status() == LifecycleStatus.ACTIVE
        );
    }

    private PersonSnapshot person(Long id) {
        if (id == null || id <= 0) bad("A valid person is required.");
        var value = masterData.findPerson(id, TENANT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Person not found."
            ));
        return new PersonSnapshot(
            id,
            value.name(),
            value.status() == LifecycleStatus.ACTIVE
        );
    }

    private record OrganizationSnapshot(Long id, String name, boolean active) {}
    private record UnitSnapshot(Long id, String name, boolean active) {}
    private record PersonSnapshot(Long id, String name, boolean active) {}

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
