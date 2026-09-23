package com.comandos.donation.service;

import com.comandos.audit.service.AuditService;
import com.comandos.core.model.*;
import com.comandos.donation.dto.DonationContract.*;
import com.comandos.donation.model.*;
import com.comandos.inventory.model.*;
import com.comandos.security.service.AccessPolicy;
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

@Service @Transactional(readOnly=true)
public class DonationService {
 private static final int PAGE_SIZE=20; private final EntityManager em; private final AccessPolicy access; private final AuditService audit;
 public DonationService(EntityManager em,AccessPolicy access,AuditService audit){this.em=em;this.access=access;this.audit=audit;}
 public Page<StockOption> stock(long organizationId,Long unitId,String kind,String search,int page){
  access.requireScope("donations","READ",organizationId,unitId);selectedUnit(organizationId,unitId);pagination(page);
  boolean assets="ASSET".equals(kind);if(!assets&&!"LOT".equals(kind))bad("Stock kind must be ASSET or LOT.");
  String model=assets?"e.model":"e.lot.model",code=assets?"e.assetCode":"e.lot.lotNumber",expiry=assets?"e.validUntil":"e.lot.validUntil";
  String from=" from "+(assets?"AssetItem":"StockBalance")+" e where e.location.organization.id=:organization"+(unitId==null?"":" and e.location.unit.id=:unit")+(assets?" and e.status='AVAILABLE'":" and e.available>0")+" and ("+expiry+" is null or "+expiry+">=:today) and (lower("+code+") like :search escape '!' or lower("+model+".name) like :search escape '!' or lower("+model+".sku) like :search escape '!')";
  var query=em.createQuery("select e"+from+" order by e.id",CoreEntity.class);var count=em.createQuery("select count(e)"+from,Long.class);String term=escaped(search);
  for(var q:List.of(query,count)){q.setParameter("organization",organizationId).setParameter("today",LocalDate.now()).setParameter("search",term);if(unitId!=null)q.setParameter("unit",unitId);}
  return new Page<>(query.setFirstResult(page*PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(e->{if(e instanceof AssetItem a)return new StockOption("ASSET",a.id,a.assetCode,a.model.name,a.model.sku,a.location.name,a.model.unitOfMeasure,"1");var b=(StockBalance)e;return new StockOption("LOT",b.id,b.lot.lotNumber,b.lot.model.name,b.lot.model.sku,b.location.name,b.lot.model.unitOfMeasure,decimal(b.available));}).toList(),count.getSingleResult(),page,PAGE_SIZE);
 }
 @Transactional public DonationView finalize(FinalizeRequest request){
  validate(request);access.requireScope("donations","CREATE",request.organizationId(),request.unitId());lockCatalog();var organization=locked(Organization.class,request.organizationId());var unit=selectedUnit(organization.id,request.unitId());String fingerprint=fingerprint(request);
  var existing=em.createQuery("select d from Donation d where d.requestId=:id",Donation.class).setParameter("id",request.requestId()).getResultStream().findFirst();
  if(existing.isPresent()){access.requireEntity("donations","READ",existing.get());if(!existing.get().requestFingerprint.equals(fingerprint))conflict("This request ID was already used for a different donation.");return view(existing.get());}
  var donor=locked(Person.class,request.donorId());var donee=locked(Person.class,request.doneeId());access.requireEntity("core/people","READ",donor);access.requireEntity("core/people","READ",donee);
  if(!organization.active||!donor.active||!donee.active)bad("Organization, donor and donee must be active.");if(donor.id.equals(donee.id))bad("Donor and donee must be different people.");
  var donation=new Donation();donation.organization=organization;donation.unit=unit;donation.donor=donor;donation.donee=donee;donation.organizationName=organization.name;donation.unitName=unit==null?null:unit.name;donation.donorName=donor.fullName;donation.doneeName=donee.fullName;donation.term=request.term().trim();donation.direction=request.direction()==null||request.direction().isBlank()?"OUTGOING":request.direction().trim().toUpperCase(Locale.ROOT);donation.documentReference=request.documentReference()==null||request.documentReference().isBlank()?null:request.documentReference().trim();var donationApprover=audit.actor();donation.approvedById=donationApprover.id();donation.approvedByLogin=donationApprover.login();donation.approvedAt=LocalDateTime.now();donation.finalizedAt=LocalDateTime.now();var actor=audit.actor();donation.finalizedById=actor.id();donation.finalizedByLogin=actor.login();donation.requestId=request.requestId();donation.requestFingerprint=fingerprint;em.persist(donation);
  List<Map<String,Object>> changes=new ArrayList<>();
  for(var line:request.items().stream().sorted(Comparator.comparing(i->i.assetId()!=null?"A"+i.assetId():"B"+i.balanceId())).toList()){
   var item=new DonationItem();item.donation=donation;item.quantity=line.quantity();
   if(line.assetId()!=null){var asset=locked(AssetItem.class,line.assetId());scope(asset.location,organization.id,request.unitId());if(!AssetStatus.AVAILABLE.name().equals(asset.status))conflict("Asset "+asset.assetCode+" is no longer available.");notExpired(asset.validUntil);if(item.quantity.compareTo(BigDecimal.ONE)!=0)bad("Individual assets require quantity 1.");item.asset=asset;item.model=asset.model;item.location=asset.location;item.stockCode=asset.assetCode;changes.add(Map.of("resource","inventory/assets","recordId",asset.id,"before",Map.of("status",asset.status),"after",Map.of("status",AssetStatus.DONATED.name())));asset.status=AssetStatus.DONATED.name();
   }else{var balance=locked(StockBalance.class,line.balanceId());scope(balance.location,organization.id,request.unitId());var lot=locked(StockLot.class,balance.lot.id);notExpired(lot.validUntil);if(balance.available.compareTo(item.quantity)<0||lot.availableQuantity.compareTo(item.quantity)<0)conflict("Insufficient available stock for lot "+lot.lotNumber+".");item.lot=lot;item.model=lot.model;item.location=balance.location;item.stockCode=lot.lotNumber;changes.add(Map.of("resource","inventory/balances","recordId",balance.id,"before",balance.available.toPlainString(),"after",balance.available.subtract(item.quantity).toPlainString()));balance.available=balance.available.subtract(item.quantity);lot.availableQuantity=lot.availableQuantity.subtract(item.quantity);}
   item.modelName=item.model.name;item.sku=item.model.sku;item.locationName=item.location.name;item.unitOfMeasure=item.model.unitOfMeasure;var movement=new StockMovement();movement.asset=item.asset;movement.lot=item.lot;movement.location=item.location;movement.nature=StockMovementNature.DONATION.name();movement.quantity=item.quantity.negate();movement.movedAt=donation.finalizedAt;movement.operatorLogin = audit.actor().login(); movement.operatorId = audit.actor().id(); em.persist(movement);item.movement=movement;em.persist(item);
  }
  em.flush();var result=view(donation);audit.record("donations",donation.id,"FINALIZE",null,Map.of("donation",result,"stockChanges",changes));return result;
 }
 public Page<DonationView> list(long organizationId,Long unitId,int page){access.requireScope("donations","READ",organizationId,unitId);selectedUnit(organizationId,unitId);pagination(page);String from=" from Donation d where d.organization.id=:organization"+(unitId==null?"":" and d.unit.id=:unit");var query=em.createQuery("select d"+from+" order by d.id desc",Donation.class);var count=em.createQuery("select count(d)"+from,Long.class);for(var q:List.of(query,count)){q.setParameter("organization",organizationId);if(unitId!=null)q.setParameter("unit",unitId);}return new Page<>(query.setFirstResult(page*PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(this::view).toList(),count.getSingleResult(),page,PAGE_SIZE);}
 public DonationView get(long id){access.requireAny("donations","READ");var d=em.find(Donation.class,id);if(d==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Donation not found.");access.requireEntity("donations","READ",d);return view(d);}
 private DonationView view(Donation d){var items=em.createQuery("select i from DonationItem i where i.donation.id=:id order by i.id",DonationItem.class).setParameter("id",d.id).getResultList().stream().map(i->new LineView(i.id,i.model.id,i.asset==null?null:i.asset.id,i.lot==null?null:i.lot.id,i.location.id,i.movement.id,i.modelName,i.sku,i.stockCode,i.locationName,i.unitOfMeasure,decimal(i.quantity))).toList();return new DonationView(d.id,d.organization.id,d.organizationName,d.unit==null?null:d.unit.id,d.unitName,d.donor.id,d.donorName,d.donee.id,d.doneeName,d.term,d.status,d.finalizedAt.toString(),d.finalizedById,d.finalizedByLogin,items);}
 private void validate(FinalizeRequest r){if(r==null||r.organizationId()==null||r.donorId()==null||r.doneeId()==null||r.term()==null||r.term().isBlank())bad("Organization, donor, donee and term are required.");uuid(r.requestId());if(r.term().trim().length()>255)bad("Term must contain at most 255 characters.");if(r.items()==null||r.items().isEmpty()||r.items().size()>100)bad("A donation requires 1 to 100 items.");Set<String> keys=new HashSet<>();for(var i:r.items()){if(i==null||(i.assetId()==null)==(i.balanceId()==null))bad("Select exactly one asset or stock balance per item.");amount(i.quantity());String key=i.assetId()!=null?"A"+i.assetId():"B"+i.balanceId();if(!keys.add(key))bad("Duplicate stock selection.");}}
 private static String fingerprint(FinalizeRequest r){String lines=r.items().stream().sorted(Comparator.comparing(i->i.assetId()!=null?"A"+i.assetId():"B"+i.balanceId())).map(i->i.assetId()+":"+i.balanceId()+":"+i.quantity().stripTrailingZeros().toPlainString()).toList().toString();return hash(r.organizationId()+"|"+r.unitId()+"|"+r.donorId()+"|"+r.doneeId()+"|"+r.term().trim()+"|"+lines);}
 private void scope(StockLocation l,long org,Long unit){access.requireScope("donations","CREATE",l.organization.id,l.unit==null?null:l.unit.id);if(!l.organization.id.equals(org)||unit!=null&&(l.unit==null||!unit.equals(l.unit.id)))bad("Every item must belong to the selected organization and unit.");}
 private OrganizationalUnit selectedUnit(long org,Long id){if(id==null)return null;var u=em.find(OrganizationalUnit.class,id);if(u==null||!u.organization.id.equals(org))bad("Select a unit in the selected organization.");return u;}
 private void lockCatalog(){em.createQuery("select c from ItemCategory c order by c.id",ItemCategory.class).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();}
 private <T>T locked(Class<T> type,Long id){if(id==null||id<=0)bad("A valid record ID is required.");var value=em.find(type,id,LockModeType.PESSIMISTIC_WRITE);if(value==null)bad(type.getSimpleName()+" not found.");return value;}
 private static void amount(BigDecimal v){if(v==null||v.signum()<=0||v.stripTrailingZeros().scale()>4||v.precision()-v.scale()>15)bad("Quantities must be positive with at most 15 integer and 4 decimal digits.");}
 private static void notExpired(LocalDate d){if(d!=null&&d.isBefore(LocalDate.now()))bad("Expired stock cannot be donated.");}
 private static String escaped(String v){return "%"+(v==null?"":v.trim().toLowerCase(Locale.ROOT)).replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
 private static String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private static void uuid(String v){try{if(v==null||!UUID.fromString(v).toString().equals(v))throw new IllegalArgumentException();}catch(IllegalArgumentException e){bad("A canonical UUID request ID is required.");}}
 private static String decimal(BigDecimal v){return v.setScale(4,RoundingMode.UNNECESSARY).toPlainString();}private static void pagination(int p){if(p<0||p>100000)bad("Invalid page.");}private static void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private static void conflict(String m){throw new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
