package com.weaponsregistration.purchase.service;
import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.core.model.Person;
import com.weaponsregistration.inventory.model.ItemModel;
import com.weaponsregistration.purchase.dto.PurchaseContract.*;
import com.weaponsregistration.purchase.model.*;
import com.weaponsregistration.purchase.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
@Service
public class PurchaseService {
 private final PurchaseRepository purchases; private final ProcurementProcessRepository procurements; private final EntityManager em;
 public PurchaseService(PurchaseRepository p,ProcurementProcessRepository pr,EntityManager em){this.purchases=p;this.procurements=pr;this.em=em;}
 @Transactional public PurchaseView create(CreatePurchaseRequest r){
  if(r==null||r.buyerOrganizationId()==null)throw new IllegalArgumentException("Buyer organization is required.");
  if(r.originPersonId()==null)throw new IllegalArgumentException("Acquisition origin person is required.");
  if(blank(r.purchaseNumber()))throw new IllegalArgumentException("Acquisition number is required.");
  if(r.items()==null||r.items().isEmpty())throw new IllegalArgumentException("Acquisition requires at least one item.");
  Purchase p=new Purchase();p.buyerOrganization=org(r.buyerOrganizationId());p.supplierOrganization=r.supplierOrganizationId()==null?null:org(r.supplierOrganizationId());p.originPerson=person(r.originPersonId());
  p.acquisitionType=r.acquisitionType()==null?AcquisitionType.ONEROUS:r.acquisitionType();p.originDescription=trim(r.originDescription());
  p.purchaseNumber=r.purchaseNumber().trim();p.purchaseDate=r.purchaseDate()==null?LocalDate.now():r.purchaseDate();
  p.discount=money(r.discount());p.freight=money(r.freight());p.taxes=money(r.taxes());p.otherCosts=money(r.otherCosts());
  p.paymentConditions=trim(r.paymentConditions());p.deliveryConditions=trim(r.deliveryConditions());p.warrantyConditions=trim(r.warrantyConditions());p.notes=trim(r.notes());
  if(p.originPerson==null)throw new IllegalArgumentException("Acquisition origin person is required.");
  for(CreatePurchaseItemRequest x:r.items()){
   if(x.itemModelId()==null||x.quantity()==null||x.quantity().signum()<=0)throw new IllegalArgumentException("Valid item and quantity are required.");
   ItemModel m=em.find(ItemModel.class,x.itemModelId());if(m==null)throw new EntityNotFoundException("ItemModel not found: "+x.itemModelId());
   PurchaseItem i=new PurchaseItem();i.purchase=p;i.itemModel=m;i.quantity=x.quantity();i.unitPrice=p.acquisitionType==AcquisitionType.FREE?BigDecimal.ZERO:money(x.unitPrice());
   i.discount=p.acquisitionType==AcquisitionType.FREE?BigDecimal.ZERO:money(x.discount());i.conditionDescription=trim(x.conditionDescription());i.notes=trim(x.notes());p.items.add(i);
  }
  if(p.acquisitionType==AcquisitionType.FREE){p.discount=BigDecimal.ZERO;p.freight=BigDecimal.ZERO;p.taxes=BigDecimal.ZERO;p.otherCosts=BigDecimal.ZERO;}
  if(r.documents()!=null)for(DocumentRequest x:r.documents()){if(x.documentType()==null)throw new IllegalArgumentException("Document type is required.");
   AcquisitionDocument d=new AcquisitionDocument();d.purchase=p;d.documentType=x.documentType();d.documentNumber=trim(x.documentNumber());d.issueDate=x.issueDate();
   d.issuer=trim(x.issuer());d.amount=x.amount()==null?null:money(x.amount());d.storageReference=trim(x.storageReference());d.notes=trim(x.notes());p.documents.add(d);}
  p.recalculateTotals();return view(purchases.save(p));
 }
 @Transactional public PurchaseView configureProcurement(Long id,CreateProcurementRequest r){
  Purchase p=find(id);assertMutableBeforeReceiving(p);boolean pub=Boolean.TRUE.equals(p.buyerOrganization.publicOrganization);
  if(r==null||r.procurementMethod()==null)throw new IllegalArgumentException("Procurement method is required.");
  if(!pub&&r.procurementMethod()!=ProcurementMethod.NOT_REQUIRED)throw new IllegalArgumentException("Private acquisition must not be forced into public procurement.");
  if(pub&&p.acquisitionType==AcquisitionType.ONEROUS&&r.procurementMethod()==ProcurementMethod.NOT_REQUIRED)throw new IllegalArgumentException("Public onerous acquisition requires an applicable procurement/direct-contracting process.");
  if(r.procurementMethod()!=ProcurementMethod.NOT_REQUIRED&&blank(r.processNumber()))throw new IllegalArgumentException("Process number is required.");
  if(r.procurementMethod()!=ProcurementMethod.NOT_REQUIRED&&blank(r.objectDescription()))throw new IllegalArgumentException("Procurement object is required.");
  if(r.procurementMethod()==ProcurementMethod.BIDDING&&r.biddingModality()==null)throw new IllegalArgumentException("Bidding modality is required.");
  if(r.procurementMethod()==ProcurementMethod.DIRECT_CONTRACTING&&r.directContractingType()==null)throw new IllegalArgumentException("Direct contracting type is required.");
  if(r.procurementMethod()==ProcurementMethod.DIRECT_CONTRACTING&&blank(r.legalBasis()))throw new IllegalArgumentException("Legal basis is required.");
  if(r.procurementMethod()==ProcurementMethod.DIRECT_CONTRACTING&&blank(r.supplierChoiceReason()))throw new IllegalArgumentException("Supplier choice reason is required for direct contracting.");
  if(r.procurementMethod()==ProcurementMethod.DIRECT_CONTRACTING&&blank(r.priceJustification()))throw new IllegalArgumentException("Price justification is required for direct contracting.");
  if(r.procurementMethod()==ProcurementMethod.NOT_REQUIRED){p.procurementProcess=null;p.status=PurchaseStatus.AUTHORIZED;return view(purchases.save(p));}
  ProcurementProcess x=p.procurementProcess==null?new ProcurementProcess():p.procurementProcess;x.organization=p.buyerOrganization;x.processNumber=r.processNumber().trim();
  x.objectDescription=r.objectDescription();x.justification=r.justification();x.procurementMethod=r.procurementMethod();x.biddingModality=r.procurementMethod()==ProcurementMethod.BIDDING?r.biddingModality():null;
  x.directContractingType=r.procurementMethod()==ProcurementMethod.DIRECT_CONTRACTING?r.directContractingType():null;x.estimatedValue=r.estimatedValue()==null?p.total:r.estimatedValue();
  x.legalBasis=r.legalBasis();x.supplierChoiceReason=r.supplierChoiceReason();x.priceJustification=r.priceJustification();p.procurementProcess=procurements.save(x);p.status=PurchaseStatus.PROCUREMENT_IN_PROGRESS;
  return view(purchases.save(p));
 }

