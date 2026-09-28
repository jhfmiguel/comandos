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

@Service
@Transactional(readOnly=true)
public class DonationReceiptService {
    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;

    public DonationReceiptService(EntityManager em, AccessPolicy access, AuditService audit) {
        this.em = em; this.access = access; this.audit = audit;
    }

    @Transactional
    public DonationView receive(ReceiveRequest request) {
        validate(request);
        access.requireScope("donations", "CREATE", request.organizationId(), request.unitId());
        Organization organization = locked(Organization.class, request.organizationId());
        OrganizationalUnit unit = selectedUnit(organization.id, request.unitId());
        Person donor = locked(Person.class, request.donorId());
        Person donee = locked(Person.class, request.doneeId());
        if (!organization.active || !donor.active || !donee.active) bad("Organization, donor and donee must be active.");
        if (donor.id.equals(donee.id)) bad("Donor and donee must be different people.");
        access.requireEntity("core/people", "READ", donor);
        access.requireEntity("core/people", "READ", donee);

        String fingerprint = fingerprint(request);
        var existing = em.createQuery("select d from Donation d where d.requestId=:id", Donation.class)
            .setParameter("id", request.requestId()).getResultStream().findFirst();
        if (existing.isPresent()) {
            if (!existing.get().requestFingerprint.equals(fingerprint)) conflict("This request ID was already used for a different donation.");
            return view(existing.get());
        }

        LocalDateTime now = LocalDateTime.now();
        var actor = audit.actor();
        Donation donation = new Donation();
        donation.organization = organization; donation.unit = unit; donation.donor = donor; donation.donee = donee;
        donation.organizationName = organization.name; donation.unitName = unit == null ? null : unit.name;
        donation.donorName = donor.fullName; donation.doneeName = donee.fullName; donation.term = request.term().trim();
        donation.direction = "INCOMING"; donation.eventType = "RECEIVED"; donation.termConfirmed = true;
        donation.titleTransferState = "TRANSFERRED_TO_ORGANIZATION"; donation.titleTransferredAt = now;
        donation.documentReference = request.documentReference().trim(); donation.approvedById = actor.id();
        donation.approvedByLogin = actor.login(); donation.approvedAt = now; donation.status = "FINALIZED";
        donation.finalizedAt = now; donation.receivedAt = now; donation.finalizedById = actor.id();
        donation.finalizedByLogin = actor.login(); donation.requestId = request.requestId(); donation.requestFingerprint = fingerprint;
        em.persist(donation); em.flush();

        List<Map<String,Object>> changes = new ArrayList<>();
        for (ReceiptLineRequest line : request.items()) {
            ItemModel model = locked(ItemModel.class, line.modelId());
            StockLocation location = locked(StockLocation.class, line.destinationLocationId());
            validateDestination(location, organization.id, request.unitId());
            BigDecimal quantity = line.quantity();
            String condition = clean(line.condition()) == null ? AssetCondition.GOOD.name() : clean(line.condition()).toUpperCase(Locale.ROOT);
            DonationItem item = new DonationItem();
            item.donation = donation; item.model = model; item.location = location; item.modelName = model.name;
            item.sku = model.sku; item.locationName = location.name; item.unitOfMeasure = model.unitOfMeasure;
            item.previousOwnerType = "DONOR"; item.previousOwnerName = donor.fullName;
            item.newOwnerType = "ORGANIZATION"; item.newOwnerName = organization.name;

            StockMovement movement = new StockMovement();
            movement.location = location; movement.nature = StockMovementNature.DONATION_IN.name();
            movement.referenceType = StockMovementReferenceType.DONATION.name(); movement.referenceId = donation.id;
            movement.movedAt = now; movement.operatorId = actor.id(); movement.operatorLogin = actor.login();

            if (Boolean.TRUE.equals(model.category.serialized)) {
                if (quantity.compareTo(BigDecimal.ONE) != 0) bad("Serialized donated assets require quantity 1.");
                String assetCode = required(line.assetCode(), "Asset code is required for serialized donated assets.");
                assertAssetIdentityAvailable(assetCode, clean(line.serialNumber()), clean(line.internalCode()));
                AssetItem asset = new AssetItem(); asset.model = model; asset.location = location; asset.assetCode = assetCode;
                asset.serialNumber = clean(line.serialNumber()); asset.internalCode = clean(line.internalCode()); asset.condition = condition;
                asset.status = AssetStatus.AVAILABLE.name(); asset.currentValue = model.listPrice == null ? BigDecimal.ZERO : model.listPrice;
                em.persist(asset); em.flush();
                item.asset = asset; item.stockCode = asset.assetCode; item.quantity = BigDecimal.ONE;
                movement.asset = asset; movement.quantity = BigDecimal.ONE;
                changes.add(Map.of("resource","inventory/assets","recordId",asset.id,"ownership",organization.name,"status",asset.status));
            } else {
                String lotNumber = required(line.lotNumber(), "Lot number is required for non-serialized donated stock.");
                assertLotAvailable(model.id, location.id, lotNumber);
                StockLot lot = new StockLot(); lot.model = model; lot.openingLocation = location; lot.lotNumber = lotNumber;
                lot.initialQuantity = quantity; lot.availableQuantity = quantity; lot.condition = condition; lot.status = AssetStatus.AVAILABLE.name();
                em.persist(lot); em.flush();
                StockBalance balance = new StockBalance(); balance.lot = lot; balance.location = location; balance.available = quantity;
                em.persist(balance); em.flush();
                item.lot = lot; item.stockCode = lot.lotNumber; item.quantity = quantity;
                movement.lot = lot; movement.quantity = quantity;
                changes.add(Map.of("resource","inventory/balances","recordId",balance.id,"ownership",organization.name,"available",quantity.toPlainString()));
            }
            em.persist(movement); item.movement = movement; em.persist(item);
        }
        em.flush();
        DonationView result = view(donation);
        audit.record("donations", donation.id, "RECEIVE", null, Map.of(
            "donation", result, "direction", "INCOMING", "eventType", "RECEIVED",
            "titleTransferState", donation.titleTransferState, "documentReference", donation.documentReference,
            "stockChanges", changes));
        return result;
    }

