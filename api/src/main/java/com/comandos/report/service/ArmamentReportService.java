package com.comandos.report.service;

import com.comandos.audit.model.AuditRecord;
import com.comandos.core.service.CanonicalMasterDataDirectory;
import com.comandos.core.service.ProductCanonicalScopeResolver;
import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.ExpirationRecord;
import com.comandos.inventory.model.StockBalance;
import com.comandos.lifecycle.model.ExceptionOccurrence;
import com.comandos.reconciliation.model.InventoryCount;
import com.comandos.report.dto.ArmamentReportContract.*;
import com.comandos.security.service.AccessPolicy;
import com.comandos.workflow.model.ApprovalWorkflow;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ArmamentReportService {

    private static final TenantId TENANT = TenantId.of("comandos");

    private final EntityManager em;
    private final AccessPolicy access;
    private final ProductCanonicalScopeResolver canonicalScope;
    private final CanonicalMasterDataDirectory masterData;

    public ArmamentReportService(
            EntityManager em,
            AccessPolicy access,
            ProductCanonicalScopeResolver canonicalScope,
            CanonicalMasterDataDirectory masterData) {
        this.em = em;
        this.access = access;
        this.canonicalScope = canonicalScope;
        this.masterData = masterData;
    }

    public Dashboard dashboard(long org, Long unit) {
        access.requireScope("inventory/assets", "READ", org, unit);

        Scope assetScope = scope("a", true, org, unit);
        Scope balanceScope = scope("b", true, org, unit);
        Scope custodyScope = scope("c", false, org, unit);
        Scope occurrenceScope = scope("x", false, org, unit);

        long total = count("select count(a) from AssetItem a where " + assetScope.clause(), assetScope);
        long available = count("select count(a) from AssetItem a where " + assetScope.clause() + " and a.status='AVAILABLE'", assetScope);
        long custody = count("select count(a) from AssetItem a where " + assetScope.clause() + " and a.status='CUSTODIED'", assetScope);
        long maintenance = count("select count(a) from AssetItem a where " + assetScope.clause() + " and a.status='IN_MAINTENANCE'", assetScope);
        long blocked = count("select count(a) from AssetItem a where " + assetScope.clause()
            + " and a.status in ('BLOCKED','MISSING','RESTRICTED')", assetScope);
        BigDecimal stock = sum("select coalesce(sum(b.available),0) from StockBalance b where " + balanceScope.clause(), balanceScope);

        long overdue = count(
            "select count(c) from Custody c where " + custodyScope.clause()
                + " and c.status='ACTIVE' and c.dueAt is not null and c.dueAt<:now",
            custodyScope,
            "now",
            LocalDateTime.now());

        long occurrences = count(
            "select count(x) from ExceptionOccurrence x where " + occurrenceScope.clause() + " and x.status='OPEN'",
            occurrenceScope);

        long expired = count(
            "select count(x) from ExpirationRecord x where " + occurrenceScope.clause()
                + " and x.expirationDate<:today and x.status<>'RESOLVED'",
            occurrenceScope,
            "today",
            LocalDate.now());

        long workflows = count(
            "select count(x) from ApprovalWorkflow x where " + occurrenceScope.clause()
                + " and x.status not in ('CONCLUDED','CANCELLED')",
            occurrenceScope);

        long divergences = count(
            "select count(x) from InventoryCount x where " + occurrenceScope.clause()
                + " and x.status.code='APPROVED'"
                + " and exists(select i.id from InventoryCountItem i where i.inventoryCount=x and i.result.code<>'MATCH')",
            occurrenceScope);

        return new Dashboard(
            total,
            available,
            custody,
            maintenance,
            blocked,
            stock,
            overdue,
            occurrences,
            expired,
            workflows,
            divergences
        );
    }

    public List<Alert> alerts(long org, Long unit) {
        var d = dashboard(org, unit);
        List<Alert> alerts = new ArrayList<>();
        if (d.overdueCustodies() > 0) {
            alerts.add(new Alert("HIGH", "OVERDUE_CUSTODY", d.overdueCustodies() + " overdue custody records", "custody", null));
        }
        if (d.openOccurrences() > 0) {
            alerts.add(new Alert("HIGH", "OPEN_OCCURRENCE", d.openOccurrences() + " exceptional occurrences require attention", "occurrences", null));
        }
        if (d.expiredCompliance() > 0) {
            alerts.add(new Alert("HIGH", "EXPIRED_COMPLIANCE", d.expiredCompliance() + " compliance records are expired", "compliance", null));
        }
        if (d.inventoryDivergences() > 0) {
            alerts.add(new Alert("MEDIUM", "INVENTORY_DIVERGENCE", d.inventoryDivergences() + " inventory counts contain divergences", "inventory", null));
        }
        if (d.pendingWorkflows() > 0) {
            alerts.add(new Alert("MEDIUM", "PENDING_APPROVAL", d.pendingWorkflows() + " workflows await action", "workflow", null));
        }
        return alerts;
    }

    public List<Position> assets(long org, Long unit, String search) {
        access.requireScope("inventory/assets", "READ", org, unit);
        Scope scope = scope("a", true, org, unit);
        String searchClause = search == null || search.isBlank()
            ? ""
            : " and (lower(a.assetCode) like :q or lower(coalesce(a.serialNumber,'')) like :q or lower(a.model.name) like :q)";

        var query = em.createQuery(
            "select a from AssetItem a where " + scope.clause() + searchClause + " order by a.id desc",
            AssetItem.class);
        bindScope(query, scope);
        if (!searchClause.isEmpty()) {
            query.setParameter("q", "%" + search.trim().toLowerCase(Locale.ROOT) + "%");
        }

        return query.setMaxResults(200).getResultList().stream()
            .map(asset -> new Position(
                asset.id,
                asset.assetCode,
                asset.serialNumber,
                asset.model.name,
                asset.status,
                asset.condition,
                asset.location.name,
                unitName(asset.location.unitLegacyId)))
            .toList();
    }

    public List<LotPosition> lots(long org, Long unit) {
        access.requireScope("inventory/assets", "READ", org, unit);
        Scope scope = scope("b", true, org, unit);
        var query = em.createQuery(
            "select b from StockBalance b where " + scope.clause() + " order by b.id desc",
            StockBalance.class);
        bindScope(query, scope);
        return query.setMaxResults(200).getResultList().stream()
            .map(balance -> new LotPosition(
                balance.lot.id,
                balance.lot.lotNumber,
                balance.lot.model.name,
                balance.location.name,
                balance.available,
                balance.reserved,
                balance.blocked,
                balance.lot.validUntil == null ? null : balance.lot.validUntil.toString()))
            .toList();
    }

    public List<History> history(String resource, Long recordId) {
        return history(resource, recordId, null, null);
    }

    private List<History> history(String resource, Long recordId, Long org, Long unit) {
        access.requireAny("audit", "READ");
        String where = " where 1=1"
            + (resource == null || resource.isBlank() ? "" : " and a.resource=:r")
            + (recordId == null ? "" : " and a.recordId=:id");
        if (org != null) {
            where += " and exists(select r.id from AuditReference r where r.eventId=a.id and r.kind='organization' and r.targetId=:org)";
        }
        if (unit != null) {
            where += " and exists(select r.id from AuditReference r where r.eventId=a.id and r.kind='unit' and r.targetId=:unit)";
        }

        var query = em.createQuery("select a from AuditRecord a" + where + " order by a.id desc", AuditRecord.class);
        if (resource != null && !resource.isBlank()) {
            query.setParameter("r", resource);
        }
        if (recordId != null) {
            query.setParameter("id", recordId);
        }
        if (org != null) {
            query.setParameter("org", org);
        }
        if (unit != null) {
            query.setParameter("unit", unit);
        }

        return query.setMaxResults(500).getResultList().stream()
            .map(a -> new History("AUDIT", a.id, a.occurredAt.toString(), a.action, a.resource + "#" + a.recordId, a.actorLogin))
            .toList();
    }

    public Bundle bundle(long org, Long unit) {
        return new Bundle(
            dashboard(org, unit),
            alerts(org, unit),
            assets(org, unit, null),
            lots(org, unit),
            history(null, null, org, unit).stream().limit(50).toList()
        );
    }

    private Scope scope(String alias, boolean throughLocation, long org, Long unit) {
        boolean canonical = canonicalScope.enabled();
        var resolved = canonical ? canonicalScope.scope(org, unit) : null;
        String prefix = alias + (throughLocation ? ".location." : ".");
        String organizationField = prefix + (canonical ? "organizationCanonicalId" : "organizationLegacyId");
        String unitField = prefix + (canonical ? "unitCanonicalId" : "unitLegacyId");
        String clause = organizationField + "=:o"
            + (unit == null ? "" : " and " + unitField + "=:u");
        return new Scope(
            clause,
            canonical ? resolved.organizationId() : org,
            unit == null ? null : (canonical ? resolved.unitId() : unit)
        );
    }

    private long count(String jpql, Scope scope, Object... parameters) {
        var query = em.createQuery(jpql, Long.class);
        bindScope(query, scope);
        for (int i = 0; i < parameters.length; i += 2) {
            query.setParameter((String) parameters[i], parameters[i + 1]);
        }
        return query.getSingleResult();
    }

    private BigDecimal sum(String jpql, Scope scope) {
        var query = em.createQuery(jpql, BigDecimal.class);
        bindScope(query, scope);
        return query.getSingleResult();
    }

    private void bindScope(jakarta.persistence.Query query, Scope scope) {
        query.setParameter("o", scope.organization());
        if (scope.unit() != null) {
            query.setParameter("u", scope.unit());
        }
    }

    private String unitName(Long legacyUnitId) {
        if (legacyUnitId == null) {
            return null;
        }
        return masterData.findUnit(legacyUnitId, TENANT)
            .map(com.fariamiguel.tenancy.api.OrganizationalUnit::name)
            .orElse(null);
    }

    private record Scope(String clause, Object organization, Object unit) {}
}
