package com.comandos.demo;

import com.comandos.audit.model.AuditRecord;
import com.comandos.audit.model.AuditReference;
import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.StockLot;
import com.comandos.inventory.model.StockMovement;
import com.comandos.purchase.model.ReceivingIncorporation;
import com.comandos.purchase.model.ReceivingStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1940)
public class AssetTraceabilityDemoVerifier implements ApplicationRunner {

    private final EntityManager em;

    public AssetTraceabilityDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        verifyGlobalIdentity();
        verifyIncorporationTraceability();
        verifyAuditHistoryShape();
    }

    private void verifyGlobalIdentity() {
        Map<String, Long> assetCodes = new HashMap<>();
        Map<String, Long> serials = new HashMap<>();
        Map<String, Long> internalCodes = new HashMap<>();

        for (AssetItem asset : em.createQuery("select a from AssetItem a order by a.id", AssetItem.class).getResultList()) {
            require(asset.location != null, "Asset has no current location: " + asset.id);
            require(!normalized(asset.assetCode).isEmpty(), "Asset has no asset code: " + asset.id);
            require(asset.assetCode.equals(normalized(asset.assetCode)),
                "Asset code is not stored normalized: " + asset.id + "/" + asset.assetCode);
            unique(assetCodes, asset.assetCode, asset.id, "asset code");

            if (!normalized(asset.serialNumber).isEmpty()) {
                require(asset.serialNumber.equals(normalized(asset.serialNumber)),
                    "Serial number is not stored normalized: " + asset.id + "/" + asset.serialNumber);
                unique(serials, asset.serialNumber, asset.id, "serial number");
            }
            if (!normalized(asset.internalCode).isEmpty()) {
                require(asset.internalCode.equals(normalized(asset.internalCode)),
                    "Internal code is not stored normalized: " + asset.id + "/" + asset.internalCode);
                unique(internalCodes, asset.internalCode, asset.id, "internal code");
            }
        }
    }

    private void verifyIncorporationTraceability() {
        List<ReceivingIncorporation> incorporations = em.createQuery(
            "select i from ReceivingIncorporation i order by i.id", ReceivingIncorporation.class
        ).getResultList();

        for (ReceivingIncorporation incorporation : incorporations) {
            require(incorporation.receiving != null && incorporation.receivingItem != null,
                "Incorporation lost receiving provenance: " + incorporation.id);
            require(incorporation.receiving.status == ReceivingStatus.DEFINITIVELY_ACCEPTED
                    || incorporation.receiving.status == ReceivingStatus.DEFINITIVELY_PARTIALLY_ACCEPTED,
                "Incorporation does not originate from a definitively accepted receiving: " + incorporation.id);
            require(incorporation.receivingItem.receiving != null
                    && incorporation.receiving.id.equals(incorporation.receivingItem.receiving.id),
                "Incorporation receiving-item link is inconsistent: " + incorporation.id);
            require(incorporation.quantity != null && incorporation.quantity.signum() > 0,
                "Incorporation quantity is invalid: " + incorporation.id);

            if ("assets".equals(incorporation.inventoryResource)) {
                require(incorporation.quantity.compareTo(BigDecimal.ONE) == 0,
                    "Individual incorporation must create exactly one asset: " + incorporation.id);
                AssetItem asset = em.find(AssetItem.class, incorporation.inventoryRecordId);
                require(asset != null, "Incorporated asset does not exist: " + incorporation.id);
                if (incorporation.receivingSerial != null) {
                    require(Boolean.TRUE.equals(incorporation.receivingSerial.accepted),
                        "Rejected serial reached asset inventory: " + incorporation.id);
                    require(normalized(incorporation.receivingSerial.serialNumber).equals(normalized(asset.serialNumber)),
                        "Receiving serial and asset serial differ: " + incorporation.id);
                }
                verifyOpeningMovement("asset", asset.id, null, asset.location);
                verifyCreateAudit("inventory/assets", asset.id, asset.location);
            } else if ("lots".equals(incorporation.inventoryResource)) {
                StockLot lot = em.find(StockLot.class, incorporation.inventoryRecordId);
                require(lot != null, "Incorporated lot does not exist: " + incorporation.id);
                require(lot.openingLocation != null, "Incorporated lot has no opening location: " + incorporation.id);
                verifyOpeningMovement("lot", null, lot.id, lot.openingLocation);
                verifyCreateAudit("inventory/lots", lot.id, lot.openingLocation);
            } else {
                throw new IllegalStateException("Unsupported incorporation inventory resource: " + incorporation.inventoryResource);
            }
        }
    }

    private void verifyOpeningMovement(String label, Long assetId, Long lotId, StockLocation location) {
        String association = assetId != null ? "m.asset.id = :recordId" : "m.lot.id = :recordId";
        Long recordId = assetId != null ? assetId : lotId;
        List<StockMovement> movements = em.createQuery(
                "select m from StockMovement m where " + association + " order by m.id", StockMovement.class)
            .setParameter("recordId", recordId)
            .getResultList();
        require(!movements.isEmpty(), "Incorporated " + label + " has no stock movement: " + recordId);
        StockMovement opening = movements.stream()
            .filter(m -> "OPENING".equals(m.nature))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Incorporated " + label + " has no opening movement: " + recordId));
        require(opening.location != null && opening.location.id.equals(location.id),
            "Opening movement lost historical location: " + label + "/" + recordId);
        require(opening.movedAt != null && opening.quantity != null && opening.quantity.signum() > 0,
            "Opening movement is incomplete: " + label + "/" + recordId);
    }

    private void verifyCreateAudit(String resource, Long recordId, StockLocation location) {
        AuditRecord event = em.createQuery(
                "select a from AuditRecord a where a.resource = :resource and a.recordId = :recordId "
                    + "and a.action = 'CREATE' order by a.id", AuditRecord.class)
            .setParameter("resource", resource)
            .setParameter("recordId", recordId)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Inventory creation has no audit event: " + resource + "/" + recordId));

        require(event.occurredAt != null && notBlank(event.actorType),
            "Audit event has no timestamp/actor type: " + event.id);
        if ("ACCOUNT".equals(event.actorType)) {
            require(event.actorId != null && notBlank(event.actorLogin),
                "Authenticated audit event lost user identity: " + event.id);
        }
        require(event.afterJson != null, "CREATE audit event has no after snapshot: " + event.id);

        require(referenceExists(event.id, resource, recordId),
            "Audit event lost inventory reference: " + event.id);
        require(location.organizationLegacyId != null
                && referenceExists(event.id, "organization", location.organizationLegacyId),
            "Audit event lost historical organization reference: " + event.id);
        if (location.unitLegacyId != null) {
            require(referenceExists(event.id, "unit", location.unitLegacyId),
                "Audit event lost historical unit reference: " + event.id);
        }
    }

    private void verifyAuditHistoryShape() {
        for (AuditRecord event : em.createQuery(
            "select a from AuditRecord a where a.resource in ('inventory/assets','inventory/lots','inventory/movements')",
            AuditRecord.class
        ).getResultList()) {
            require(event.occurredAt != null && notBlank(event.action) && notBlank(event.actorType),
                "Inventory audit history is incomplete: " + event.id);
            if ("UPDATE".equals(event.action)) {
                require(event.beforeJson != null && event.afterJson != null,
                    "Inventory transition audit lacks before/after snapshots: " + event.id);
            }
        }
    }

    private boolean referenceExists(Long eventId, String kind, Long targetId) {
        return em.createQuery(
                "select count(r) from AuditReference r where r.eventId = :eventId and r.kind = :kind and r.targetId = :targetId",
                Long.class)
            .setParameter("eventId", eventId)
            .setParameter("kind", kind)
            .setParameter("targetId", targetId)
            .getSingleResult() > 0;
    }

    private static void unique(Map<String, Long> seen, String raw, Long id, String label) {
        String value = normalized(raw);
        Long previous = seen.putIfAbsent(value, id);
        if (previous != null && !previous.equals(id)) {
            throw new IllegalStateException(
                "Duplicate global " + label + " after normalization: assets " + previous + " and " + id + ".");
        }
    }

    private static String normalized(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replaceAll("[\\p{javaWhitespace}\\p{Z}]", "")
            .toUpperCase(Locale.ROOT);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
