package com.weaponsregistration.purchase.service;
import com.weaponsregistration.inventory.model.ItemModel;
import com.weaponsregistration.inventory.service.InventoryService;
import com.weaponsregistration.purchase.dto.ReceivingIncorporationContract;
import com.weaponsregistration.purchase.model.*;
import com.weaponsregistration.purchase.repository.*;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class ReceivingIncorporationService {
 private final ReceivingIncorporationRepository repo;private final EquipmentReceivingRepository receivings;private final ReceivingSerialRepository serials;private final InventoryService inventory;private final EntityManager em;
 public ReceivingIncorporationService(ReceivingIncorporationRepository a,EquipmentReceivingRepository b,EquipmentReceivingItemRepository c,ReceivingSerialRepository d,InventoryService i,EntityManager e){repo=a;receivings=b;serials=d;inventory=i;em=e;}
 public List<ReceivingIncorporation> list(Long id){return id==null?repo.findAll():repo.findByReceivingIdOrderByCreatedAtAsc(id);}
 @Transactional public ReceivingIncorporation create(ReceivingIncorporationContract.CreateRequest r){
  if(r==null||r.receivingId()==null||r.receivingItemId()==null)throw new IllegalArgumentException("Receiving and receiving item are required.");if(r.stockLocationId()==null)throw new IllegalArgumentException("Stock location is required.");
  EquipmentReceiving rec=receivings.findById(r.receivingId()).orElseThrow(()->new IllegalArgumentException("Receiving not found."));if(rec.status!=ReceivingStatus.DEFINITIVELY_ACCEPTED)throw new IllegalStateException("Only definitively accepted receiving records can be incorporated.");
  EquipmentReceivingItem item=em.find(EquipmentReceivingItem.class,r.receivingItemId(),LockModeType.PESSIMISTIC_WRITE);if(item==null||item.receiving==null||!rec.id.equals(item.receiving.id))throw new IllegalArgumentException("Receiving item is invalid.");ItemModel model=em.find(ItemModel.class,item.itemModelId);if(model==null)throw new IllegalStateException("Item model no longer exists.");
  BigDecimal accepted=nz(item.acceptedQuantity),already=repo.findByReceivingItemIdOrderByCreatedAtAsc(item.id).stream().map(x->nz(x.quantity)).reduce(BigDecimal.ZERO,BigDecimal::add),value=r.incorporationValue()==null?(item.acquisitionItem==null?BigDecimal.ZERO:nz(item.acquisitionItem.unitPrice)):r.incorporationValue();if(value.signum()<0)throw new IllegalArgumentException("Incorporation value cannot be negative.");String condition=clean(r.initialCondition());if(condition==null)condition=clean(item.conditionDescription);if(condition==null)condition="GOOD";
  ReceivingSerial serial=null;BigDecimal qty;String resource;Long recordId;String assetCode=clean(r.assetCode()),lot=clean(r.lotNumber());
  if(Boolean.TRUE.equals(model.category.serialized)){if(r.receivingSerialId()==null)throw new IllegalArgumentException("Serial is required.");serial=serials.findById(r.receivingSerialId()).orElseThrow(()->new IllegalArgumentException("Receiving serial not found."));if(serial.receivingItem==null||!item.id.equals(serial.receivingItem.id))throw new IllegalArgumentException("Serial does not belong to item.");if(!Boolean.TRUE.equals(serial.accepted))throw new IllegalStateException("Rejected serial cannot be incorporated.");if(repo.existsByReceivingSerialId(serial.id))throw new IllegalStateException("Serial already incorporated.");qty=BigDecimal.ONE;if(already.add(qty).compareTo(accepted)>0)throw new IllegalStateException("Incorporation exceeds accepted quantity.");if(assetCode==null)assetCode="PAT-"+rec.id+"-"+item.id+"-"+serial.id;var d=new HashMap<String,Object>();d.put("modelId",model.id);d.put("locationId",r.stockLocationId());d.put("assetCode",assetCode);d.put("serialNumber",serial.serialNumber);d.put("condition",condition);d.put("status","AVAILABLE");d.put("validUntil",item.expirationDate);d.put("currentValue",value);recordId=((Number)inventory.save("assets",null,d).get("id")).longValue();resource="assets";serial.assetCode=assetCode;serials.save(serial);
  }else{qty=r.quantity()==null?accepted.subtract(already):r.quantity();if(qty.signum()<=0)throw new IllegalArgumentException("Quantity must be greater than zero.");if(already.add(qty).compareTo(accepted)>0)throw new IllegalStateException("Incorporation exceeds accepted quantity.");if(lot==null)lot=clean(item.lotNumber);if(lot==null)lot="REC-"+rec.id+"-"+item.id;var d=new HashMap<String,Object>();d.put("modelId",model.id);d.put("openingLocationId",r.stockLocationId());d.put("lotNumber",lot);d.put("initialQuantity",qty);d.put("validUntil",item.expirationDate);recordId=((Number)inventory.save("lots",null,d).get("id")).longValue();resource="lots";}
  ReceivingIncorporation x=new ReceivingIncorporation();x.receiving=rec;x.receivingItem=item;x.receivingSerial=serial;x.stockLocationId=r.stockLocationId();x.assetCode=assetCode;x.lotNumber=lot;x.quantity=qty;x.incorporationValue=value;x.initialCondition=condition;x.inventoryResource=resource;x.inventoryRecordId=recordId;x.incorporatedBy=clean(r.incorporatedBy());x.incorporatedAt=r.incorporatedAt()==null?LocalDateTime.now():r.incorporatedAt();x.notes=clean(r.notes());return repo.save(x);
 }
 private static BigDecimal nz(BigDecimal v){return v==null?BigDecimal.ZERO:v;}private static String clean(String v){if(v==null)return null;v=v.trim();return v.isEmpty()?null:v;}
}
