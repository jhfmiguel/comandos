package com.comandos.demo;

import com.comandos.compliance.model.CompliancePolicy;
import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.AssetStatus;
import com.comandos.inventory.model.ExpirationRecord;
import com.comandos.inventory.model.RecallItem;
import com.comandos.inventory.model.RegulatoryControl;
import com.comandos.inventory.model.StockLot;
import com.comandos.lifecycle.model.PeriodicInspection;
import com.comandos.maintenance.model.WorkOrder;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1995)
public class ComplianceLifecycleDemoVerifier implements ApplicationRunner {
    private final EntityManager em;

    public ComplianceLifecycleDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        verifyPolicies();
        verifyAssetAndLotValidity();
        verifyExpirationRecords();
        verifyRegulatoryControls();
        verifyRecalls();
        verifyMaintenanceDates();
        verifyInspectionHistory();
    }

    private void verifyPolicies() {
        var policies = em.createQuery("select p from CompliancePolicy p", CompliancePolicy.class).getResultList();
        require(!policies.isEmpty(), "Compliance regression requires a preventive policy.");
        for (CompliancePolicy policy : policies) {
            require(policy.organization != null, "Compliance policy requires organization scope.");
            require(policy.expirationWarningDays != null && policy.expirationWarningDays >= 0, "Expiration warning window must be non-negative.");
            require(policy.maintenanceWarningDays != null && policy.maintenanceWarningDays >= 0, "Maintenance warning window must be non-negative.");
            require(policy.regulatoryWarningDays != null && policy.regulatoryWarningDays >= 0, "Regulatory warning window must be non-negative.");
            require(policy.inspectionIntervalDays != null && policy.inspectionIntervalDays > 0, "Inspection interval must be positive.");
            if (policy.unit != null) require(policy.unit.organization.id.equals(policy.organization.id), "Compliance policy unit must belong to its organization.");
        }
    }

    private void verifyAssetAndLotValidity() {
        LocalDate today = LocalDate.now();
        for (AssetItem asset : em.createQuery("select a from AssetItem a", AssetItem.class).getResultList()) {
            if (asset.validUntil != null && asset.validUntil.isBefore(today) && !AssetStatus.terminalCodes().contains(asset.status)) {
                require(!AssetStatus.AVAILABLE.name().equals(asset.status), "Expired asset cannot remain available.");
            }
        }
        for (StockLot lot : em.createQuery("select l from StockLot l", StockLot.class).getResultList()) {
            if (lot.validUntil != null && lot.validUntil.isBefore(today) && lot.availableQuantity != null && lot.availableQuantity.signum() > 0) {
                require(!AssetStatus.AVAILABLE.name().equals(lot.status), "Expired lot with remaining stock cannot remain available.");
            }
        }
    }

    private void verifyExpirationRecords() {
        LocalDate today = LocalDate.now();
        for (ExpirationRecord record : em.createQuery("select e from ExpirationRecord e", ExpirationRecord.class).getResultList()) {
            require(record.organization != null && record.expirationDate != null && notBlank(record.type) && notBlank(record.status),
                "Expiration record must preserve scope, type, date and status.");
            require((record.asset == null) != (record.lot == null), "Expiration record must reference exactly one asset or lot.");
            if (record.expirationDate.isBefore(today)) {
                if (record.asset != null && !AssetStatus.terminalCodes().contains(record.asset.status))
                    require(!AssetStatus.AVAILABLE.name().equals(record.asset.status), "Expired controlled asset cannot remain available.");
                if (record.lot != null && record.lot.availableQuantity.signum() > 0)
                    require(!AssetStatus.AVAILABLE.name().equals(record.lot.status), "Expired controlled lot cannot remain available.");
            }
        }
    }

    private void verifyRegulatoryControls() {
        LocalDate today = LocalDate.now();
        for (RegulatoryControl control : em.createQuery("select r from RegulatoryControl r", RegulatoryControl.class).getResultList()) {
            require(control.asset != null && notBlank(control.externalSystem) && notBlank(control.registrationNumber) && notBlank(control.status),
                "Regulatory control must preserve asset, system, registration and status.");
            if (control.validUntil != null && control.validUntil.isBefore(today) && !AssetStatus.terminalCodes().contains(control.asset.status)) {
                require(!AssetStatus.AVAILABLE.name().equals(control.asset.status) || !Set.of("ACTIVE", "VALID", "COMPLIANT").contains(control.status.toUpperCase()),
                    "Expired regulatory control cannot present an unrestricted available asset as compliant.");
            }
        }
    }

    private void verifyRecalls() {
        for (RecallItem item : em.createQuery("select i from RecallItem i", RecallItem.class).getResultList()) {
            require(item.recall != null && notBlank(item.recall.number) && notBlank(item.recall.reason) && notBlank(item.recall.status),
                "Recall item must preserve recall identity and status.");
            require((item.asset == null) != (item.lot == null), "Recall item must reference exactly one asset or lot.");
            require(notBlank(item.action), "Recall item requires an action.");
            boolean active = !Set.of("CLOSED", "RESOLVED", "COMPLETED", "CANCELLED").contains(item.recall.status.toUpperCase());
            if (active && item.asset != null && !AssetStatus.terminalCodes().contains(item.asset.status))
                require(!AssetStatus.AVAILABLE.name().equals(item.asset.status), "Asset in active recall cannot remain available.");
            if (active && item.lot != null && item.lot.availableQuantity.signum() > 0)
                require(!AssetStatus.AVAILABLE.name().equals(item.lot.status), "Lot in active recall cannot remain available.");
        }
    }

    private void verifyMaintenanceDates() {
        for (WorkOrder order : em.createQuery("select w from WorkOrder w", WorkOrder.class).getResultList()) {
            require(order.asset != null && order.openedAt != null && notBlank(order.status), "Maintenance order must preserve asset, opening and status.");
            if (order.nextMaintenanceAt != null && order.completedAt != null)
                require(!order.nextMaintenanceAt.isBefore(order.completedAt), "Next maintenance cannot precede completed maintenance.");
            if (order.nextMaintenanceAt != null && order.nextMaintenanceAt.isBefore(LocalDateTime.now())
                    && AssetStatus.AVAILABLE.name().equals(order.asset.status)) {
                WorkOrder newer = em.createQuery("select w from WorkOrder w where w.asset.id=:asset and w.completedAt>:completed order by w.completedAt desc", WorkOrder.class)
                    .setParameter("asset", order.asset.id)
                    .setParameter("completed", order.completedAt == null ? order.openedAt : order.completedAt)
                    .setMaxResults(1).getResultStream().findFirst().orElse(null);
                require(newer != null || !"COMPLETED".equals(order.status), "Overdue preventive maintenance must not be silently presented as current compliance.");
            }
        }
    }

    private void verifyInspectionHistory() {
        for (PeriodicInspection inspection : em.createQuery("select i from PeriodicInspection i", PeriodicInspection.class).getResultList()) {
            require(inspection.asset != null && inspection.inspectedAt != null && notBlank(inspection.result), "Inspection compliance source must preserve asset, date and result.");
            if (inspection.approvedAt != null) require(!inspection.approvedAt.isBefore(inspection.inspectedAt), "Inspection approval cannot precede inspection.");
        }
    }

    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