 @Transactional public PurchaseView updateProcurementStatus(Long id, ProcurementStatusRequest r){
  Purchase p=find(id);assertMutableBeforeReceiving(p);
  if(p.procurementProcess==null)throw new IllegalStateException("Acquisition has no procurement process.");
  if(r==null||r.status()==null)throw new IllegalArgumentException("Procurement status is required.");
  ProcurementStatus current=p.procurementProcess.status;
  ProcurementStatus target=r.status();
  if(current==target)return view(p);
  Map<ProcurementStatus,Set<ProcurementStatus>> allowed=Map.ofEntries(
   Map.entry(ProcurementStatus.DRAFT,Set.of(ProcurementStatus.PLANNING,ProcurementStatus.UNDER_REVIEW,ProcurementStatus.CANCELLED)),
   Map.entry(ProcurementStatus.PLANNING,Set.of(ProcurementStatus.UNDER_REVIEW,ProcurementStatus.CANCELLED)),
   Map.entry(ProcurementStatus.UNDER_REVIEW,Set.of(ProcurementStatus.AUTHORIZED,ProcurementStatus.CANCELLED,ProcurementStatus.FAILED)),
   Map.entry(ProcurementStatus.AUTHORIZED,Set.of(ProcurementStatus.PUBLISHED,ProcurementStatus.PROPOSAL_PHASE,ProcurementStatus.HOMOLOGATED,ProcurementStatus.CANCELLED)),
   Map.entry(ProcurementStatus.PUBLISHED,Set.of(ProcurementStatus.PROPOSAL_PHASE,ProcurementStatus.CANCELLED,ProcurementStatus.FAILED)),
   Map.entry(ProcurementStatus.PROPOSAL_PHASE,Set.of(ProcurementStatus.QUALIFICATION_PHASE,ProcurementStatus.JUDGMENT_PHASE,ProcurementStatus.CANCELLED,ProcurementStatus.FAILED)),
   Map.entry(ProcurementStatus.QUALIFICATION_PHASE,Set.of(ProcurementStatus.JUDGMENT_PHASE,ProcurementStatus.CANCELLED,ProcurementStatus.FAILED)),
   Map.entry(ProcurementStatus.JUDGMENT_PHASE,Set.of(ProcurementStatus.APPEAL_PHASE,ProcurementStatus.AWARDED,ProcurementStatus.CANCELLED,ProcurementStatus.FAILED)),
   Map.entry(ProcurementStatus.APPEAL_PHASE,Set.of(ProcurementStatus.AWARDED,ProcurementStatus.CANCELLED,ProcurementStatus.FAILED)),
   Map.entry(ProcurementStatus.AWARDED,Set.of(ProcurementStatus.HOMOLOGATED,ProcurementStatus.CANCELLED,ProcurementStatus.FAILED)),
   Map.entry(ProcurementStatus.HOMOLOGATED,Set.of(ProcurementStatus.CONTRACTED,ProcurementStatus.CANCELLED)),
   Map.entry(ProcurementStatus.CONTRACTED,Set.of())
  );
  if(!allowed.getOrDefault(current,Set.of()).contains(target))throw new IllegalStateException("Invalid procurement status transition: "+current+" -> "+target+".");
  p.procurementProcess.status=target;
  LocalDate today=LocalDate.now();
  if(target==ProcurementStatus.PUBLISHED)p.procurementProcess.publicationDate=today;
  if(target==ProcurementStatus.AWARDED)p.procurementProcess.awardDate=today;
  if(target==ProcurementStatus.HOMOLOGATED)p.procurementProcess.homologationDate=today;
  procurements.save(p.procurementProcess);
  return view(purchases.save(p));
 }

