package com.comandos.compliance.service;
import com.fariamiguel.app.compliance.service.ComplianceService;

import com.fariamiguel.app.compliance.dto.ComplianceContract.*;
import com.fariamiguel.app.compliance.model.CompliancePolicy;
import com.fariamiguel.app.core.service.CanonicalMasterDataDirectory;
import com.fariamiguel.app.inventory.model.*;
import com.comandos.inventory.model.*;
import com.fariamiguel.app.core.service.ProductCanonicalScopeResolver;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;
import com.fariamiguel.app.lifecycle.model.PeriodicInspection;
import com.fariamiguel.app.maintenance.model.WorkOrder;
import com.comandos.security.service.ComandosAccessPolicy;
import jakarta.persistence.EntityManager;
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
@org.springframework.context.annotation.Primary
@Transactional(readOnly = true)
public class ComandosComplianceService extends ComplianceService {
    private static final TenantId TENANT = TenantId.of("comandos");
    private final ProductCanonicalScopeResolver canonicalScope;
    private final CanonicalMasterDataDirectory masterData;

    public ComandosComplianceService(EntityManager em, ComandosAccessPolicy access,
            ProductCanonicalScopeResolver canonicalScope, CanonicalMasterDataDirectory masterData) {
        super(em, access);
        this.canonicalScope = canonicalScope;
        this.masterData = masterData;
    }

    @Override
    protected void validatePolicyScope(long organizationId, Long unitId) {
        var organizationView = masterData.findOrganization(organizationId, TENANT)
            .filter(value -> value.status() == LifecycleStatus.ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Active organization is required."
            ));

        if (unitId != null) {
            CompanyId companyId = CompanyId.of("comandos:organization:" + organizationId);
            boolean activeUnitInOrganization = masterData
                .listUnits(organizationId, TENANT, companyId)
                .stream()
                .anyMatch(value -> CanonicalMasterDataDirectory.legacyUnitId(value.id()) == unitId);
            if (!activeUnitInOrganization) {
                bad("Active unit in the selected organization is required.");
            }
        }

    }

    @Override
    protected void assessRegulatoryControls(AssetItem asset, PolicyView policy, List<AlertView> alerts, List<String> reasons) {
        for (RegulatoryControl control : regulatoryControls(asset.id)) {
            if (!Set.of("ACTIVE", "VALID", "COMPLIANT").contains(safe(control.status))) {
                reasons.add("REGULATORY_" + safe(control.status));
                alerts.add(new AlertView("CRITICAL", "REGULATORY_STATUS", "regulatory-controls", control.id, asset.assetCode,
                    "Regulatory control " + control.externalSystem + " is " + control.status + ".", date(control.validUntil), null));
            }
            evaluateDate(alerts, reasons, "REGULATORY_EXPIRATION", "regulatory-controls", control.id, asset.assetCode,
                control.validUntil, policy.regulatoryWarningDays(), "Regulatory control " + control.externalSystem, true);
        }

    }
    @Override
    protected List<AssetItem> assets(long org, Long unit) {
        boolean canonical = canonicalScope.enabled();
        var scope = canonical ? canonicalScope.scope(org, unit) : null;
        String organizationField = canonical
            ? "a.location.organizationCanonicalId"
            : "a.location.organizationLegacyId";
        String unitField = canonical
            ? "a.location.unitCanonicalId"
            : "a.location.unitLegacyId";
        String jpql = "select a from AssetItem a where " + organizationField + "=:org"
            + (unit == null ? "" : " and " + unitField + "=:unit");
        var q = em.createQuery(jpql, AssetItem.class)
            .setParameter("org", canonical ? scope.organizationId() : org);
        if (unit != null) {
            q.setParameter("unit", canonical ? scope.unitId() : unit);
        }
        return q.getResultList();
    }

    @Override
    protected List<StockLot> lots(long org, Long unit) {
        boolean canonical = canonicalScope.enabled();
        var scope = canonical ? canonicalScope.scope(org, unit) : null;
        String organizationField = canonical
            ? "b.location.organizationCanonicalId"
            : "b.location.organizationLegacyId";
        String unitField = canonical
            ? "b.location.unitCanonicalId"
            : "b.location.unitLegacyId";
        String jpql = "select distinct b.lot from StockBalance b where " + organizationField + "=:org"
            + (unit == null ? "" : " and " + unitField + "=:unit");
        var q = em.createQuery(jpql, StockLot.class)
            .setParameter("org", canonical ? scope.organizationId() : org);
        if (unit != null) {
            q.setParameter("unit", canonical ? scope.unitId() : unit);
        }
        return q.getResultList();
    }

    private List<RegulatoryControl> regulatoryControls(long assetId) {
        return em.createQuery("select r from RegulatoryControl r where r.asset.id=:id", RegulatoryControl.class)
            .setParameter("id", assetId).getResultList();
    }

}