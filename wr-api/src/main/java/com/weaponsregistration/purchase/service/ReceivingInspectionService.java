package com.weaponsregistration.purchase.service;
import com.weaponsregistration.purchase.dto.ReceivingInspectionContract;
import com.weaponsregistration.purchase.model.*;
import com.weaponsregistration.purchase.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class ReceivingInspectionService {
 private final ReceivingInspectionRepository inspections;private final EquipmentReceivingRepository receivings;private final EquipmentReceivingItemRepository items;
 public ReceivingInspectionService(ReceivingInspectionRepository a,EquipmentReceivingRepository b,EquipmentReceivingItemRepository c){inspections=a;receivings=b;items=c;}
 public List<ReceivingInspection> list(Long id){return id==null?inspections.findAll():inspections.findAll().stream().filter(x->x.receiving!=null&&id.equals(x.receiving.id)).toList();}
 @Transactional public ReceivingInspection create(Long id,ReceivingInspectionContract.CreateRequest r){EquipmentReceiving e=receivings.findById(id).orElseThrow(()->new IllegalArgumentException("Receiving not found: "+id));if(!Boolean.TRUE.equals(e.physicalChecked)||!Boolean.TRUE.equals(e.documentsChecked))throw new IllegalStateException("Physical and documentary checks are required before acceptance.");ReceivingInspection x=new ReceivingInspection();x.receiving=e;x.inspectedAt=r.inspectedAt()==null?LocalDateTime.now():r.inspectedAt();x.inspector=r.inspector();x.provisionalReceipt=Boolean.TRUE.equals(r.provisionalReceipt());x.definitiveReceipt=Boolean.TRUE.equals(r.definitiveReceipt());x.approved=Boolean.TRUE.equals(r.approved());x.nonConformity=r.nonConformity();x.decisionNotes=r.decisionNotes();x=inspections.save(x);for(var item:items.findByReceivingIdOrderByCreatedAtAsc(id)){item.acceptedQuantity=x.approved?item.receivedQuantity:BigDecimal.ZERO;item.rejectedQuantity=x.approved?BigDecimal.ZERO:item.receivedQuantity;items.save(item);}e.status=!x.approved?ReceivingStatus.REJECTED:x.definitiveReceipt?ReceivingStatus.DEFINITIVELY_ACCEPTED:x.provisionalReceipt?ReceivingStatus.PROVISIONALLY_ACCEPTED:ReceivingStatus.UNDER_INSPECTION;receivings.save(e);return x;}
}