 @Transactional public PurchaseView authorize(Long id){
  Purchase p=find(id);assertMutableBeforeReceiving(p);
  if(p.status==PurchaseStatus.AUTHORIZED||p.status==PurchaseStatus.ORDERED)return view(p);
  boolean publicOnerous=Boolean.TRUE.equals(p.buyerOrganization.publicOrganization)&&p.acquisitionType==AcquisitionType.ONEROUS;
  if(publicOnerous){
   if(p.procurementProcess==null)throw new IllegalStateException("Public onerous acquisition requires a procurement process before authorization.");
   if(!Set.of(ProcurementStatus.HOMOLOGATED,ProcurementStatus.CONTRACTED).contains(p.procurementProcess.status))
    throw new IllegalStateException("Procurement must be homologated or contracted before acquisition authorization.");
  }
  p.status=PurchaseStatus.AUTHORIZED;
  return view(purchases.save(p));
 }

 @Transactional public PurchaseView order(Long id){
  Purchase p=find(id);assertMutableBeforeReceiving(p);
  if(p.status==PurchaseStatus.ORDERED)return view(p);
  if(p.status!=PurchaseStatus.AUTHORIZED)throw new IllegalStateException("Only an authorized acquisition can be ordered.");
  p.status=PurchaseStatus.ORDERED;
  return view(purchases.save(p));
 }

 @Transactional(readOnly=true) public PurchaseView get(Long id){return view(find(id));}
 @Transactional(readOnly=true) public List<PurchaseView> list(Long organizationId){return purchases.findByBuyerOrganizationIdOrderByCreatedAtDesc(organizationId).stream().map(this::view).toList();}
 @Transactional public PurchaseView cancel(Long id){Purchase p=find(id);if(p.items.stream().anyMatch(i->nz(i.receivedQuantity).signum()>0))throw new IllegalStateException("Acquisition with received items cannot be cancelled.");p.status=PurchaseStatus.CANCELLED;return view(purchases.save(p));}
 private PurchaseView view(Purchase p){
  ProcurementView pv=null;if(p.procurementProcess!=null){var x=p.procurementProcess;pv=new ProcurementView(x.id,x.processNumber,x.procurementMethod,x.biddingModality,x.directContractingType,x.status,x.estimatedValue,x.legalBasis);}
  var iv=p.items.stream().map(i->new PurchaseItemView(i.id,i.itemModel.id,i.itemModel.name,i.quantity,i.receivedQuantity,i.unitPrice,i.discount,i.calculateTotal(),i.conditionDescription)).toList();
  var dv=p.documents.stream().map(d->new DocumentView(d.id,d.documentType,d.documentNumber,d.issueDate,d.issuer,d.amount,d.storageReference,d.notes)).toList();
  return new PurchaseView(p.id,p.buyerOrganization.id,p.buyerOrganization.name,p.buyerOrganization.publicOrganization,p.supplierOrganization==null?null:p.supplierOrganization.id,p.supplierOrganization==null?null:p.supplierOrganization.name,
   p.originPerson.id,p.originPerson.fullName,p.originPerson.personType,p.originPerson.taxId,p.acquisitionType,p.originDescription,p.purchaseNumber,p.purchaseDate,p.status,p.subtotal,p.discount,p.freight,p.taxes,p.otherCosts,p.total,p.paymentConditions,p.deliveryConditions,p.warrantyConditions,p.notes,pv,iv,dv);
 }
 private Organization org(Long id){Organization o=em.find(Organization.class,id);if(o==null)throw new EntityNotFoundException("Organization not found: "+id);return o;}
 private Person person(Long id){Person p=em.find(Person.class,id);if(p==null)throw new EntityNotFoundException("Person not found: "+id);if(!Boolean.TRUE.equals(p.active))throw new IllegalArgumentException("Acquisition origin person must be active.");return p;}
 private Purchase find(Long id){return purchases.findById(id).orElseThrow(()->new EntityNotFoundException("Purchase not found: "+id));}
 private void assertMutableBeforeReceiving(Purchase p){if(p.status==PurchaseStatus.CANCELLED)throw new IllegalStateException("Cancelled acquisition cannot be changed.");if(p.items.stream().anyMatch(i->nz(i.receivedQuantity).signum()>0))throw new IllegalStateException("Acquisition cannot change contracting or authorization after receiving has started.");}
 private BigDecimal nz(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
 private BigDecimal money(BigDecimal v){if(v==null)return BigDecimal.ZERO;if(v.signum()<0)throw new IllegalArgumentException("Financial values cannot be negative.");return v;}
 private String trim(String v){return v==null?null:v.trim();}private boolean blank(String v){return v==null||v.isBlank();}
}