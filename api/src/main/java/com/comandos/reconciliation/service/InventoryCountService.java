package com.comandos.reconciliation.service;

import com.comandos.audit.service.AuditService;
import com.comandos.core.model.*;
import com.comandos.inventory.model.*;
import com.comandos.reconciliation.dto.InventoryCountContract.*;
import com.comandos.reconciliation.model.*;
import com.comandos.security.service.AccessPolicy;
import jakarta.persistence.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional(readOnly=true)
public class InventoryCountService {
    private final EntityManager em; private final AccessPolicy access; private final AuditService audit;
    public InventoryCountService(EntityManager em,AccessPolicy access,AuditService audit){this.em=em;this.access=access;this.audit=audit;}

    @Transactional public CountView open(OpenRequest r){
        if(r==null||r.organizationId()==null||r.locationId()==null||blank(r.purpose()))bad("Organization, location and purpose are required.");
        uuid(r.requestId()); access.requireScope("inventory-counts","CREATE",r.organizationId(),r.unitId());
        var existing=em.createQuery("select c from InventoryCount c where c.requestId=:id",InventoryCount.class).setParameter("id",r.requestId()).getResultStream().findFirst();
        String fp=hash(r.organizationId()+"|"+r.unitId()+"|"+r.locationId()+"|"+r.purpose().trim());
        if(existing.isPresent()){access.requireEntity("inventory-counts","READ",existing.get());if(!existing.get().requestFingerprint.equals(fp))conflict("Request ID already used.");return view(existing.get());}
        var organization=locked(Organization.class,r.organizationId());if(!organization.active)bad("Organization must be active.");
        var unit=unit(organization.id,r.unitId());var location=locked(StockLocation.class,r.locationId());
        if(!location.organization.id.equals(organization.id)||unit!=null&&(location.unit==null||!location.unit.id.equals(unit.id)))bad("Location does not belong to the selected scope.");
        access.requireScope("inventory-counts","CREATE",location.organization.id,location.unit==null?null:location.unit.id);
        long active=em.createQuery("select count(c) from InventoryCount c where c.location.id=:l and c.status.code in ('OPEN','COUNTED')",Long.class).setParameter("l",location.id).getSingleResult();
        if(active>0)conflict("This location already has an unfinished inventory count.");
        var c=new InventoryCount();c.organization=organization;c.unit=unit;c.location=location;c.status=status("OPEN");c.organizationName=organization.name;c.unitName=unit==null?null:unit.name;c.locationName=location.name;c.purpose=trim(r.purpose(),255);c.openedAt=LocalDateTime.now();var actor=audit.actor();c.openedById=actor.id();c.openedByLogin=actor.login();c.requestId=r.requestId();c.requestFingerprint=fp;em.persist(c);
        var assets=em.createQuery("select a from AssetItem a where a.location.id=:l and a.status in ('AVAILABLE','BLOCKED') order by a.id",AssetItem.class).setParameter("l",location.id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        for(var a:assets){var i=base(c,a.model,a.assetCode);i.asset=a;i.systemQuantity=BigDecimal.ONE;em.persist(i);}
        var balances=em.createQuery("select b from StockBalance b where b.location.id=:l and (b.available+b.reserved+b.blocked)>0 order by b.id",StockBalance.class).setParameter("l",location.id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        for(var b:balances){var i=base(c,b.lot.model,b.lot.lotNumber);i.lot=b.lot;i.balance=b;i.systemQuantity=b.available.add(b.reserved).add(b.blocked);em.persist(i);}
        if(assets.isEmpty()&&balances.isEmpty())bad("The selected location has no countable stock.");
        em.flush();var result=view(c);audit.record("inventory-counts",c.id,"OPEN",null,result);return result;
    }

    @Transactional public CountView count(long id,SubmitRequest r){
        var c=locked(InventoryCount.class,id);access.requireEntity("inventory-counts","COUNT",c);if(!Set.of("OPEN","COUNTED").contains(c.status.code))conflict("Only an open count can be entered.");
        var previousView=view(c);var stored=items(id);if(r==null||r.items()==null||r.items().size()!=stored.size())bad("A counted quantity is required for every inventory item.");
        var supplied=new HashMap<Long,CountLine>();for(var line:r.items()){if(line==null||line.itemId()==null||supplied.put(line.itemId(),line)!=null)bad("Inventory items must be unique.");quantity(line.countedQuantity());}
        var match=result("MATCH");var shortage=result("SHORTAGE");var surplus=result("SURPLUS");
        for(var item:stored){var line=supplied.get(item.id);if(line==null)bad("A counted quantity is required for every inventory item.");if(item.asset!=null&&line.countedQuantity().signum()!=0&&line.countedQuantity().compareTo(BigDecimal.ONE)!=0)bad("Individual asset count must be zero or one.");item.countedQuantity=line.countedQuantity();item.differenceQuantity=line.countedQuantity().subtract(item.systemQuantity);item.result=item.differenceQuantity.signum()==0?match:item.differenceQuantity.signum()<0?shortage:surplus;item.notes=blank(line.notes())?null:trim(line.notes(),500);}
        c.status=status("COUNTED");c.countedAt=LocalDateTime.now();em.flush();var value=view(c);audit.record("inventory-counts",c.id,"COUNT",previousView,value);return value;
    }

    @Transactional public CountView approve(long id){
        var c=locked(InventoryCount.class,id);access.requireEntity("inventory-counts","APPROVE",c);if("APPROVED".equals(c.status.code))return view(c);if(!"COUNTED".equals(c.status.code))conflict("The count must be completed before approval.");
        var previousView=view(c);List<Map<String,Object>> changes=new ArrayList<>();
        for(var item:items(id)){if(item.countedQuantity==null)conflict("The count is incomplete.");var diff=item.differenceQuantity;if(diff.signum()==0)continue;
            if(item.asset!=null){var a=locked(AssetItem.class,item.asset.id);if(!a.location.id.equals(c.location.id)||!Set.of(AssetStatus.AVAILABLE.name(),AssetStatus.BLOCKED.name()).contains(a.status))conflict("Equipment moved or became unavailable after the inventory snapshot. Cancel this count and open a new one.");if(diff.signum()<0){String before=a.status;a.status=AssetStatus.BLOCKED.name();item.adjustmentMovement=movement(a,null,c.location,diff,c.id);changes.add(Map.of("assetId",a.id,"before",before,"after",a.status,"difference",decimal(diff)));}else conflict("Register unidentified individual assets before approving a surplus.");}
            else{var b=locked(StockBalance.class,item.balance.id);var lot=locked(StockLot.class,item.lot.id);if(b.available.add(b.reserved).add(b.blocked).compareTo(item.systemQuantity)!=0)conflict("Lot quantity changed after the inventory snapshot. Cancel this count and open a new one.");if(diff.signum()<0&&b.available.add(diff).signum()<0)conflict("Release reserved or blocked quantity before approving this shortage.");b.available=b.available.add(diff);lot.availableQuantity=lot.availableQuantity.add(diff);if(b.available.signum()<0||lot.availableQuantity.signum()<0)conflict("Inventory adjustment would create negative stock.");item.adjustmentMovement=movement(null,lot,c.location,diff,c.id);changes.add(Map.of("balanceId",b.id,"difference",decimal(diff)));}}
        c.status=status("APPROVED");c.approvedAt=LocalDateTime.now();var actor=audit.actor();c.approvedById=actor.id();c.approvedByLogin=actor.login();em.flush();var value=view(c);audit.record("inventory-counts",c.id,"APPROVE",previousView,Map.of("inventoryCount",value,"adjustments",changes));return value;
    }

    @Transactional public CountView cancel(long id){var c=locked(InventoryCount.class,id);access.requireEntity("inventory-counts","CANCEL",c);if(Set.of("APPROVED","CANCELLED").contains(c.status.code))return view(c);var previousView=view(c);c.status=status("CANCELLED");em.flush();var value=view(c);audit.record("inventory-counts",c.id,"CANCEL",previousView,value);return value;}

    public Page<CountView> list(long org,Long unit,int page){access.requireScope("inventory-counts","READ",org,unit);unit(org,unit);if(page<0)bad("Invalid page.");String f=" from InventoryCount c where c.organization.id=:o"+(unit==null?"":" and c.unit.id=:u");var q=em.createQuery("select c"+f+" order by c.id desc",InventoryCount.class);var total=em.createQuery("select count(c)"+f,Long.class);q.setParameter("o",org);total.setParameter("o",org);if(unit!=null){q.setParameter("u",unit);total.setParameter("u",unit);}return new Page<>(q.setFirstResult(page*20).setMaxResults(20).getResultList().stream().map(this::view).toList(),total.getSingleResult(),page,20);}

    private InventoryCountItem base(InventoryCount c,ItemModel model,String code){var i=new InventoryCountItem();i.inventoryCount=c;i.model=model;i.modelName=model.name;i.sku=model.sku;i.stockCode=code;i.unitOfMeasure=model.unitOfMeasure;return i;}
    private StockMovement movement(AssetItem a,StockLot l,StockLocation location,BigDecimal q,Long inventoryCountId){var m=new StockMovement();m.asset=a;m.lot=l;m.location=location;m.nature=StockMovementNature.INVENTORY_ADJUSTMENT.name();m.referenceType="INVENTORY_COUNT";m.referenceId=inventoryCountId;m.quantity=q;m.movedAt=LocalDateTime.now();m.operatorLogin=audit.actor().login();m.operatorId=audit.actor().id();em.persist(m);return m;}
    private CountView view(InventoryCount c){return new CountView(c.id,c.organization.id,c.organizationName,c.unit==null?null:c.unit.id,c.unitName,c.location.id,c.locationName,c.purpose,c.status.code,c.status.name,c.openedAt.toString(),c.countedAt==null?null:c.countedAt.toString(),c.approvedAt==null?null:c.approvedAt.toString(),c.openedByLogin,c.approvedByLogin,items(c.id).stream().map(i->new ItemView(i.id,i.asset!=null?"ASSET":"LOT",i.asset==null?null:i.asset.id,i.lot==null?null:i.lot.id,i.modelName,i.sku,i.stockCode,i.unitOfMeasure,decimal(i.systemQuantity),i.countedQuantity==null?null:decimal(i.countedQuantity),i.differenceQuantity==null?null:decimal(i.differenceQuantity),i.result==null?null:i.result.code,i.result==null?null:i.result.name,i.notes)).toList());}
    private List<InventoryCountItem> items(long id){return em.createQuery("select i from InventoryCountItem i where i.inventoryCount.id=:id order by i.id",InventoryCountItem.class).setParameter("id",id).getResultList();}
    private InventoryCountStatusType status(String code){var value=em.createQuery("select s from InventoryCountStatusType s where s.code=:c",InventoryCountStatusType.class).setParameter("c",code).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Required inventory count status is not configured."));if(!value.active)conflict("Required inventory count status "+code+" is inactive.");return value;}
    private InventoryCountResultType result(String code){var value=em.createQuery("select s from InventoryCountResultType s where s.code=:c",InventoryCountResultType.class).setParameter("c",code).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Required inventory result is not configured."));if(!value.active)conflict("Required inventory result "+code+" is inactive.");return value;}
    private OrganizationalUnit unit(long org,Long id){if(id==null)return null;var u=em.find(OrganizationalUnit.class,id);if(u==null||!u.organization.id.equals(org))bad("Select a unit in the organization.");return u;}
    private<T>T locked(Class<T>type,Long id){var value=id==null?null:em.find(type,id,LockModeType.PESSIMISTIC_WRITE);if(value==null)bad(type.getSimpleName()+" not found.");return value;}
    private static void quantity(BigDecimal q){if(q==null||q.signum()<0||q.stripTrailingZeros().scale()>4)bad("Counted quantity must be non-negative with at most four decimals.");}
    private static String trim(String v,int max){v=v.trim();if(v.length()>max)bad("Text is too long.");return v;}private static boolean blank(String v){return v==null||v.isBlank();}
    private static String decimal(BigDecimal v){return v.setScale(4,RoundingMode.UNNECESSARY).toPlainString();}
    private static void uuid(String v){try{if(v==null||!UUID.fromString(v).toString().equals(v))throw new Exception();}catch(Exception e){bad("A canonical UUID request ID is required.");}}
    private static String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private static void conflict(String m){throw new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
