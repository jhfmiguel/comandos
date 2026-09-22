package com.comandos.consumption.service;

import com.comandos.audit.service.AuditService;
import com.comandos.consumption.dto.AmmunitionConsumptionContract.*;
import com.comandos.consumption.model.*;
import com.comandos.core.model.*;
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

@Service
@Transactional(readOnly = true)
public class AmmunitionConsumptionService {
    private static final int PAGE_SIZE = 20;
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;
    public AmmunitionConsumptionService(EntityManager em, AccessPolicy access, AuditService audit) {
        this.em = em; this.access = access; this.audit = audit;
    }

    public Page<StockOption> stock(long organizationId, Long unitId, String search, int page) {
        access.requireScope("ammunition-consumptions", "READ", organizationId, unitId);
        selectedUnit(organizationId, unitId); pagination(page);
        String from = " from StockBalance b where b.location.organization.id = :organization"
            + (unitId == null ? "" : " and b.location.unit.id = :unit")
            + " and b.lot.model.category.family = 'AMMUNITION' and b.lot.model.category.lotControlled = true"
            + " and b.lot.model.category.serialized = false and b.lot.model.category.consumable = true"
            + " and b.available > 0 and b.lot.availableQuantity > 0"
            + " and (b.lot.validUntil is null or b.lot.validUntil >= :today)"
            + " and (lower(b.lot.model.sku) like :search escape '!' or lower(b.lot.model.name) like :search escape '!'"
            + " or lower(b.lot.lotNumber) like :search escape '!' or lower(b.location.name) like :search escape '!')";
        var query = em.createQuery("select b" + from + " order by b.lot.validUntil, b.id", StockBalance.class);
        var count = em.createQuery("select count(b)" + from, Long.class);
        for (var q : List.of(query, count)) {
            q.setParameter("organization", organizationId).setParameter("today", LocalDate.now()).setParameter("search", escaped(search));
            if (unitId != null) q.setParameter("unit", unitId);
        }
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(this::stockView).toList(),
            count.getSingleResult(), page, PAGE_SIZE);
    }

    @Transactional
    public ConsumptionView finalize(FinalizeRequest request) {
        validate(request);
        access.requireScope("ammunition-consumptions", "CREATE", request.organizationId(), request.unitId());
        lockCatalog();
        var organization = locked(Organization.class, request.organizationId());
        var unit = selectedUnit(organization.id, request.unitId());
        String fingerprint = fingerprint(request);
        var existing = em.createQuery("select c from AmmunitionConsumption c where c.requestId = :requestId", AmmunitionConsumption.class)
            .setParameter("requestId", request.requestId()).getResultStream().findFirst();
        if (existing.isPresent()) {
            access.requireEntity("ammunition-consumptions", "READ", existing.get());
            if (!existing.get().requestFingerprint.equals(fingerprint)) conflict("This request ID was already used for a different consumption.");
            return view(existing.get());
        }
        var responsible = locked(Person.class, request.responsibleId());
        var authorizer = locked(Person.class, request.authorizerId());
        access.requireEntity("core/people", "READ", responsible);
        access.requireEntity("core/people", "READ", authorizer);
        if (!organization.active || !responsible.active || !authorizer.active)
            bad("Organization, responsible person and authorizer must be active.");
        var now = LocalDateTime.now();
        var consumption = new AmmunitionConsumption();
        consumption.organization = organization; consumption.unit = unit; consumption.responsible = responsible; consumption.authorizer = authorizer;
        consumption.organizationName = organization.name; consumption.unitName = unit == null ? null : unit.name;
        consumption.responsibleName = responsible.fullName; consumption.authorizerName = authorizer.fullName;
        consumption.purpose = request.purpose().trim();
        consumption.activityType = request.activityType() == null || request.activityType().isBlank() ? "OPERATION" : request.activityType().trim().toUpperCase(Locale.ROOT);
        consumption.operationTraining = request.operationTraining() == null || request.operationTraining().isBlank() ? null : request.operationTraining().trim();
        consumption.consumedAt = now;
        consumption.requestId = request.requestId(); consumption.requestFingerprint = fingerprint;
        var actor = audit.actor(); consumption.finalizedById = actor.id(); consumption.finalizedByLogin = actor.login(); em.persist(consumption);
        List<Map<String, Object>> stockChanges = new ArrayList<>();
        for (var line : request.items().stream().sorted(Comparator.comparing(LineRequest::balanceId)).toList()) {
            var balance = locked(StockBalance.class, line.balanceId());
            var lot = locked(StockLot.class, balance.lot.id);
            BigDecimal delivered = line.deliveredQuantity() == null ? line.quantity() : line.deliveredQuantity();
            BigDecimal used = line.usedQuantity() == null ? line.quantity() : line.usedQuantity();
            BigDecimal returned = line.returnedQuantity() == null ? delivered.subtract(used) : line.returnedQuantity();
            if (delivered == null || used == null || returned == null || delivered.signum() <= 0 || used.signum() < 0 || returned.signum() < 0
                    || used.add(returned).compareTo(delivered) != 0) bad("Delivered quantity must equal used plus returned quantity.");
            validateStock(balance, lot, organization.id, request.unitId(), used);
            BigDecimal oldBalance = balance.available; BigDecimal oldLot = lot.availableQuantity;
            balance.available = oldBalance.subtract(used); lot.availableQuantity = oldLot.subtract(used);
            var movement = new StockMovement(); movement.lot = lot; movement.location = balance.location;
            movement.nature = "CONSUMPTION_DEFLAGRATION"; movement.quantity = used.negate(); movement.movedAt = now; movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement);
            var item = new AmmunitionConsumptionItem(); item.consumption = consumption; item.lot = lot; item.balance = balance;
            item.location = balance.location; item.movement = movement; item.modelName = lot.model.name; item.sku = lot.model.sku;
            item.lotNumber = lot.lotNumber; item.locationName = balance.location.name; item.unitOfMeasure = lot.model.unitOfMeasure;
            item.quantity = used; item.deliveredQuantity = delivered; item.usedQuantity = used; item.returnedQuantity = returned;
            item.result = line.result().trim(); em.persist(item);
            stockChanges.add(Map.of("balanceId", balance.id, "lotId", lot.id, "before", oldBalance, "after", balance.available,
                "quantity", used, "movementNature", movement.nature));
        }
        em.flush();
        var result = view(consumption);
        audit.record("ammunition-consumptions", consumption.id, "FINALIZE", null,
            Map.of("consumption", result, "stockChanges", stockChanges));
        return result;
    }

    public Page<ConsumptionView> list(long organizationId, Long unitId, int page) {
        access.requireScope("ammunition-consumptions", "READ", organizationId, unitId);
        selectedUnit(organizationId, unitId); pagination(page);
        String from = " from AmmunitionConsumption c where c.organization.id = :organization" + (unitId == null ? "" : " and c.unit.id = :unit");
        var query = em.createQuery("select c" + from + " order by c.id desc", AmmunitionConsumption.class);
        var count = em.createQuery("select count(c)" + from, Long.class);
        for (var q : List.of(query, count)) { q.setParameter("organization", organizationId); if (unitId != null) q.setParameter("unit", unitId); }
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(this::view).toList(),
            count.getSingleResult(), page, PAGE_SIZE);
    }
    public ConsumptionView get(long id) {
        access.requireAny("ammunition-consumptions", "READ");
        var value = em.find(AmmunitionConsumption.class, id); if (value == null) notFound();
        access.requireEntity("ammunition-consumptions", "READ", value); return view(value);
    }
    private ConsumptionView view(AmmunitionConsumption value) {
        var items = em.createQuery("select i from AmmunitionConsumptionItem i where i.consumption.id = :id order by i.id", AmmunitionConsumptionItem.class)
            .setParameter("id", value.id).getResultList().stream().map(i -> new ItemView(i.id, i.lot.id, i.balance.id, i.sku,
                i.modelName, i.lotNumber, i.locationName, i.unitOfMeasure, i.quantity, i.result, i.movement.id,
                i.deliveredQuantity, i.usedQuantity, i.returnedQuantity)).toList();
        return new ConsumptionView(value.id, value.organization.id, value.organizationName, value.unit == null ? null : value.unit.id,
            value.unitName, value.responsible.id, value.responsibleName, value.authorizer.id, value.authorizerName, value.purpose,
            value.status, value.consumedAt.toString(), value.finalizedById, value.finalizedByLogin, items,
            value.activityType, value.operationTraining);
    }
    private StockOption stockView(StockBalance b) { return new StockOption(b.id, b.lot.id, b.lot.model.sku, b.lot.model.name,
        b.lot.lotNumber, b.location.name, b.lot.model.unitOfMeasure, b.available,
        b.lot.validUntil == null ? null : b.lot.validUntil.toString()); }
    private void validateStock(StockBalance b, StockLot lot, long organizationId, Long unitId, BigDecimal quantity) {
        access.requireScope("ammunition-consumptions", "CREATE", b.location.organization.id, b.location.unit == null ? null : b.location.unit.id);
        if (!b.location.organization.id.equals(organizationId) || unitId != null && (b.location.unit == null || !unitId.equals(b.location.unit.id)))
            bad("Every ammunition lot must belong to the selected organization and unit.");
        var category = lot.model.category;
        if (!"AMMUNITION".equals(category.family) || !Boolean.TRUE.equals(category.lotControlled)
                || Boolean.TRUE.equals(category.serialized) || !Boolean.TRUE.equals(category.consumable))
            bad("Only lot-controlled, non-serialized consumable ammunition can be consumed.");
        if (lot.validUntil != null && lot.validUntil.isBefore(LocalDate.now())) bad("Expired ammunition cannot be consumed.");
        if (b.available.compareTo(quantity) < 0 || lot.availableQuantity.compareTo(quantity) < 0)
            conflict("The selected ammunition lot no longer has enough available stock.");
    }
    private void validate(FinalizeRequest r) {
        if (r == null || r.organizationId() == null || r.responsibleId() == null || r.authorizerId() == null
                || r.purpose() == null || r.purpose().isBlank()) bad("Organization, responsible person, authorizer and purpose are required.");
        uuid(r.requestId()); if (r.purpose().trim().length() > 255) bad("Purpose must contain at most 255 characters.");
        if (r.items() == null || r.items().isEmpty() || r.items().size() > 100) bad("Select 1 to 100 ammunition lots.");
        var ids = new HashSet<Long>();
        for (var line : r.items()) {
            if (line == null || line.balanceId() == null || line.balanceId() <= 0 || line.quantity() == null
                    || line.quantity().signum() <= 0 || line.quantity().scale() > 4 || line.result() == null || line.result().isBlank())
                bad("Every item requires a distinct balance, a positive quantity with at most four decimals, and a result.");
            if (!ids.add(line.balanceId())) bad("Each stock balance can appear only once.");
            if (line.result().trim().length() > 255) bad("Result must contain at most 255 characters.");
        }
    }
    private void lockCatalog() { em.createQuery("select c from ItemCategory c order by c.id", ItemCategory.class)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList(); }
    private OrganizationalUnit selectedUnit(long organizationId, Long unitId) {
        if (unitId == null) return null; var unit = em.find(OrganizationalUnit.class, unitId);
        if (unit == null || !unit.organization.id.equals(organizationId)) bad("Select a unit in the selected organization."); return unit;
    }
    private static String fingerprint(FinalizeRequest r) {
        String lines = r.items().stream().sorted(Comparator.comparing(LineRequest::balanceId))
            .map(i -> i.balanceId() + ":" + i.quantity().stripTrailingZeros().toPlainString() + ":" + i.result().trim()).toList().toString();
        return hash(r.organizationId() + "|" + r.unitId() + "|" + r.responsibleId() + "|" + r.authorizerId() + "|" + r.purpose().trim() + "|" + r.activityType() + "|" + r.operationTraining() + "|" + lines);
    }
    private static String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(value.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); } }
    private static void uuid(String value) { try { if (value == null || !UUID.fromString(value).toString().equals(value)) throw new IllegalArgumentException(); }
        catch (IllegalArgumentException ex) { bad("A canonical UUID request ID is required."); } }
    private <T> T locked(Class<T> type, Long id) { if (id == null || id <= 0) bad("A valid record ID is required.");
        var value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE); if (value == null) bad(type.getSimpleName() + " not found."); return value; }
    private static String escaped(String value) { return "%" + (value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%"; }
    private static void pagination(int page) { if (page < 0 || page > 100000) bad("Invalid page."); }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private static void notFound() { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ammunition consumption not found."); }
}
