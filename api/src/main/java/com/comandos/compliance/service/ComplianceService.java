package com.comandos.compliance.service;

import com.comandos.compliance.dto.ComplianceContract.*;
import com.comandos.compliance.model.CompliancePolicy;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.inventory.model.*;
import com.comandos.lifecycle.model.PeriodicInspection;
import com.comandos.maintenance.model.WorkOrder;
import com.comandos.security.service.AccessPolicy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ComplianceService {
    private final EntityManager em;
    private final AccessPolicy access;

    public ComplianceService(EntityManager em, AccessPolicy access) {
        this.em = em;
        this.access = access;
    }

    public SummaryView summary(long organizationId, Long unitId) {
        access.requireScope("inventory/assets", "READ", organizationId, unitId);
        PolicyView policy = policy(organizationId, unitId);
        List<AlertView> alerts = new ArrayList<>();
        List<ItemComplianceView> items = new ArrayList<>();

        for (AssetItem asset : assets(organizationId, unitId)) {
            if (!AssetStatus.terminalCodes().contains(asset.status)) items.add(assetView(asset, policy, alerts));
        }
        for (StockLot lot : lots(organizationId, unitId)) items.add(lotView(lot, policy, alerts));

        alerts.sort(Comparator.comparingInt((AlertView a) -> severityRank(a.severity()))
            .thenComparing(a -> a.dueDate() == null ? "9999-12-31" : a.dueDate())
            .thenComparing(AlertView::code));
        items.sort(Comparator.comparing(ItemComplianceView::kind).thenComparing(ItemComplianceView::code));

        long compliant = items.stream().filter(ItemComplianceView::compliant).count();
        long critical = alerts.stream().filter(a -> "CRITICAL".equals(a.severity())).count();
        long warning = alerts.stream().filter(a -> "WARNING".equals(a.severity())).count();
        long info = alerts.stream().filter(a -> "INFO".equals(a.severity())).count();
        return new SummaryView(organizationId, unitId, policy, items.size(), compliant, items.size() - compliant,
            critical, warning, info, List.copyOf(alerts), List.copyOf(items));
    }

    public PolicyView policy(long organizationId, Long unitId) {
        access.requireScope("inventory/assets", "READ", organizationId, unitId);
        CompliancePolicy value = findPolicy(organizationId, unitId);
        if (value == null) return new PolicyView(null, organizationId, unitId, 30, 30, 30, 180, true);
        return view(value);
    }

    @Transactional
    public PolicyView savePolicy(long organizationId, Long unitId, PolicyRequest request) {
        access.requireScope("inventory/assets", "UPDATE", organizationId, unitId);
        if (request == null) bad("Compliance policy is required.");
        int expiration = days(request.expirationWarningDays(), 30, "Expiration warning days", 0, 3650);
        int maintenance = days(request.maintenanceWarningDays(), 30, "Maintenance warning days", 0, 3650);
        int regulatory = days(request.regulatoryWarningDays(), 30, "Regulatory warning days", 0, 3650);
        int inspection = days(request.inspectionIntervalDays(), 180, "Inspection interval days", 1, 3650);
        Organization organization = em.find(Organization.class, organizationId, LockModeType.PESSIMISTIC_WRITE);
        if (organization == null || !Boolean.TRUE.equals(organization.active)) bad("Active organization is required.");
        OrganizationalUnit unit = null;
        if (unitId != null) {
            unit = em.find(OrganizationalUnit.class, unitId, LockModeType.PESSIMISTIC_WRITE);
            if (unit == null || !unit.organization.id.equals(organizationId) || !Boolean.TRUE.equals(unit.active))
                bad("Active unit in the selected organization is required.");
        }
        CompliancePolicy value = exactPolicy(organizationId, unitId);
        if (value == null) {
            value = new CompliancePolicy();
            value.organization = organization;
            value.unit = unit;
            em.persist(value);
        }
        value.expirationWarningDays = expiration;
        value.maintenanceWarningDays = maintenance;
        value.regulatoryWarningDays = regulatory;
        value.inspectionIntervalDays = inspection;
        value.active = request.active() == null || request.active();
        em.flush();
        return view(value);
    }

    private ItemComplianceView assetView(AssetItem asset, PolicyView policy, List<AlertView> alerts) {
        List<String> reasons = new ArrayList<>();
        long before = alerts.size();
        LocalDate today = LocalDate.now();

        if (Set.of(AssetStatus.BLOCKED.name(), AssetStatus.MISSING.name(), AssetStatus.RESTRICTED.name()).contains(asset.status))
            reasons.add("STATUS_" + asset.status);
        evaluateDate(alerts, reasons, "ASSET_EXPIRATION", "inventory/assets", asset.id, asset.assetCode,
            asset.validUntil, policy.expirationWarningDays(), "Asset validity", true);

        for (ExpirationRecord record : expirationRecords(asset.id, null)) {
            evaluateDate(alerts, reasons, "EXPIRATION_" + safe(record.type), "expiration-records", record.id, asset.assetCode,
                record.expirationDate, policy.expirationWarningDays(), "Expiration control " + record.type, true);
        }

        for (RegulatoryControl control : regulatoryControls(asset.id)) {
            if (!Set.of("ACTIVE", "VALID", "COMPLIANT").contains(safe(control.status))) {
                reasons.add("REGULATORY_" + safe(control.status));
                alerts.add(new AlertView("CRITICAL", "REGULATORY_STATUS", "regulatory-controls", control.id, asset.assetCode,
                    "Regulatory control " + control.externalSystem + " is " + control.status + ".", date(control.validUntil), null));
            }
            evaluateDate(alerts, reasons, "REGULATORY_EXPIRATION", "regulatory-controls", control.id, asset.assetCode,
                control.validUntil, policy.regulatoryWarningDays(), "Regulatory control " + control.externalSystem, true);
        }

        List<RecallItem> recalls = activeRecalls(asset.id, null);
        if (!recalls.isEmpty()) {
            reasons.add("ACTIVE_RECALL");
            for (RecallItem item : recalls) alerts.add(new AlertView("CRITICAL", "ACTIVE_RECALL", "recalls", item.recall.id,
                asset.assetCode, "Active recall " + item.recall.number + ": " + item.recall.reason, null, null));
        }

        WorkOrder maintenance = latestMaintenance(asset.id);
        String nextMaintenance = maintenance == null || maintenance.nextMaintenanceAt == null ? null : maintenance.nextMaintenanceAt.toString();
        if (maintenance != null && maintenance.nextMaintenanceAt != null) {
            evaluateDate(alerts, reasons, "MAINTENANCE_DUE", "maintenance", maintenance.id, asset.assetCode,
                maintenance.nextMaintenanceAt.toLocalDate(), policy.maintenanceWarningDays(), "Preventive maintenance", true);
        }

        PeriodicInspection inspection = latestInspection(asset.id);
        String inspectionAt = inspection == null ? null : inspection.inspectedAt.toString();
        String inspectionResult = inspection == null ? null : inspection.result;
        if (inspection == null) {
            reasons.add("NO_INSPECTION_HISTORY");
            alerts.add(new AlertView("WARNING", "INSPECTION_REQUIRED", "periodic-inspections", null, asset.assetCode,
                "Asset has no periodic inspection history.", null, null));
        } else {
            LocalDate due = inspection.inspectedAt.toLocalDate().plusDays(policy.inspectionIntervalDays());
            long age = ChronoUnit.DAYS.between(inspection.inspectedAt.toLocalDate(), today);
            if (age >= policy.inspectionIntervalDays()) {
                reasons.add("INSPECTION_OVERDUE");
                alerts.add(dateAlert("INSPECTION_OVERDUE", "periodic-inspections", inspection.id, asset.assetCode,
                    due, policy.expirationWarningDays(), "Periodic inspection is overdue."));
            } else {
                long remaining = ChronoUnit.DAYS.between(today, due);
                int window = Math.min(policy.expirationWarningDays(), policy.inspectionIntervalDays());
                if (remaining <= window) alerts.add(dateAlert("INSPECTION_DUE", "periodic-inspections", inspection.id,
                    asset.assetCode, due, window, "Periodic inspection is approaching."));
            }
            if (Set.of("FAILED", "REPROVED", "MAINTENANCE_REQUIRED").contains(safe(inspection.result))
                    && !correctedAfter(asset.id, inspection.inspectedAt)) {
                reasons.add("INSPECTION_NONCONFORMITY");
            }
        }

        long activeAlerts = alerts.size() - before;
        return new ItemComplianceView("ASSET", asset.id, asset.assetCode, asset.model.name, asset.status,
            reasons.isEmpty(), List.copyOf(new HashSet<>(reasons)), date(asset.validUntil), nextMaintenance,
            inspectionAt, inspectionResult, recalls.size(), activeAlerts);
    }

    private ItemComplianceView lotView(StockLot lot, PolicyView policy, List<AlertView> alerts) {
        List<String> reasons = new ArrayList<>();
        long before = alerts.size();
        if (Set.of("BLOCKED", "RESTRICTED").contains(safe(lot.status))) reasons.add("STATUS_" + safe(lot.status));
        evaluateDate(alerts, reasons, "LOT_EXPIRATION", "inventory/lots", lot.id, lot.lotNumber,
            lot.validUntil, policy.expirationWarningDays(), "Lot validity", true);
        for (ExpirationRecord record : expirationRecords(null, lot.id)) {
            evaluateDate(alerts, reasons, "EXPIRATION_" + safe(record.type), "expiration-records", record.id, lot.lotNumber,
                record.expirationDate, policy.expirationWarningDays(), "Expiration control " + record.type, true);
        }
        List<RecallItem> recalls = activeRecalls(null, lot.id);
        if (!recalls.isEmpty()) {
            reasons.add("ACTIVE_RECALL");
            for (RecallItem item : recalls) alerts.add(new AlertView("CRITICAL", "ACTIVE_RECALL", "recalls", item.recall.id,
                lot.lotNumber, "Active recall " + item.recall.number + ": " + item.recall.reason, null, null));
        }
        return new ItemComplianceView("LOT", lot.id, lot.lotNumber, lot.model.name, lot.status,
            reasons.isEmpty(), List.copyOf(new HashSet<>(reasons)), date(lot.validUntil), null, null, null,
            recalls.size(), alerts.size() - before);
    }

    private void evaluateDate(List<AlertView> alerts, List<String> reasons, String type, String resource, Long recordId,
                              String code, LocalDate due, int warningDays, String label, boolean overdueNonCompliant) {
        if (due == null) return;
        long remaining = ChronoUnit.DAYS.between(LocalDate.now(), due);
        if (remaining < 0 && overdueNonCompliant) reasons.add(type + "_EXPIRED");
        if (remaining <= warningDays) alerts.add(dateAlert(type, resource, recordId, code, due, warningDays,
            remaining < 0 ? label + " expired." : label + " is approaching."));
    }

    private AlertView dateAlert(String type, String resource, Long recordId, String code, LocalDate due,
                                int warningDays, String message) {
        long remaining = ChronoUnit.DAYS.between(LocalDate.now(), due);
        String severity = remaining < 0 || remaining <= Math.min(7, warningDays) ? "CRITICAL" : "WARNING";
        return new AlertView(severity, type, resource, recordId, code, message, due.toString(), Math.toIntExact(remaining));
    }

    private List<AssetItem> assets(long org, Long unit) {
        String jpql = "select a from AssetItem a where a.location.organization.id=:org" +
            (unit == null ? "" : " and a.location.unit.id=:unit");
        var q = em.createQuery(jpql, AssetItem.class).setParameter("org", org);
        if (unit != null) q.setParameter("unit", unit);
        return q.getResultList();
    }

    private List<StockLot> lots(long org, Long unit) {
        String jpql = "select distinct b.lot from StockBalance b where b.location.organization.id=:org" +
            (unit == null ? "" : " and b.location.unit.id=:unit");
        var q = em.createQuery(jpql, StockLot.class).setParameter("org", org);
        if (unit != null) q.setParameter("unit", unit);
        return q.getResultList();
    }

    private List<ExpirationRecord> expirationRecords(Long assetId, Long lotId) {
        if (assetId != null) return em.createQuery("select e from ExpirationRecord e where e.asset.id=:id", ExpirationRecord.class)
            .setParameter("id", assetId).getResultList();
        return em.createQuery("select e from ExpirationRecord e where e.lot.id=:id", ExpirationRecord.class)
            .setParameter("id", lotId).getResultList();
    }

    private List<RegulatoryControl> regulatoryControls(long assetId) {
        return em.createQuery("select r from RegulatoryControl r where r.asset.id=:id", RegulatoryControl.class)
            .setParameter("id", assetId).getResultList();
    }

    private List<RecallItem> activeRecalls(Long assetId, Long lotId) {
        String jpql = assetId != null
            ? "select i from RecallItem i where i.asset.id=:id and upper(i.recall.status) not in ('CLOSED','RESOLVED','COMPLETED','CANCELLED')"
            : "select i from RecallItem i where i.lot.id=:id and upper(i.recall.status) not in ('CLOSED','RESOLVED','COMPLETED','CANCELLED')";
        return em.createQuery(jpql, RecallItem.class).setParameter("id", assetId != null ? assetId : lotId).getResultList();
    }

    private WorkOrder latestMaintenance(long assetId) {
        return em.createQuery("select w from WorkOrder w where w.asset.id=:id and w.nextMaintenanceAt is not null order by w.completedAt desc,w.id desc", WorkOrder.class)
            .setParameter("id", assetId).setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private boolean correctedAfter(long assetId, LocalDateTime inspectionAt) {
        Long count = em.createQuery("select count(w) from WorkOrder w where w.asset.id=:id and w.status='COMPLETED' and w.completedAt>:at", Long.class)
            .setParameter("id", assetId).setParameter("at", inspectionAt).getSingleResult();
        return count > 0;
    }

    private PeriodicInspection latestInspection(long assetId) {
        return em.createQuery("select i from PeriodicInspection i where i.asset.id=:id order by i.inspectedAt desc,i.id desc", PeriodicInspection.class)
            .setParameter("id", assetId).setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private CompliancePolicy findPolicy(long org, Long unit) {
        CompliancePolicy exact = exactPolicy(org, unit);
        if (exact != null && Boolean.TRUE.equals(exact.active)) return exact;
        if (unit != null) {
            CompliancePolicy fallback = exactPolicy(org, null);
            if (fallback != null && Boolean.TRUE.equals(fallback.active)) return fallback;
        }
        return null;
    }

    private CompliancePolicy exactPolicy(long org, Long unit) {
        String jpql = "select p from CompliancePolicy p where p.organization.id=:org and " +
            (unit == null ? "p.unit is null" : "p.unit.id=:unit");
        var q = em.createQuery(jpql, CompliancePolicy.class).setParameter("org", org);
        if (unit != null) q.setParameter("unit", unit);
        return q.setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private static PolicyView view(CompliancePolicy p) {
        return new PolicyView(p.id, p.organization.id, p.unit == null ? null : p.unit.id, p.expirationWarningDays,
            p.maintenanceWarningDays, p.regulatoryWarningDays, p.inspectionIntervalDays, p.active);
    }

    private static int days(Integer value, int fallback, String label, int min, int max) {
        int result = value == null ? fallback : value;
        if (result < min || result > max) bad(label + " must be between " + min + " and " + max + ".");
        return result;
    }

    private static int severityRank(String value) { return "CRITICAL".equals(value) ? 0 : "WARNING".equals(value) ? 1 : 2; }
    private static String date(LocalDate value) { return value == null ? null : value.toString(); }
    private static String safe(String value) { return value == null ? "" : value.trim().toUpperCase(); }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
