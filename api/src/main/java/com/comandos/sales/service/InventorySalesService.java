package com.comandos.sales.service;

import com.comandos.core.model.*;
import com.comandos.inventory.model.*;
import com.comandos.reconciliation.model.InventoryCountItem;
import com.comandos.sales.dto.SalesContract.*;
import com.comandos.sales.model.*;
import com.comandos.security.service.AccessPolicy;
import com.comandos.audit.service.AuditService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class InventorySalesService {
    private static final int PAGE_SIZE = 20;
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;
    public InventorySalesService(EntityManager em, AccessPolicy access, AuditService audit) { this.em = em; this.access = access; this.audit = audit; }

    public Page<StockOption> stock(long organizationId, Long unitId, String kind, String search, int page) {
        access.requireScope("sales", "READ", organizationId, unitId);
        selectedUnit(organizationId, unitId);
        pagination(page);
        boolean assets = "ASSET".equals(kind);
        if (!assets && !"LOT".equals(kind)) bad("Stock kind must be ASSET or LOT.");
        String model = assets ? "e.model" : "e.lot.model";
        String code = assets ? "e.assetCode" : "e.lot.lotNumber";
        String expiry = assets ? "e.validUntil" : "e.lot.validUntil";
        String from = " from " + (assets ? "AssetItem" : "StockBalance") + " e where e.location.organization.id = :organization"
            + (unitId == null ? "" : " and e.location.unit.id = :unit")
            + (assets ? " and e.status = 'AVAILABLE'" : " and e.available > 0")
            + " and (" + expiry + " is null or " + expiry + " >= :today)"
            + " and (lower(" + code + ") like :search escape '!' or lower(" + model + ".name) like :search escape '!'"
            + " or lower(" + model + ".sku) like :search escape '!')";
        String term = "%" + search.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var query = em.createQuery("select e" + from + " order by e.id", CoreEntity.class);
        var count = em.createQuery("select count(e)" + from, Long.class);
        for (var q : List.of(query, count)) q.setParameter("organization", organizationId).setParameter("today", LocalDate.now()).setParameter("search", term);
        if (unitId != null) for (var q : List.of(query, count)) q.setParameter("unit", unitId);
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(e -> {
            if (e instanceof AssetItem a) return new StockOption("ASSET", a.id, a.assetCode, a.model.name, a.model.sku,
                a.location.name, a.model.unitOfMeasure, "1", decimal(a.model.listPrice));
            var b = (StockBalance) e;
            return new StockOption("LOT", b.id, b.lot.lotNumber, b.lot.model.name, b.lot.model.sku,
                b.location.name, b.lot.model.unitOfMeasure, decimal(b.available), decimal(b.lot.model.listPrice));
        }).toList(), count.getSingleResult(), page, PAGE_SIZE);
    }

    @Transactional
    public SaleView finalizeSale(FinalizeRequest request) {
        validateRequest(request);
        access.requireScope("sales", "CREATE", request.organizationId(), request.unitId());
        // Same lock order as inventory registration: catalog, then organization.
        // This also protects prices, expiry dates and manual availability changes.
        em.createQuery("select c from ItemCategory c order by c.id", ItemCategory.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        var organization = locked(Organization.class, request.organizationId());
        String fingerprint = fingerprint(request);
        var existing = em.createQuery("select s from InventorySale s where s.requestId = :key", InventorySale.class)
            .setParameter("key", request.requestId()).getResultStream().findFirst();
        if (existing.isPresent()) {
            access.requireEntity("sales", "READ", existing.get());
            if (!existing.get().requestFingerprint.equals(fingerprint)) conflict("This request ID was already used for a different sale.");
            return view(existing.get());
        }
        var unit = selectedUnit(organization.id, request.unitId());
        var buyer = locked(Person.class, request.buyerId());
        access.requireEntity("core/people", "READ", buyer);
        if (!organization.active || !buyer.active) bad("Organization and buyer must be active.");
        var sale = new InventorySale();
        sale.organization = organization;
        sale.unit = unit;
        sale.unitName = unit == null ? null : unit.name;
        sale.buyer = buyer;
        sale.organizationName = organization.name;
        sale.buyerName = buyer.fullName;
        sale.paymentMethod = request.paymentMethod();
        sale.processNumber = request.processNumber() == null || request.processNumber().isBlank() ? null : request.processNumber().trim();
        sale.legalBasis = request.legalBasis() == null || request.legalBasis().isBlank() ? null : request.legalBasis().trim();
        sale.documentReference = request.documentReference() == null || request.documentReference().isBlank() ? null : request.documentReference().trim();
        sale.finalizedAt = LocalDateTime.now();
        sale.withdrawnAt = sale.finalizedAt;
        sale.withdrawnByLogin = audit.actor().login();
        var actor = audit.actor();
        sale.finalizedById = actor.id();
        sale.finalizedByLogin = actor.login();
        sale.total = BigDecimal.ZERO;
        sale.requestId = request.requestId();
        sale.requestFingerprint = fingerprint;
        em.persist(sale);
        List<Map<String, Object>> stockChanges = new ArrayList<>();
        for (var requested : request.items()) {
            var item = new InventorySaleItem();
            item.sale = sale;
            item.quantity = requested.quantity();
            if (requested.assetId() != null) {
                var asset = locked(AssetItem.class, requested.assetId());
                validateLocation(asset.location, organization.id, request.unitId());
                if (!"AVAILABLE".equals(asset.status)) conflict("Asset " + asset.assetCode + " is no longer available.");
                notExpired(asset.validUntil);
                if (item.quantity.compareTo(BigDecimal.ONE) != 0) bad("Individual assets require quantity 1.");
                item.asset = asset;
                item.model = asset.model;
                item.location = asset.location;
                item.stockCode = asset.assetCode;
                stockChanges.add(Map.of("resource", "inventory/assets", "recordId", asset.id,
                    "before", Map.of("status", asset.status), "after", Map.of("status", "SOLD")));
                asset.status = "SOLD";
            } else {
                var balance = locked(StockBalance.class, requested.balanceId());
                validateLocation(balance.location, organization.id, request.unitId());
                var lot = locked(StockLot.class, balance.lot.id);
                notExpired(lot.validUntil);
                if (balance.available.compareTo(item.quantity) < 0 || lot.availableQuantity.compareTo(item.quantity) < 0)
                    conflict("Insufficient available stock for lot " + lot.lotNumber + ". Refresh stock and try again.");
                item.lot = lot;
                item.model = lot.model;
                item.location = balance.location;
                item.stockCode = lot.lotNumber;
                stockChanges.add(Map.of("resource", "inventory/balances", "recordId", balance.id,
                    "before", Map.of("available", balance.available.toPlainString()),
                    "after", Map.of("available", balance.available.subtract(item.quantity).toPlainString())));
                stockChanges.add(Map.of("resource", "inventory/lots", "recordId", lot.id,
                    "before", Map.of("availableQuantity", lot.availableQuantity.toPlainString()),
                    "after", Map.of("availableQuantity", lot.availableQuantity.subtract(item.quantity).toPlainString())));
                balance.available = balance.available.subtract(item.quantity);
                lot.availableQuantity = lot.availableQuantity.subtract(item.quantity);
            }
            item.unitPrice = item.model.listPrice;
            if (item.unitPrice.compareTo(requested.expectedUnitPrice()) != 0)
                conflict("The price of " + item.model.name + " has changed. Remove the item and select it again.");
            item.subtotal = item.unitPrice.multiply(item.quantity).setScale(4, RoundingMode.HALF_UP);
            amount(item.subtotal, false);
            sale.total = sale.total.add(item.subtotal);
            amount(sale.total, false);
            item.modelName = item.model.name;
            item.sku = item.model.sku;
            item.unitOfMeasure = item.model.unitOfMeasure;
            item.locationName = item.location.name;
            var movement = new StockMovement();
            movement.asset = item.asset;
            movement.lot = item.lot;
            movement.location = item.location;
            movement.nature = "SALE";
            movement.quantity = item.quantity.negate();
            movement.movedAt = sale.finalizedAt;
            movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement);
            item.movement = movement;
            em.persist(item);
        }
        em.flush();
        var result = view(sale);
        audit.record("sales", sale.id, "FINALIZE", null, Map.of("sale", result, "stockChanges", stockChanges));
        return result;
    }

    public Page<SaleView> list(long organizationId, Long unitId, int page) {
        access.requireScope("sales", "READ", organizationId, unitId);
        selectedUnit(organizationId, unitId);
        pagination(page);
        String from = " from InventorySale s where s.organization.id = :organization" + (unitId == null ? "" : " and s.unit.id = :unit");
        var query = em.createQuery("select s" + from + " order by s.id desc", InventorySale.class);
        var count = em.createQuery("select count(s)" + from, Long.class);
        for (var q : List.of(query, count)) {
            q.setParameter("organization", organizationId);
            if (unitId != null) q.setParameter("unit", unitId);
        }
        var rows = query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList();
        return new Page<>(rows.stream().map(this::view).toList(), count.getSingleResult(), page, PAGE_SIZE);
    }

    public SaleView get(long id) {
        access.requireAny("sales", "READ");
        var sale = em.find(InventorySale.class, id);
        if (sale == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sale not found.");
        access.requireEntity("sales", "READ", sale);
        return view(sale);
    }

    @Transactional
    public ReturnView returnItems(long saleId, ReturnRequest request) {
        if (request == null || request.reasonId() == null || request.notes() == null || request.notes().isBlank())
            bad("Reason and notes are required.");
        canonicalUuid(request.requestId());
        if (request.items() == null || request.items().isEmpty() || request.items().size() > 100)
            bad("A return requires 1 to 100 items.");
        var sale = locked(InventorySale.class, saleId);
        access.requireEntity("sales", request.cancellation() ? "CANCEL" : "RETURN", sale);
        String fingerprint = returnFingerprint(saleId, request);
        var existing = em.createQuery("select r from SaleReturn r where r.requestId=:id", SaleReturn.class)
            .setParameter("id", request.requestId()).getResultStream().findFirst();
        if (existing.isPresent()) {
            access.requireEntity("sales", "READ", existing.get().sale);
            if (!existing.get().requestFingerprint.equals(fingerprint)) conflict("Request ID already used for another return.");
            return returnView(existing.get());
        }
        var reason = locked(SaleReturnReasonType.class, request.reasonId());
        if (!reason.active) bad("The selected return reason is inactive.");
        var saleItems = em.createQuery("select i from InventorySaleItem i where i.sale.id=:id order by i.id", InventorySaleItem.class)
            .setParameter("id", saleId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        Map<Long,InventorySaleItem> byId = new HashMap<>(); saleItems.forEach(i -> byId.put(i.id, i));
        Map<Long,BigDecimal> remaining = new HashMap<>();
        for (var i : saleItems) remaining.put(i.id, i.quantity.subtract(returned(i.id)));
        Set<Long> unique = new HashSet<>();
        for (var line : request.items()) {
            if (line == null || line.saleItemId() == null || !unique.add(line.saleItemId()) || !byId.containsKey(line.saleItemId()))
                bad("Return items must be unique and belong to the sale.");
            amount(line.quantity(), true);
            if (line.quantity().compareTo(remaining.get(line.saleItemId())) > 0) conflict("Return quantity exceeds the remaining sold quantity.");
            if (byId.get(line.saleItemId()).asset != null && line.quantity().compareTo(BigDecimal.ONE) != 0)
                bad("Individual assets require quantity 1.");
        }
        var outstanding = remaining.entrySet().stream().filter(e -> e.getValue().signum() > 0).map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
        if (request.cancellation() && (!unique.equals(outstanding) || request.items().stream().anyMatch(l -> l.quantity().compareTo(remaining.get(l.saleItemId())) != 0)))
            bad("Cancellation must return every remaining sale item in full.");
        var operation = new SaleReturn(); operation.sale=sale;operation.reason=reason;operation.cancellation=request.cancellation();
        operation.notes=request.notes().trim();if(operation.notes.length()>1000)bad("Return notes are too long.");
        operation.refundReference=request.refundReference()==null||request.refundReference().isBlank()?null:request.refundReference().trim();
        if(operation.refundReference!=null&&operation.refundReference.length()>255)bad("Refund reference is too long.");
        operation.returnedAt=LocalDateTime.now();operation.refundAmount=BigDecimal.ZERO;var actor=audit.actor();operation.operatorId=actor.id();operation.operatorLogin=actor.login();operation.requestId=request.requestId();operation.requestFingerprint=fingerprint;em.persist(operation);
        List<Map<String,Object>> changes = new ArrayList<>();
        for (var line : request.items()) {
            var sold = byId.get(line.saleItemId()); var item = new SaleReturnItem(); item.saleReturn=operation;item.saleItem=sold;item.quantity=line.quantity();item.refundAmount=sold.unitPrice.multiply(line.quantity()).setScale(4,RoundingMode.HALF_UP);operation.refundAmount=operation.refundAmount.add(item.refundAmount);
            var movement=new StockMovement();movement.asset=sold.asset;movement.lot=sold.lot;movement.location=sold.location;movement.nature=request.cancellation()?"SALE_CANCELLATION":"SALE_RETURN";movement.quantity=line.quantity();movement.movedAt=operation.returnedAt;movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement);item.movement=movement;
            if(sold.asset!=null){var asset=locked(AssetItem.class,sold.asset.id);if(!"SOLD".equals(asset.status))conflict("The sold asset cannot be returned in its current state.");asset.status=canRestore(asset)?"AVAILABLE":"BLOCKED";changes.add(Map.of("assetId",asset.id,"after",asset.status));}
            else{var balance=em.createQuery("select b from StockBalance b where b.lot.id=:lot and b.location.id=:location",StockBalance.class).setParameter("lot",sold.lot.id).setParameter("location",sold.location.id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().orElseGet(()->{var b=new StockBalance();b.lot=sold.lot;b.location=sold.location;em.persist(b);return b;});var lot=locked(StockLot.class,sold.lot.id);balance.available=balance.available.add(line.quantity());lot.availableQuantity=lot.availableQuantity.add(line.quantity());changes.add(Map.of("balanceId",balance.id,"quantity",decimal(line.quantity())));}
            em.persist(item);
        }
        em.flush();var value=returnView(operation);audit.record("sales",sale.id,request.cancellation()?"CANCEL":"RETURN",null,Map.of("return",value,"stockChanges",changes));return value;
    }

    private SaleView view(InventorySale sale) {
        var items = em.createQuery("select i from InventorySaleItem i where i.sale.id = :sale order by i.id", InventorySaleItem.class)
            .setParameter("sale", sale.id).getResultList().stream().map(i -> new LineView(i.id, i.model.id,
                i.asset == null ? null : i.asset.id, i.lot == null ? null : i.lot.id, i.location.id, i.movement.id,
                i.modelName, i.sku, i.stockCode, i.locationName, i.unitOfMeasure, decimal(i.quantity), decimal(i.unitPrice), decimal(i.subtotal))).toList();
        return new SaleView(sale.id, sale.organization.id, sale.organizationName, sale.buyer.id, sale.buyerName,
            sale.paymentMethod.name(), sale.status, sale.finalizedAt.toString(), decimal(sale.total), items, sale.finalizedById, sale.finalizedByLogin,
            sale.unit == null ? null : sale.unit.id, sale.unitName, returns(sale.id));
    }

    private List<ReturnView> returns(long saleId){return em.createQuery("select r from SaleReturn r where r.sale.id=:id order by r.id",SaleReturn.class).setParameter("id",saleId).getResultList().stream().map(this::returnView).toList();}
    private ReturnView returnView(SaleReturn r){var lines=em.createQuery("select i from SaleReturnItem i where i.saleReturn.id=:id order by i.id",SaleReturnItem.class).setParameter("id",r.id).getResultList().stream().map(i->new ReturnLineView(i.id,i.saleItem.id,i.saleItem.stockCode,i.saleItem.modelName,decimal(i.quantity),decimal(i.refundAmount),i.movement.id)).toList();return new ReturnView(r.id,r.reason.id,r.reason.code,r.reason.name,r.cancellation,r.notes,r.returnedAt.toString(),decimal(r.refundAmount),r.refundReference,r.operatorLogin,lines);}
    private BigDecimal returned(long saleItemId){return em.createQuery("select coalesce(sum(i.quantity),0) from SaleReturnItem i where i.saleItem.id=:id",BigDecimal.class).setParameter("id",saleItemId).getSingleResult();}
    private boolean canRestore(AssetItem a){if(a.validUntil!=null&&a.validUntil.isBefore(LocalDate.now()))return false;long recalls=em.createQuery("select count(i) from RecallItem i where i.asset.id=:a and i.recall.status in ('OPEN','IN_PROGRESS')",Long.class).setParameter("a",a.id).getSingleResult();long expired=em.createQuery("select count(e) from ExpirationRecord e where e.asset.id=:a and e.status='EXPIRED'",Long.class).setParameter("a",a.id).getSingleResult();long missing=em.createQuery("select count(i) from InventoryCountItem i where i.asset.id=:a and i.inventoryCount.status.code='APPROVED' and i.result.code='SHORTAGE'",Long.class).setParameter("a",a.id).getSingleResult();return recalls+expired+missing==0;}
    private static String returnFingerprint(long saleId,ReturnRequest r){var lines=r.items().stream().sorted(Comparator.comparing(ReturnLineRequest::saleItemId)).toList();return digest(saleId+"|"+r.reasonId()+"|"+r.notes().trim()+"|"+r.refundReference()+"|"+r.cancellation()+"|"+lines);}
    private static void canonicalUuid(String value){try{if(value==null||!UUID.fromString(value).toString().equals(value))throw new Exception();}catch(Exception e){bad("A canonical UUID request ID is required.");}}
    private static String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}

    private void validateRequest(FinalizeRequest request) {
        if (request == null || request.organizationId() == null || request.buyerId() == null || request.paymentMethod() == null)
            bad("Organization, buyer and payment method are required.");
        try {
            if (request.requestId() == null || !UUID.fromString(request.requestId()).toString().equals(request.requestId()))
                bad("A canonical UUID request ID is required.");
        } catch (IllegalArgumentException ex) { bad("A canonical UUID request ID is required."); }
        if (request.items() == null || request.items().isEmpty() || request.items().size() > 100) bad("A sale requires 1 to 100 items.");
        Set<String> keys = new HashSet<>();
        for (var item : request.items()) {
            if (item == null || (item.assetId() == null) == (item.balanceId() == null)) bad("Select exactly one asset or stock balance per item.");
            amount(item.quantity(), true);
            amount(item.expectedUnitPrice(), false);
            String key = item.assetId() != null ? "asset:" + item.assetId() : "balance:" + item.balanceId();
            if (!keys.add(key)) bad("Duplicate stock selection. Adjust the quantity of the existing item.");
        }
    }

    private static void amount(BigDecimal value, boolean positive) {
        if (value == null || value.signum() < 0 || positive && value.signum() == 0
                || value.stripTrailingZeros().scale() > 4 || value.precision() - value.scale() > 15)
            bad("Amounts must be nonnegative with at most 15 integer and 4 decimal digits; quantities must be positive.");
    }
    private static String fingerprint(FinalizeRequest request) {
        var value = new StringBuilder().append(request.organizationId()).append('|').append(request.buyerId()).append('|').append(request.paymentMethod());
        for (var item : request.items()) value.append('|').append(item.assetId()).append(':').append(item.balanceId())
            .append(':').append(item.quantity().stripTrailingZeros().toPlainString()).append(':').append(item.expectedUnitPrice().stripTrailingZeros().toPlainString());
        // Preserve fingerprints of earlier organization-wide requests.
        if (request.unitId() != null) value.append("|unit:").append(request.unitId());
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private OrganizationalUnit selectedUnit(long organizationId, Long unitId) {
        if (unitId == null) return null;
        var unit = em.find(OrganizationalUnit.class, unitId);
        if (unit == null || !unit.organization.id.equals(organizationId)) bad("Select a unit in the selected organization.");
        return unit;
    }
    private void validateLocation(StockLocation location, long organizationId, Long unitId) {
        access.requireScope("sales", "CREATE", location.organization.id, location.unit == null ? null : location.unit.id);
        if (!location.organization.id.equals(organizationId)) bad("All stock must belong to the selected organization.");
        if (unitId != null && (location.unit == null || !unitId.equals(location.unit.id)))
            bad("All stock must belong to the selected unit.");
    }
    private <T> T locked(Class<T> type, Long id) {
        if (id == null || id <= 0) bad("A valid record ID is required.");
        T entity = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) bad(type.getSimpleName() + " not found.");
        return entity;
    }
    private static void notExpired(LocalDate date) {
        if (date != null && date.isBefore(LocalDate.now())) bad("Expired stock cannot be sold.");
    }
    private static String decimal(BigDecimal value) { return value.setScale(4, RoundingMode.UNNECESSARY).toPlainString(); }
    private static void pagination(int page) { if (page < 0 || page > 100000) bad("Invalid page."); }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
