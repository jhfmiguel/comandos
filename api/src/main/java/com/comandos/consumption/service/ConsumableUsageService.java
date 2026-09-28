package com.comandos.consumption.service;

import com.comandos.audit.service.AuditService;
import com.comandos.consumption.dto.ConsumableUsageContract.*;
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
public class ConsumableUsageService {
    private static final int PAGE_SIZE = 20;
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;
    public ConsumableUsageService(EntityManager em, AccessPolicy access, AuditService audit) {
        this.em = em; this.access = access; this.audit = audit;
    }

    public Page<StockOption> stock(long organizationId, Long unitId, String search, int page) {
        access.requireScope("ammunition-consumptions", "READ", organizationId, unitId);
        selectedUnit(organizationId, unitId); pagination(page);
        String from = " from StockBalance b where b.location.organization.id=:organization"
            + (unitId == null ? "" : " and b.location.unit.id=:unit")
            + " and b.lot.model.category.lotControlled=true"
            + " and b.lot.model.category.serialized=false"
            + " and b.lot.model.category.consumable=true"
            + " and b.available>0 and b.lot.availableQuantity>0"
            + " and (b.lot.validUntil is null or b.lot.validUntil>=:today)"
            + " and (lower(b.lot.model.sku) like :search escape '!' or lower(b.lot.model.name) like :search escape '!'"
            + " or lower(b.lot.lotNumber) like :search escape '!' or lower(b.lot.model.category.family) like :search escape '!')";
        var query = em.createQuery("select b" + from + " order by b.lot.validUntil, b.id", StockBalance.class);
        var count = em.createQuery("select count(b)" + from, Long.class);
        for (var q : List.of(query, count)) {
            q.setParameter("organization", organizationId).setParameter("today", LocalDate.now()).setParameter("search", escaped(search));
            if (unitId != null) q.setParameter("unit", unitId);
        }
        return new Page<>(query.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(this::stockView).toList(), count.getSingleResult(), page, PAGE_SIZE);
    }

    @Transactional
    public UsageView finalizeUsage(FinalizeRequest request) {
        validate(request);
        access.requireScope("ammunition-consumptions", "CREATE", request.organizationId(), request.unitId());
        var organization = locked(Organization.class, request.organizationId());
        var unit = selectedUnit(organization.id, request.unitId());
        String fingerprint = fingerprint(request);
        var existing = em.createQuery("select u from ConsumableUsage u where u.requestId=:id", ConsumableUsage.class)
            .setParameter("id", request.requestId()).getResultStream().findFirst();
        if (existing.isPresent()) {
            if (!existing.get().requestFingerprint.equals(fingerprint)) conflict("Request ID already used for another consumable usage.");
            return view(existing.get());
        }
        var responsible = locked(Person.class, request.responsibleId());
        var authorizer = locked(Person.class, request.authorizerId());
        if (!organization.active || !responsible.active || !authorizer.active) bad("Organization, responsible person and authorizer must be active.");
        var now = LocalDateTime.now();
        var usage = new ConsumableUsage();
        usage.organization=organization; usage.unit=unit; usage.responsible=responsible; usage.authorizer=authorizer;
        usage.organizationName=organization.name; usage.unitName=unit==null?null:unit.name;
        usage.responsibleName=responsible.fullName; usage.authorizerName=authorizer.fullName;
        usage.purpose=request.purpose().trim(); usage.activityType=normalized(request.activityType(), "OPERATION");
        usage.operationTraining=blankToNull(request.operationTraining()); usage.deliveredAt=now; usage.closedAt=now;
        var actor=audit.actor(); usage.finalizedById=actor.id(); usage.finalizedByLogin=actor.login();
        usage.requestId=request.requestId(); usage.requestFingerprint=fingerprint; em.persist(usage); em.flush();

        List<Map<String,Object>> changes = new ArrayList<>();
        for (var line : request.items().stream().sorted(Comparator.comparing(LineRequest::balanceId)).toList()) {
            var balance=locked(StockBalance.class,line.balanceId()); var lot=locked(StockLot.class,balance.lot.id);
            validateStock(balance,lot,organization.id,request.unitId(),line.deliveredQuantity());
            BigDecimal delivered=line.deliveredQuantity(), used=line.usedQuantity(), returned=line.returnedQuantity();
            BigDecimal beforeBalance=balance.available, beforeLot=lot.availableQuantity;
            balance.available=beforeBalance.subtract(delivered); lot.availableQuantity=beforeLot.subtract(delivered);

            var delivery=movement(lot,balance.location,StockMovementNature.CONSUMABLE_DELIVERY,delivered.negate(),usage.id,now);
            StockMovement returnedMovement=null;
            if (returned.signum()>0) {
                balance.available=balance.available.add(returned); lot.availableQuantity=lot.availableQuantity.add(returned);
                returnedMovement=movement(lot,balance.location,StockMovementNature.CONSUMABLE_RETURN,returned,usage.id,now);
            }
            var item=new ConsumableUsageItem(); item.usage=usage; item.lot=lot; item.balance=balance; item.location=balance.location;
            item.deliveryMovement=delivery; item.returnMovement=returnedMovement; item.family=lot.model.category.family;
            item.modelName=lot.model.name; item.sku=lot.model.sku; item.lotNumber=lot.lotNumber; item.locationName=balance.location.name;
            item.unitOfMeasure=lot.model.unitOfMeasure; item.deliveredQuantity=delivered; item.usedQuantity=used; item.returnedQuantity=returned;
            item.usedAt=now; item.returnedAt=returned.signum()>0?now:null; item.result=line.result().trim(); em.persist(item);
            changes.add(Map.of("balanceId",balance.id,"family",item.family,"delivered",delivered,"used",used,"returned",returned,
                "before",beforeBalance,"after",balance.available));
        }
        em.flush(); var result=view(usage);
        audit.record("consumable-usages",usage.id,"CLOSE",null,Map.of("usage",result,"stockChanges",changes));
        return result;
    }

