package com.weaponsregistration.purchase.service;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.inventory.model.ItemModel;
import com.weaponsregistration.purchase.dto.EquipmentReceivingContract;
import com.weaponsregistration.purchase.model.*;
import com.weaponsregistration.purchase.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Service
public class EquipmentReceivingService {
 private final EquipmentReceivingRepository receivingRepository; private final EquipmentReceivingItemRepository itemRepository; private final ReceivingSerialRepository serialRepository; private final PurchaseRepository purchaseRepository; private final PurchaseItemRepository purchaseItemRepository; private final EntityManager em;
 public EquipmentReceivingService(EquipmentReceivingRepository r,EquipmentReceivingItemRepository i,ReceivingSerialRepository s,PurchaseRepository p,PurchaseItemRepository pi,EntityManager em){this.receivingRepository=r;this.itemRepository=i;this.serialRepository=s;this.purchaseRepository=p;this.purchaseItemRepository=pi;this.em=em;}
 public List<EquipmentReceiving> list(Long acquisitionId){return acquisitionId==null?receivingRepository.findAll():receivingRepository.findByAcquisitionIdOrderByCreatedAtAsc(acquisitionId);}
 public EquipmentReceiving get(Long id){return receivingRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Receiving not found: "+id));}
 @Transactional public EquipmentReceiving create(EquipmentReceivingContract.CreateRequest r){
  if(r==null)throw new IllegalArgumentException("Receiving request is required."); ReceivingSourceType source=r.sourceType()==null?ReceivingSourceType.ACQUISITION:r.sourceType();
  if(source==ReceivingSourceType.ACQUISITION&&r.acquisitionId()==null)throw new IllegalArgumentException("Acquisition is required when receiving source is ACQUISITION.");
  if(r.items()==null||r.items().isEmpty())throw new IllegalArgumentException("At least one receiving item is required.");
  Purchase acquisition=r.acquisitionId()==null?null:purchaseRepository.findById(r.acquisitionId()).orElseThrow(()->new IllegalArgumentException("Acquisition not found: "+r.acquisitionId()));
  EquipmentReceiving e=new EquipmentReceiving();e.sourceType=source;e.acquisition=acquisition;
  if(r.receivingOrganizationId()!=null){Organization o=em.find(Organization.class,r.receivingOrganizationId());if(o==null)throw new IllegalArgumentException("Organization not found: "+r.receivingOrganizationId());e.receivingOrganization=o;}
  e.receivingUnit=clean(r.receivingUnit());e.receivingLocation=clean(r.receivingLocation());e.deliveryDocumentNumber=clean(r.deliveryDocumentNumber());e.invoiceNumber=clean(r.invoiceNumber());e.receivedAt=r.receivedAt()==null?LocalDateTime.now():r.receivedAt();e.receivedBy=clean(r.receivedBy());e.physicalChecked=Boolean.TRUE.equals(r.physicalChecked());e.documentsChecked=Boolean.TRUE.equals(r.documentsChecked());e.notes=clean(r.notes());e.status=ReceivingStatus.RECEIVED;e=receivingRepository.save(e);
  boolean partial=false;
  for(var x:r.items()){
   if(x.receivedQuantity()==null||x.receivedQuantity().signum()<=0)throw new IllegalArgumentException("Received quantity must be greater than zero.");
   PurchaseItem ai=null;ItemModel model;BigDecimal expected;
   if(source==ReceivingSourceType.ACQUISITION){if(x.acquisitionItemId()==null)throw new IllegalArgumentException("Acquisition item is required.");ai=em.find(PurchaseItem.class,x.acquisitionItemId(),LockModeType.PESSIMISTIC_WRITE);if(ai==null)throw new IllegalArgumentException("Acquisition item not found: "+x.acquisitionItemId());if(ai.purchase==null||!acquisition.id.equals(ai.purchase.id))throw new IllegalArgumentException("Acquisition item does not belong to selected acquisition.");model=ai.itemModel;expected=ai.pendingQuantity();if(x.itemModelId()!=null&&!model.id.equals(x.itemModelId()))throw new IllegalArgumentException("Received model differs from acquired item model.");}else{if(x.itemModelId()==null)throw new IllegalArgumentException("Item model is required.");model=em.find(ItemModel.class,x.itemModelId());if(model==null)throw new IllegalArgumentException("Item model not found.");expected=x.expectedQuantity();}
   EquipmentReceivingItem item=new EquipmentReceivingItem();item.receiving=e;item.acquisitionItem=ai;item.itemModelId=model.id;item.expectedQuantity=expected;item.receivedQuantity=x.receivedQuantity();item.acceptedQuantity=BigDecimal.ZERO;item.rejectedQuantity=BigDecimal.ZERO;item.lotNumber=clean(x.lotNumber());item.manufactureDate=x.manufactureDate();item.expirationDate=x.expirationDate();item.conditionDescription=clean(x.conditionDescription());item.notes=clean(x.notes());
   if(expected!=null&&x.receivedQuantity().compareTo(expected)!=0){item.divergenceDescription="Expected "+expected.toPlainString()+", received "+x.receivedQuantity().toPlainString()+".";partial|=x.receivedQuantity().compareTo(expected)<0;} item=itemRepository.save(item);
   Set<String> seen=new HashSet<>();if(x.serialNumbers()!=null)for(String v:x.serialNumbers()){String sv=clean(v);if(sv==null)continue;if(!seen.add(sv))throw new IllegalArgumentException("Duplicate serial number in receiving: "+sv);ReceivingSerial s=new ReceivingSerial();s.receivingItem=item;s.serialNumber=sv;serialRepository.save(s);}
   if(Boolean.TRUE.equals(model.category.serialized)){long count=x.serialNumbers()==null?0:x.serialNumbers().stream().filter(v->clean(v)!=null).count();if(x.receivedQuantity().stripTrailingZeros().scale()>0||x.receivedQuantity().longValueExact()!=count)throw new IllegalArgumentException("Serialized item quantity must equal serial count.");}
   if(ai!=null){ai.receivedQuantity=nz(ai.receivedQuantity).add(x.receivedQuantity());purchaseItemRepository.save(ai);}
  }
  e.status=partial?ReceivingStatus.PARTIALLY_RECEIVED:ReceivingStatus.RECEIVED;return receivingRepository.save(e);
 }
 @Transactional public EquipmentReceiving changeStatus(Long id,EquipmentReceivingContract.StatusRequest r){EquipmentReceiving e=get(id);if(r.status()==null)throw new IllegalArgumentException("Status is required.");e.status=r.status();if(r.notes()!=null&&!r.notes().isBlank())e.notes=r.notes().trim();return receivingRepository.save(e);}
 private static BigDecimal nz(BigDecimal v){return v==null?BigDecimal.ZERO:v;} private static String clean(String v){if(v==null)return null;v=v.trim();return v.isEmpty()?null:v;}
}