    private DonationView view(Donation d) {
        var items = em.createQuery("select i from DonationItem i where i.donation.id=:id order by i.id", DonationItem.class)
            .setParameter("id", d.id).getResultList().stream().map(i -> new LineView(i.id,i.model.id,
                i.asset==null?null:i.asset.id,i.lot==null?null:i.lot.id,i.location.id,i.movement.id,
                i.modelName,i.sku,i.stockCode,i.locationName,i.unitOfMeasure,decimal(i.quantity))).toList();
        return new DonationView(d.id,d.organization.id,d.organizationName,d.unit==null?null:d.unit.id,d.unitName,
            d.donor.id,d.donorName,d.donee.id,d.doneeName,d.term,d.status,d.finalizedAt.toString(),d.finalizedById,d.finalizedByLogin,items);
    }

    private void validate(ReceiveRequest r) {
        if (r == null || r.organizationId()==null || r.donorId()==null || r.doneeId()==null || r.term()==null || r.term().isBlank())
            bad("Organization, donor, donee and term are required.");
        uuid(r.requestId());
        if (r.documentReference()==null || r.documentReference().isBlank()) bad("A document reference is required for a received donation.");
        if (r.term().trim().length()>255 || r.documentReference().trim().length()>500) bad("Donation term or document reference is too long.");
        if (r.items()==null || r.items().isEmpty() || r.items().size()>100) bad("A received donation requires 1 to 100 items.");
        for (var line : r.items()) {
            if (line==null || line.modelId()==null || line.destinationLocationId()==null) bad("Model and destination location are required for every donated item.");
            amount(line.quantity());
        }
    }

    private void validateDestination(StockLocation l, long organizationId, Long unitId) {
        if (!Boolean.TRUE.equals(l.active) || !l.organization.id.equals(organizationId)) bad("Donation destination must be an active location in the selected organization.");
        if (unitId != null && (l.unit == null || !unitId.equals(l.unit.id))) bad("Donation destination must belong to the selected unit.");
    }

    private void assertAssetIdentityAvailable(String assetCode,String serial,String internalCode) {
        long code = em.createQuery("select count(a) from AssetItem a where upper(a.assetCode)=:v",Long.class).setParameter("v",assetCode.toUpperCase(Locale.ROOT)).getSingleResult();
        if (code>0) conflict("Asset code already exists.");
        if (serial!=null && em.createQuery("select count(a) from AssetItem a where upper(a.serialNumber)=:v",Long.class).setParameter("v",serial.toUpperCase(Locale.ROOT)).getSingleResult()>0) conflict("Serial number already exists.");
        if (internalCode!=null && em.createQuery("select count(a) from AssetItem a where upper(a.internalCode)=:v",Long.class).setParameter("v",internalCode.toUpperCase(Locale.ROOT)).getSingleResult()>0) conflict("Internal code already exists.");
    }

    private void assertLotAvailable(Long modelId,Long locationId,String lotNumber) {
        long count=em.createQuery("select count(l) from StockLot l where l.model.id=:m and l.openingLocation.id=:l and upper(l.lotNumber)=:n",Long.class)
            .setParameter("m",modelId).setParameter("l",locationId).setParameter("n",lotNumber.toUpperCase(Locale.ROOT)).getSingleResult();
        if(count>0) conflict("Lot number already exists for this model and location.");
    }

    private OrganizationalUnit selectedUnit(long org,Long id){if(id==null)return null;var u=em.find(OrganizationalUnit.class,id);if(u==null||!u.organization.id.equals(org)||!Boolean.TRUE.equals(u.active))bad("Select an active unit in the selected organization.");return u;}
    private <T>T locked(Class<T> type,Long id){if(id==null||id<=0)bad("A valid record ID is required.");var value=em.find(type,id,LockModeType.PESSIMISTIC_WRITE);if(value==null)bad(type.getSimpleName()+" not found.");return value;}
    private static String fingerprint(ReceiveRequest r){String lines=r.items().stream().map(i->i.modelId()+":"+i.destinationLocationId()+":"+clean(i.assetCode())+":"+clean(i.serialNumber())+":"+clean(i.internalCode())+":"+clean(i.lotNumber())+":"+i.quantity().stripTrailingZeros().toPlainString()).toList().toString();return hash(r.organizationId()+"|"+r.unitId()+"|"+r.donorId()+"|"+r.doneeId()+"|"+r.term().trim()+"|"+r.documentReference().trim()+"|"+lines);}
    private static String required(String v,String message){if(v==null||v.isBlank())bad(message);return v.trim();}
    private static String clean(String v){return v==null||v.isBlank()?null:v.trim();}
    private static void amount(BigDecimal v){if(v==null||v.signum()<=0||v.stripTrailingZeros().scale()>4||v.precision()-v.scale()>15)bad("Quantities must be positive with at most 15 integer and 4 decimal digits.");}
    private static String decimal(BigDecimal v){return v.setScale(4,RoundingMode.UNNECESSARY).toPlainString();}
    private static String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static void uuid(String v){try{if(v==null||!UUID.fromString(v).toString().equals(v))throw new IllegalArgumentException();}catch(IllegalArgumentException e){bad("A canonical UUID request ID is required.");}}
    private static void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);} private static void conflict(String m){throw new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