    public UsageView get(long id) { var u=em.find(ConsumableUsage.class,id); if(u==null)notFound(); return view(u); }
    public Page<UsageView> list(long organizationId,Long unitId,int page) {
        access.requireScope("ammunition-consumptions","READ",organizationId,unitId); selectedUnit(organizationId,unitId); pagination(page);
        String from=" from ConsumableUsage u where u.organization.id=:organization"+(unitId==null?"":" and u.unit.id=:unit");
        var q=em.createQuery("select u"+from+" order by u.id desc",ConsumableUsage.class); var c=em.createQuery("select count(u)"+from,Long.class);
        for(var x:List.of(q,c)){x.setParameter("organization",organizationId);if(unitId!=null)x.setParameter("unit",unitId);}
        return new Page<>(q.setFirstResult(page*PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(this::view).toList(),c.getSingleResult(),page,PAGE_SIZE);
    }

    private StockMovement movement(StockLot lot, StockLocation location, StockMovementNature nature, BigDecimal qty, Long ref, LocalDateTime at) {
        var m=new StockMovement();m.lot=lot;m.location=location;m.nature=nature.name();m.referenceType=StockMovementReferenceType.CONSUMABLE_USAGE.name();
        m.referenceId=ref;m.quantity=qty;m.movedAt=at;var actor=audit.actor();m.operatorId=actor.id();m.operatorLogin=actor.login();em.persist(m);return m;
    }
    private UsageView view(ConsumableUsage u){var items=em.createQuery("select i from ConsumableUsageItem i where i.usage.id=:id order by i.id",ConsumableUsageItem.class).setParameter("id",u.id).getResultList().stream().map(i->new ItemView(i.id,i.lot.id,i.balance.id,i.family,i.sku,i.modelName,i.lotNumber,i.locationName,i.unitOfMeasure,i.deliveredQuantity,i.usedQuantity,i.returnedQuantity,i.deliveryMovement.id,i.returnMovement==null?null:i.returnMovement.id,i.result)).toList();return new UsageView(u.id,u.organization.id,u.organizationName,u.unit==null?null:u.unit.id,u.unitName,u.responsible.id,u.responsibleName,u.authorizer.id,u.authorizerName,u.purpose,u.activityType,u.operationTraining,u.status,u.deliveredAt.toString(),u.closedAt.toString(),u.finalizedById,u.finalizedByLogin,items);}
    private StockOption stockView(StockBalance b){return new StockOption(b.id,b.lot.id,b.lot.model.category.family,b.lot.model.sku,b.lot.model.name,b.lot.lotNumber,b.location.name,b.lot.model.unitOfMeasure,b.available,b.lot.validUntil==null?null:b.lot.validUntil.toString());}
    private void validateStock(StockBalance b,StockLot lot,long org,Long unit,BigDecimal delivered){if(!b.location.organization.id.equals(org)||unit!=null&&(b.location.unit==null||!unit.equals(b.location.unit.id)))bad("Every consumable lot must belong to the selected organization and unit.");var c=lot.model.category;if(!Boolean.TRUE.equals(c.lotControlled)||Boolean.TRUE.equals(c.serialized)||!Boolean.TRUE.equals(c.consumable))bad("Only lot-controlled, non-serialized consumable items use this lifecycle.");if(lot.validUntil!=null&&lot.validUntil.isBefore(LocalDate.now()))bad("Expired consumable stock cannot be delivered.");if(b.available.compareTo(delivered)<0||lot.availableQuantity.compareTo(delivered)<0)conflict("Insufficient available consumable stock for delivery.");}
    private void validate(FinalizeRequest r){if(r==null||r.organizationId()==null||r.responsibleId()==null||r.authorizerId()==null||r.purpose()==null||r.purpose().isBlank())bad("Organization, responsible person, authorizer and purpose are required.");uuid(r.requestId());if(r.items()==null||r.items().isEmpty()||r.items().size()>100)bad("Select 1 to 100 consumable lots.");Set<Long> ids=new HashSet<>();for(var l:r.items()){if(l==null||l.balanceId()==null||l.deliveredQuantity()==null||l.usedQuantity()==null||l.returnedQuantity()==null||l.deliveredQuantity().signum()<=0||l.usedQuantity().signum()<0||l.returnedQuantity().signum()<0||l.usedQuantity().add(l.returnedQuantity()).compareTo(l.deliveredQuantity())!=0||l.result()==null||l.result().isBlank())bad("Each line must satisfy delivered = used + returned and include a result.");if(!ids.add(l.balanceId()))bad("Each stock balance can appear only once.");}}
    private OrganizationalUnit selectedUnit(long org,Long id){if(id==null)return null;var u=em.find(OrganizationalUnit.class,id);if(u==null||!u.organization.id.equals(org))bad("Select a unit in the selected organization.");return u;}
    private <T>T locked(Class<T> type,Long id){if(id==null||id<=0)bad("A valid record ID is required.");var v=em.find(type,id,LockModeType.PESSIMISTIC_WRITE);if(v==null)bad(type.getSimpleName()+" not found.");return v;}
    private static String fingerprint(FinalizeRequest r){String lines=r.items().stream().sorted(Comparator.comparing(LineRequest::balanceId)).map(i->i.balanceId()+":"+i.deliveredQuantity()+":"+i.usedQuantity()+":"+i.returnedQuantity()+":"+i.result().trim()).toList().toString();return hash(r.organizationId()+"|"+r.unitId()+"|"+r.responsibleId()+"|"+r.authorizerId()+"|"+r.purpose().trim()+"|"+lines);}
    private static String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static void uuid(String v){try{if(v==null||!UUID.fromString(v).toString().equals(v))throw new IllegalArgumentException();}catch(IllegalArgumentException e){bad("A canonical UUID request ID is required.");}}
    private static String escaped(String v){return "%"+(v==null?"":v.trim().toLowerCase(Locale.ROOT)).replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
    private static String normalized(String v,String d){return v==null||v.isBlank()?d:v.trim().toUpperCase(Locale.ROOT);}private static String blankToNull(String v){return v==null||v.isBlank()?null:v.trim();}
    private static void pagination(int p){if(p<0||p>100000)bad("Invalid page.");}private static void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private static void conflict(String m){throw new ResponseStatusException(HttpStatus.CONFLICT,m);}private static void notFound(){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Consumable usage not found.");}
}
