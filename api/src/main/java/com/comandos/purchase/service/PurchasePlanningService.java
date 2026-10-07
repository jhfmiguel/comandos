package com.comandos.purchase.service;

import com.comandos.core.service.ProductCanonicalScopeResolver;
import com.comandos.core.service.CanonicalMasterDataDirectory;
import com.comandos.purchase.dto.PurchasePlanningContract.CreateRequest;
import com.comandos.purchase.dto.PurchasePlanningContract.StatusRequest;
import com.comandos.purchase.dto.PurchasePlanningContract.View;
import com.comandos.purchase.model.PurchasePlanning;
import com.comandos.purchase.model.PurchasePlanningStatus;
import com.comandos.purchase.repository.PurchasePlanningRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.tenancy.api.TenantId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PurchasePlanningService {
    private static final TenantId TENANT = TenantId.of("comandos");
    private final PurchasePlanningRepository repository;
    private final ProductCanonicalScopeResolver canonicalScope;
    private final CanonicalMasterDataDirectory masterData;

    public PurchasePlanningService(
            PurchasePlanningRepository repository,
            ProductCanonicalScopeResolver canonicalScope,
            CanonicalMasterDataDirectory masterData) {
        this.repository = repository;
        this.canonicalScope = canonicalScope;
        this.masterData = masterData;
    }

    @Transactional
    public View create(CreateRequest request) {
        if (request == null || request.organizationId() == null) {
            throw new IllegalArgumentException("Organization is required.");
        }
        if (blank(request.objectDescription()) || blank(request.needJustification())) {
            throw new IllegalArgumentException("Object description and need justification are required.");
        }

        long organizationId = request.organizationId();
        var organization = masterData.findOrganization(organizationId, TENANT)
            .orElseThrow(() -> new EntityNotFoundException(
                "Organization not found: " + organizationId
            ));
        if (organization.status() != LifecycleStatus.ACTIVE) {
            throw new IllegalArgumentException("Organization must be active.");
        }

        PurchasePlanning planning = new PurchasePlanning();
        planning.organizationLegacyId = organizationId;
        planning.organizationCanonicalId = canonicalScope.organization(organizationId);
        planning.processNumber = clean(request.processNumber());
        planning.requestingUnit = clean(request.requestingUnit());
        planning.objectDescription = required(request.objectDescription(), 2000, "Object description");
        planning.needJustification = required(request.needJustification(), 4000, "Need justification");
        planning.estimatedValue = request.estimatedValue();
        planning.expectedContractDate = request.expectedContractDate();
        planning.budgetSource = clean(request.budgetSource());
        planning.budgetAvailable = request.budgetAvailable();
        planning.technicalPreliminaryStudyRequired = defaultTrue(request.technicalPreliminaryStudyRequired());
        planning.termsOfReferenceRequired = defaultTrue(request.termsOfReferenceRequired());
        planning.riskAnalysisRequired = defaultTrue(request.riskAnalysisRequired());
        planning.priceResearchRequired = defaultTrue(request.priceResearchRequired());
        planning.notes = clean(request.notes());
        planning.status = PurchasePlanningStatus.DRAFT;

        return view(repository.save(planning));
    }

    @Transactional
    public View updateStatus(Long id, StatusRequest request) {
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("Planning status is required.");
        }
        PurchasePlanning planning = find(id);
        planning.status = request.status();
        return view(repository.save(planning));
    }

    public View get(Long id) {
        return view(find(id));
    }

    public List<View> list(Long organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization is required.");
        }
        var rows = canonicalScope.enabled()
            ? repository.findByOrganizationCanonicalIdOrderByCreatedAtDesc(
                canonicalScope.organization(organizationId)
            )
            : repository.findByOrganizationLegacyIdOrderByCreatedAtDesc(organizationId);

        return rows.stream()
            .map(this::view)
            .toList();
    }

    private PurchasePlanning find(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase planning not found: " + id));
    }

    private View view(PurchasePlanning p) {
        return new View(
            p.id,
            p.organizationLegacyId,
            p.processNumber,
            p.requestingUnit,
            p.objectDescription,
            p.needJustification,
            p.estimatedValue,
            p.expectedContractDate,
            p.budgetSource,
            p.budgetAvailable,
            p.technicalPreliminaryStudyRequired,
            p.termsOfReferenceRequired,
            p.riskAnalysisRequired,
            p.priceResearchRequired,
            p.status,
            p.notes,
            p.organizationCanonicalId
        );
    }

    private static Boolean defaultTrue(Boolean value) {
        return value == null || value;
    }

    private static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String required(String value, int maxLength, String label) {
        String cleaned = clean(value);
        if (cleaned == null) throw new IllegalArgumentException(label + " is required.");
        if (cleaned.length() > maxLength) throw new IllegalArgumentException(label + " is too long.");
        return cleaned;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
