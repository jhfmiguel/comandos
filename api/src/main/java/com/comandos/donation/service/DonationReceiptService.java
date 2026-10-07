package com.comandos.donation.service;

import com.comandos.audit.service.AuditService;
import com.comandos.core.service.CanonicalMasterDataDirectory;
import com.comandos.core.service.ProductCanonicalScopeResolver;
import com.comandos.donation.dto.DonationContract.*;
import com.comandos.donation.model.*;
import com.comandos.inventory.model.*;
import com.comandos.security.service.AccessPolicy;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DonationReceiptService {

    private static final TenantId TENANT = TenantId.of("comandos");

    private final EntityManager em;
    private final AccessPolicy access;
    private final AuditService audit;
    private final ProductCanonicalScopeResolver canonicalScope;
    private final CanonicalMasterDataDirectory masterData;

    public DonationReceiptService(
            EntityManager em,
            AccessPolicy access,
            AuditService audit,
            ProductCanonicalScopeResolver canonicalScope,
            CanonicalMasterDataDirectory masterData) {
        this.em = em;
        this.access = access;
        this.audit = audit;
        this.canonicalScope = canonicalScope;
        this.masterData = masterData;
    }

    @Transactional
    public DonationView receive(ReceiveRequest request) {
        validate(request);
        access.requireScope(
            "donations",
            "CREATE",
            request.organizationId(),
            request.unitId()
        );

        OrganizationSnapshot organization = organization(request.organizationId());
        UnitSnapshot unit = selectedUnit(organization.id(), request.unitId());
        PersonSnapshot donor = person(request.donorId());
        PersonSnapshot donee = person(request.doneeId());

        if (!organization.active() || !donor.active() || !donee.active()) {
            bad("Organization, donor and donee must be active.");
        }
        if (donor.id().equals(donee.id())) {
            bad("Donor and donee must be different people.");
        }
        access.requireAny("core/people", "READ");

        String fingerprint = fingerprint(request);
        var existing = em.createQuery(
                "select d from Donation d where d.requestId=:id",
                Donation.class)
            .setParameter("id", request.requestId())
            .getResultStream()
            .findFirst();

        if (existing.isPresent()) {
            if (!existing.get().requestFingerprint.equals(fingerprint)) {
                conflict("This request ID was already used for a different donation.");
            }
            return view(existing.get());
        }

        LocalDateTime now = LocalDateTime.now();
        var actor = audit.actor();

        Donation donation = new Donation();
        donation.organizationLegacyId = organization.id();
        donation.unitLegacyId = unit == null ? null : unit.id();
        donation.donorLegacyId = donor.id();
        donation.doneeLegacyId = donee.id();

        donation.organizationCanonicalId =
            canonicalScope.organization(organization.id());
        donation.unitCanonicalId =
            canonicalScope.unit(unit == null ? null : unit.id());
        donation.donorCanonicalId = canonicalScope.person(donor.id());
        donation.doneeCanonicalId = canonicalScope.person(donee.id());

        donation.organizationName = organization.name();
        donation.unitName = unit == null ? null : unit.name();
        donation.donorName = donor.name();
        donation.doneeName = donee.name();
        donation.term = request.term().trim();
        donation.direction = "INCOMING";
        donation.eventType = "RECEIVED";
        donation.termConfirmed = true;
        donation.titleTransferState = "TRANSFERRED_TO_ORGANIZATION";
        donation.titleTransferredAt = now;
        donation.documentReference = request.documentReference().trim();
        donation.approvedById = actor.id();
        donation.approvedByLogin = actor.login();
        donation.approvedAt = now;
        donation.status = "FINALIZED";
        donation.finalizedAt = now;
        donation.receivedAt = now;
        donation.finalizedById = actor.id();
        donation.finalizedByLogin = actor.login();
        donation.requestId = request.requestId();
        donation.requestFingerprint = fingerprint;

        em.persist(donation);
        em.flush();

        var changes = new ArrayList<Map<String, Object>>();

        for (ReceiptLineRequest line : request.items()) {
            ItemModel model = locked(ItemModel.class, line.modelId());
            StockLocation location =
                locked(StockLocation.class, line.destinationLocationId());

            validateDestination(
                location,
                organization.id(),
                request.unitId()
            );

            BigDecimal quantity = line.quantity();
            String condition = clean(line.condition()) == null
                ? AssetCondition.GOOD.name()
                : clean(line.condition()).toUpperCase(Locale.ROOT);

            DonationItem item = new DonationItem();
            item.donation = donation;
            item.model = model;
            item.location = location;
            item.modelName = model.name;
            item.sku = model.sku;
            item.locationName = location.name;
            item.unitOfMeasure = model.unitOfMeasure;
            item.previousOwnerType = "DONOR";
            item.previousOwnerName = donor.name();
            item.newOwnerType = "ORGANIZATION";
            item.newOwnerName = organization.name();

            StockMovement movement = new StockMovement();
            movement.location = location;
            movement.nature = StockMovementNature.DONATION_IN.name();
            movement.referenceType = StockMovementReferenceType.DONATION.name();
            movement.referenceId = donation.id;
            movement.movedAt = now;
            movement.operatorId = actor.id();
            movement.operatorLogin = actor.login();

            if (Boolean.TRUE.equals(model.category.serialized)) {
                if (quantity.compareTo(BigDecimal.ONE) != 0) {
                    bad("Serialized donated assets require quantity 1.");
                }

                String assetCode = required(
                    line.assetCode(),
                    "Asset code is required for serialized donated assets."
                );

                assertAssetIdentityAvailable(
                    assetCode,
                    clean(line.serialNumber()),
                    clean(line.internalCode())
                );

                AssetItem asset = new AssetItem();
                asset.model = model;
                asset.location = location;
                asset.assetCode = assetCode;
                asset.serialNumber = clean(line.serialNumber());
                asset.internalCode = clean(line.internalCode());
                asset.condition = condition;
                asset.status = AssetStatus.AVAILABLE.name();
                asset.currentValue =
                    model.listPrice == null ? BigDecimal.ZERO : model.listPrice;

                em.persist(asset);
                em.flush();

                item.asset = asset;
                item.stockCode = asset.assetCode;
                item.quantity = BigDecimal.ONE;

                movement.asset = asset;
                movement.quantity = BigDecimal.ONE;

                changes.add(Map.of(
                    "resource", "inventory/assets",
                    "recordId", asset.id,
                    "ownership", organization.name(),
                    "status", asset.status
                ));
            } else {
                String lotNumber = required(
                    line.lotNumber(),
                    "Lot number is required for non-serialized donated stock."
                );

                assertLotAvailable(model.id, location.id, lotNumber);

                StockLot lot = new StockLot();
                lot.model = model;
                lot.openingLocation = location;
                lot.lotNumber = lotNumber;
                lot.initialQuantity = quantity;
                lot.availableQuantity = quantity;
                lot.condition = condition;
                lot.status = AssetStatus.AVAILABLE.name();

                em.persist(lot);
                em.flush();

                StockBalance balance = new StockBalance();
                balance.lot = lot;
                balance.location = location;
                balance.available = quantity;

                em.persist(balance);
                em.flush();

                item.lot = lot;
                item.stockCode = lot.lotNumber;
                item.quantity = quantity;

                movement.lot = lot;
                movement.quantity = quantity;

                changes.add(Map.of(
                    "resource", "inventory/balances",
                    "recordId", balance.id,
                    "ownership", organization.name(),
                    "available", quantity.toPlainString()
                ));
            }

            em.persist(movement);
            item.movement = movement;
            em.persist(item);
        }

        em.flush();

        DonationView result = view(donation);
        audit.record(
            "donations",
            donation.id,
            "RECEIVE",
            null,
            Map.of(
                "donation", result,
                "direction", "INCOMING",
                "eventType", "RECEIVED",
                "titleTransferState", donation.titleTransferState,
                "documentReference", donation.documentReference,
                "stockChanges", changes
            )
        );

        return result;
    }

    private DonationView view(Donation donation) {
        var items = em.createQuery(
                "select i from DonationItem i where i.donation.id=:id order by i.id",
                DonationItem.class)
            .setParameter("id", donation.id)
            .getResultList()
            .stream()
            .map(item -> new LineView(
                item.id,
                item.model.id,
                item.asset == null ? null : item.asset.id,
                item.lot == null ? null : item.lot.id,
                item.location.id,
                item.movement.id,
                item.modelName,
                item.sku,
                item.stockCode,
                item.locationName,
                item.unitOfMeasure,
                decimal(item.quantity)
            ))
            .toList();

        return new DonationView(
            donation.id,
            donation.organizationLegacyId,
            donation.organizationName,
            donation.unitLegacyId,
            donation.unitName,
            donation.donorLegacyId,
            donation.donorName,
            donation.doneeLegacyId,
            donation.doneeName,
            donation.term,
            donation.status,
            donation.finalizedAt.toString(),
            donation.finalizedById,
            donation.finalizedByLogin,
            items
        );
    }

    private void validate(ReceiveRequest request) {
        if (request == null
                || request.organizationId() == null
                || request.donorId() == null
                || request.doneeId() == null
                || request.term() == null
                || request.term().isBlank()) {
            bad("Organization, donor, donee and term are required.");
        }

        uuid(request.requestId());

        if (request.documentReference() == null
                || request.documentReference().isBlank()) {
            bad("A document reference is required for a received donation.");
        }

        if (request.term().trim().length() > 255
                || request.documentReference().trim().length() > 500) {
            bad("Donation term or document reference is too long.");
        }

        if (request.items() == null
                || request.items().isEmpty()
                || request.items().size() > 100) {
            bad("A received donation requires 1 to 100 items.");
        }

        for (var line : request.items()) {
            if (line == null
                    || line.modelId() == null
                    || line.destinationLocationId() == null) {
                bad("Model and destination location are required for every donated item.");
            }
            amount(line.quantity());
        }
    }

    private void validateDestination(
            StockLocation location,
            long organizationId,
            Long unitId) {

        if (!Boolean.TRUE.equals(location.active)) {
            bad("Donation destination must be an active location in the selected organization.");
        }

        if (canonicalScope.enabled()) {
            var scope = canonicalScope.scope(organizationId, unitId);
            if (!location.matchesCanonicalScope(
                    scope.organizationId(),
                    scope.unitId())) {
                bad("Donation destination must belong to the selected canonical organization and unit.");
            }
            return;
        }

        if (!organizationIdEquals(location.organizationLegacyId, organizationId)) {
            bad("Donation destination must be an active location in the selected organization.");
        }

        if (unitId != null && !unitId.equals(location.unitLegacyId)) {
            bad("Donation destination must belong to the selected unit.");
        }
    }

    private OrganizationSnapshot organization(Long id) {
        if (id == null || id <= 0) {
            bad("A valid organization is required.");
        }

        var value = masterData.findOrganization(id, TENANT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Organization not found."
            ));

        return new OrganizationSnapshot(
            id,
            value.legalName(),
            value.status() == LifecycleStatus.ACTIVE
        );
    }

    private PersonSnapshot person(Long id) {
        if (id == null || id <= 0) {
            bad("A valid person is required.");
        }

        var value = masterData.findPerson(id, TENANT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Person not found."
            ));

        return new PersonSnapshot(
            id,
            value.name(),
            value.status() == LifecycleStatus.ACTIVE
        );
    }

    private UnitSnapshot selectedUnit(long organizationId, Long id) {
        if (id == null) return null;

        var value = masterData.findUnit(id, TENANT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Select an active unit in the selected organization."
            ));

        if (!value.active()
                || !CompanyId.of("comandos:organization:" + organizationId)
                    .equals(value.companyId())) {
            bad("Select an active unit in the selected organization.");
        }

        return new UnitSnapshot(id, value.name());
    }

    private <T> T locked(Class<T> type, Long id) {
        if (id == null || id <= 0) {
            bad("A valid record ID is required.");
        }

        var value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) {
            bad(type.getSimpleName() + " not found.");
        }
        return value;
    }

    private void assertAssetIdentityAvailable(
            String assetCode,
            String serial,
            String internalCode) {

        long code = em.createQuery(
                "select count(a) from AssetItem a where upper(a.assetCode)=:v",
                Long.class)
            .setParameter("v", assetCode.toUpperCase(Locale.ROOT))
            .getSingleResult();

        if (code > 0) {
            conflict("Asset code already exists.");
        }

        if (serial != null
                && em.createQuery(
                    "select count(a) from AssetItem a where upper(a.serialNumber)=:v",
                    Long.class)
                    .setParameter("v", serial.toUpperCase(Locale.ROOT))
                    .getSingleResult() > 0) {
            conflict("Serial number already exists.");
        }

        if (internalCode != null
                && em.createQuery(
                    "select count(a) from AssetItem a where upper(a.internalCode)=:v",
                    Long.class)
                    .setParameter("v", internalCode.toUpperCase(Locale.ROOT))
                    .getSingleResult() > 0) {
            conflict("Internal code already exists.");
        }
    }

    private void assertLotAvailable(
            Long modelId,
            Long locationId,
            String lotNumber) {

        long count = em.createQuery(
                "select count(l) from StockLot l "
                    + "where l.model.id=:m and l.openingLocation.id=:l "
                    + "and upper(l.lotNumber)=:n",
                Long.class)
            .setParameter("m", modelId)
            .setParameter("l", locationId)
            .setParameter("n", lotNumber.toUpperCase(Locale.ROOT))
            .getSingleResult();

        if (count > 0) {
            conflict("Lot number already exists for this model and location.");
        }
    }

    private static boolean organizationIdEquals(Long actual, long expected) {
        return actual != null && actual == expected;
    }

    private static String fingerprint(ReceiveRequest request) {
        String lines = request.items().stream()
            .map(item ->
                item.modelId() + ":"
                    + item.destinationLocationId() + ":"
                    + clean(item.assetCode()) + ":"
                    + clean(item.serialNumber()) + ":"
                    + clean(item.internalCode()) + ":"
                    + clean(item.lotNumber()) + ":"
                    + item.quantity().stripTrailingZeros().toPlainString())
            .toList()
            .toString();

        return hash(
            request.organizationId() + "|"
                + request.unitId() + "|"
                + request.donorId() + "|"
                + request.doneeId() + "|"
                + request.term().trim() + "|"
                + request.documentReference().trim() + "|"
                + lines
        );
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            bad(message);
        }
        return value.trim();
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void amount(BigDecimal value) {
        if (value == null
                || value.signum() <= 0
                || value.stripTrailingZeros().scale() > 4
                || value.precision() - value.scale() > 15) {
            bad("Quantities must be positive with at most 15 integer and 4 decimal digits.");
        }
    }

    private static String decimal(BigDecimal value) {
        return value.setScale(4, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void uuid(String value) {
        try {
            if (value == null
                    || !UUID.fromString(value).toString().equals(value)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            bad("A canonical UUID request ID is required.");
        }
    }

    private static void bad(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static void conflict(String message) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record OrganizationSnapshot(
        Long id,
        String name,
        boolean active) {}

    private record UnitSnapshot(
        Long id,
        String name) {}

    private record PersonSnapshot(
        Long id,
        String name,
        boolean active) {}
}
