package com.weaponsregistration.inventory.service;

import com.weaponsregistration.core.model.CoreEntity;
import com.weaponsregistration.core.service.CoreCatalog;
import com.weaponsregistration.inventory.model.*;
import com.weaponsregistration.reservation.model.ReservationStatusType;
import com.weaponsregistration.reconciliation.model.InventoryCountResultType;
import com.weaponsregistration.reconciliation.model.InventoryCountStatusType;
import com.weaponsregistration.sales.model.SaleReturnReasonType;
import com.weaponsregistration.custody.model.CustodyReturnConditionType;
import com.weaponsregistration.security.service.AccessPolicy;
import com.weaponsregistration.audit.service.AuditService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class InventoryService {
    private final EntityManager em;
    private final InventoryRules rules;
    private final AccessPolicy access;
    private final AuditService audit;
    public InventoryService(EntityManager em, InventoryRules rules, AccessPolicy access, AuditService audit) { this.em = em; this.rules = rules; this.access = access; this.audit = audit; }

    public record PageResult(List<Map<String, Object>> content, long totalElements, int page, int size) {}

    public PageResult list(String resource, String search, int page, int size, Long organizationId) {
        var spec = InventoryCatalog.get(resource);
        if (page < 0 || page > 100000 || size < 1 || size > 100) bad("Invalid pagination.");
        List<String> expressions = new ArrayList<>(List.of("cast(e.id as string)"));
        spec.fields().stream().filter(f -> Set.of("text", "choice").contains(f.type()))
            .forEach(f -> expressions.add("lower(e." + f.property() + ")"));
        boolean searchRequested = search != null && !search.isBlank();
        String where = searchRequested ? " where (" + String.join(" or ", expressions.stream().map(e -> e + " like :search escape '!'").toList()) + ")" : "";
        String organizationPath = switch (resource) {
            case "locations" -> "e.organization.id";
            case "assets", "balances", "movements" -> "e.location.organization.id";
            case "regulatory-controls" -> "e.asset.location.organization.id";
            case "expirations", "certifications", "recalls" -> "e.organization.id";
            case "recall-items" -> "e.recall.organization.id";
            case "equipment-sets" -> "e.organization.id";
            case "equipment-set-components" -> "e.equipmentSet.organization.id";
            case "lots" -> "e.openingLocation.organization.id";
            default -> null;
        };
        boolean scoped = organizationId != null && organizationPath != null;
        if (scoped) where += (searchRequested ? " and " : " where ") + organizationPath + " = :organizationId";
        where += (where.isEmpty() ? " where " : " and ") + access.predicate("inventory/" + resource, "READ", "e");
        var query = em.createQuery("select e from " + spec.entity().getSimpleName() + " e" + where + " order by e.id", spec.entity());
        var count = em.createQuery("select count(e) from " + spec.entity().getSimpleName() + " e" + where, Long.class);
        if (searchRequested) {
            String term = "%" + search.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            query.setParameter("search", term); count.setParameter("search", term);
        }
        if (scoped) { query.setParameter("organizationId", organizationId); count.setParameter("organizationId", organizationId); }
        return new PageResult(query.setFirstResult(page * size).setMaxResults(size).getResultList().stream()
            .map(row -> view(spec, row)).toList(), count.getSingleResult(), page, size);
    }

    public Map<String, Object> get(String resource, long id) {
        var spec = InventoryCatalog.get(resource);
        access.requireAny("inventory/" + resource, "READ");
        var entity = find(spec.entity(), id);
        access.requireEntity("inventory/" + resource, "READ", entity);
        return view(spec, entity);
    }

    @Transactional
    public Map<String, Object> save(String resource, Long id, Map<String, Object> data) {
        return save(resource, id, data, null);
    }

    @Transactional
    public Map<String, Object> receiveBoxes(Map<String, Object> data, String packaging) {
        return save("lots", null, data, packaging);
    }

    private Map<String, Object> save(String resource, Long id, Map<String, Object> data, String packaging) {
        var spec = InventoryCatalog.get(resource);
        String action = id == null ? "CREATE" : "UPDATE";
        access.requireAny("inventory/" + resource, action);
        writable(spec);
        var allowed = new HashSet<>(spec.fields().stream().filter(f -> !f.readOnly()).map(InventoryCatalog.Field::name).toList());
        allowed.add("version");
        if (!allowed.containsAll(data.keySet())) bad("The form contains unsupported or read-only fields.");
        lockCatalog();
        rules.lockOrganization(spec, data);
        CoreEntity entity;
        try { entity = id == null ? spec.entity().getConstructor().newInstance() : find(spec.entity(), id); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
        if (id != null) access.requireEntity("inventory/" + resource, action, entity);
        Map<String, Object> previous = id == null ? Map.of() : view(spec, entity);
        if (id != null && !Objects.equals(entity.version, integer(data.get("version"), true))) conflict("This record has changed. Reload before saving.");
        for (var field : spec.fields()) {
            if (field.readOnly()) continue;
            Object value = parse(field, data.get(field.name()));
            if (value instanceof CoreEntity reference) access.requireEntity(
                field.reference().startsWith("core/") ? field.reference() : "inventory/" + field.reference(), "READ", reference);
            if (id != null && field.createOnly()) {
                Object current = read(entity, field.property());
                boolean equal = current instanceof CoreEntity ref ? value instanceof CoreEntity other && Objects.equals(ref.id, other.id)
                    : current instanceof BigDecimal decimal && value instanceof BigDecimal other ? decimal.compareTo(other) == 0 : Objects.equals(current, value);
                if (!equal) bad(field.label() + " cannot be changed after registration.");
            }
            write(entity, field.property(), value);
        }
        access.requireEntity("inventory/" + resource, action, entity);
        rules.validate(entity, previous);
        if (id == null) {
            if (entity instanceof StockLot lot) lot.openingPackaging = packaging;
            if (entity instanceof StockLot lot) lot.availableQuantity = lot.initialQuantity;
            em.persist(entity);
            createOpening(entity);
        }
        em.flush();
        var result = view(spec, entity);
        audit.record("inventory/" + resource, entity.id, action, id == null ? null : previous, result);
        return result;
    }

    @Transactional
    public void delete(String resource, long id, Long version) {
        var spec = InventoryCatalog.get(resource);
        access.requireAny("inventory/" + resource, "DELETE");
        writable(spec);
        lockCatalog();
        var entity = find(spec.entity(), id);
        access.requireEntity("inventory/" + resource, "DELETE", entity);
        if (!Objects.equals(entity.version, version)) conflict("This record has changed. Reload before deleting.");
        rules.beforeDelete(entity);
        var before = view(spec, entity);
        em.remove(entity);
        em.flush();
        audit.record("inventory/" + resource, id, "DELETE", before, null);
    }

    private void lockCatalog() {
        // Serialize schema and asset writes before reading related category rows.
        // This also prevents concurrent parent changes from creating a cycle.
        em.createQuery("select c from ItemCategory c order by c.id", ItemCategory.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }

    private void createOpening(CoreEntity entity) {
        StockMovement movement = new StockMovement();
        movement.nature = "OPENING";
        movement.movedAt = LocalDateTime.now();
        if (entity instanceof StockLot lot) {
            StockBalance balance = new StockBalance();
            balance.lot = lot; balance.location = lot.openingLocation;
            balance.available = lot.initialQuantity;
            em.persist(balance);
            movement.lot = lot; movement.location = lot.openingLocation;
            movement.quantity = lot.initialQuantity;
        } else if (entity instanceof AssetItem asset) {
            movement.asset = asset; movement.location = asset.location; movement.quantity = BigDecimal.ONE;
        } else return;
        em.persist(movement);
    }

    private Object parse(InventoryCatalog.Field field, Object raw) {
        if (raw instanceof String text) raw = text.isBlank() ? null : text.trim();
        if (raw == null) {
            if (field.required()) bad(field.label() + " is required.");
            return null;
        }
        if (field.reference() != null) return find(referenceClass(field.reference()), integer(raw, false));
        if (field.type().equals("boolean")) {
            if (!(raw instanceof Boolean)) bad(field.label() + " must be true or false.");
            return raw;
        }
        if (field.type().equals("date")) {
            try { return LocalDate.parse(raw.toString()); }
            catch (RuntimeException ex) { bad(field.label() + " must be a valid date."); }
        }
        if (field.type().equals("decimal")) {
            try {
                var value = new BigDecimal(raw.toString()).stripTrailingZeros();
                if (value.signum() < 0 || Math.max(0, value.scale()) > 4 || value.precision() - value.scale() > 15) throw new IllegalArgumentException();
                return value;
            } catch (RuntimeException ex) { bad(field.label() + " must be nonnegative, with at most 15 integer and 4 decimal digits."); }
        }
        if (field.type().equals("integer")) {
            try {
                int value = new BigDecimal(raw.toString()).intValueExact();
                if (value <= 0) throw new IllegalArgumentException();
                return value;
            } catch (RuntimeException ex) { bad(field.label() + " must be a positive whole number."); }
        }
        if (!(raw instanceof String) || raw.toString().length() > 255) bad(field.label() + " must contain at most 255 characters.");
        if (!field.choices().isEmpty() && !field.choices().contains(raw)) bad(field.label() + " is invalid.");
        return raw;
    }

    private Class<? extends CoreEntity> referenceClass(String reference) {
        return reference.startsWith("core/") ? CoreCatalog.get(reference.substring(5)).entity() : InventoryCatalog.get(reference).entity();
    }

    private CoreEntity find(Class<? extends CoreEntity> type, long id) {
        var entity = em.find(type, id);
        if (entity == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Referenced record not found.");
        return entity;
    }

    private Map<String, Object> view(InventoryCatalog.Resource spec, CoreEntity entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", entity.id); result.put("version", entity.version);
        result.put("createdAt", entity.createdAt); result.put("updatedAt", entity.updatedAt);
        result.put("label", label(entity));
        Map<String, String> labels = new LinkedHashMap<>();
        for (var field : spec.fields()) {
            Object value = read(entity, field.property());
            if (value instanceof CoreEntity ref) { result.put(field.name(), ref.id); labels.put(field.name(), label(ref)); }
            else result.put(field.name(), value instanceof BigDecimal decimal ? decimal.toPlainString() : value);
        }
        result.put("referenceLabels", labels);
        return result;
    }

    private String label(CoreEntity entity) {
        String name = null;
        for (String field : List.of("name", "assetCode", "lotNumber", "value", "nature")) {
            try { Object value = entity.getClass().getField(field).get(entity); if (value != null) { name = value.toString(); break; } }
            catch (NoSuchFieldException ignored) { }
            catch (IllegalAccessException ex) { throw new IllegalStateException(ex); }
        }
        if (entity instanceof StockBalance balance) name = balance.lot.lotNumber + " / " + balance.location.name;
        if (entity instanceof EquipmentSet set) name = set.code + " / " + set.name;
        if (entity instanceof EquipmentSetComponent component) name = component.equipmentSet.code + " / "
            + (component.asset == null ? component.balance.lot.lotNumber : component.asset.assetCode);
        if (entity instanceof CategoryCharacteristic binding) name = binding.category.name + " / " + binding.characteristic.name;
        if (entity instanceof FirearmSpecification specification) name = specification.model.name + " / " + specification.caliber;
        if (entity instanceof AmmunitionSpecification specification) name = specification.model.name + " / " + specification.caliber;
        if (entity instanceof GrenadeSpecification specification) name = specification.model.name + " / " + specification.grenadeType;
        if (entity instanceof SpraySpecification specification) name = specification.model.name + " / " + specification.agent;
        if (entity instanceof BallisticProtectionSpecification specification) name = specification.model.name + " / " + specification.protectionLevel;
        if (entity instanceof ElectricalDeviceSpecification specification) name = specification.model.name + " / " + specification.cartridgeType;
        if (entity instanceof OpticalSpecification specification) name = specification.model.name + " / " + specification.opticalType;
        if (entity instanceof RegulatoryControl control) name = control.registrationNumber + " / " + control.asset.assetCode;
        if (entity instanceof ExpirationRecord expiration) name = expiration.type + " / " + expiration.expirationDate;
        if (entity instanceof CertificationRecord certification) name = certification.type + " / " + certification.number;
        if (entity instanceof Recall recall) name = recall.number;
        if (entity instanceof RecallItem item) name = item.recall.number + " / " + (item.asset == null ? item.lot.lotNumber : item.asset.assetCode);
        if (entity instanceof ReservationStatusType status) name = status.code + " / " + status.name;
        if (entity instanceof InventoryCountStatusType status) name = status.code + " / " + status.name;
        if (entity instanceof InventoryCountResultType result) name = result.code + " / " + result.name;
        if (entity instanceof SaleReturnReasonType reason) name = reason.code + " / " + reason.name;
        if (entity instanceof CustodyReturnConditionType condition) name = condition.code + " / " + condition.name;
        return (name == null ? entity.getClass().getSimpleName() : name) + " (#" + entity.id + ")";
    }

    private Object read(CoreEntity entity, String field) {
        try { return entity.getClass().getField(field).get(entity); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    private void write(CoreEntity entity, String field, Object value) {
        try { entity.getClass().getField(field).set(entity, value); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    private Long integer(Object raw, boolean allowZero) {
        try { long value = new BigDecimal(String.valueOf(raw)).longValueExact(); if (value < (allowZero ? 0 : 1)) throw new IllegalArgumentException(); return value; }
        catch (RuntimeException ex) { bad("Invalid ID or version."); return null; }
    }

    private void writable(InventoryCatalog.Resource spec) {
        if (spec.readOnly()) throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Stock balances and movements are maintained by inventory operations.");
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
