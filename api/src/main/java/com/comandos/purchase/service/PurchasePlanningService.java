package com.comandos.purchase.service;

import com.comandos.core.model.Organization;
import com.comandos.core.service.ProductMasterDataReferenceSynchronizer;
import com.comandos.core.service.ProductCanonicalScopeResolver;
import com.comandos.purchase.dto.PurchasePlanningContract.CreateRequest;
import com.comandos.purchase.dto.PurchasePlanningContract.StatusRequest;
import com.comandos.purchase.dto.PurchasePlanningContract.View;
import com.comandos.purchase.model.PurchasePlanning;
import com.comandos.purchase.model.PurchasePlanningStatus;
import com.comandos.purchase.repository.PurchasePlanningRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PurchasePlanningService {
    private final PurchasePlanningRepository repository;
    private final EntityManager em;
    private final ProductMasterDataReferenceSynchronizer masterDataReferences;
    private final ProductCanonicalScopeResolver canonicalScope;

    public PurchasePlanningService(
            PurchasePlanningRepository repository,
            EntityManager em,
            ProductMasterDataReferenceSynchronizer masterDataReferences,
            ProductCanonicalScopeResolver canonicalScope) {
        this.repository = repository;
        this.em = em;
        this.masterDataReferences = masterDataReferences;
        this.canonicalScope = canonicalScope;
    }

    @Transactional
    public View create(CreateRequest request) {
        if (request == null || request.organizationId() == null) {
            throw new IllegalArgumentException("Organization is required.");
        }
        if (blank(request.objectDescription()) || blank(request.needJustification())) {
            throw new IllegalArgumentException("Object description and need justification are required.");
        }

        Organization organization = em.find(Organization.class, request.organizationId());
        if (organization == null) {
            throw new EntityNotFoundException("Organization not found: " + request.organizationId());
        }
        if (!Boolean.TRUE.equals(organization.active)) {
            throw new IllegalArgumentException("Organization must be active.");
        }

        PurchasePlanning planning = new PurchasePlanning();
        planning.organization = organization;
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

        if (canonicalScope.enabled()) {
            masterDataReferences.synchronizeForBackfill(planning);
        } else {
            masterDataReferences.synchronize(planning);
        }
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
            : repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);

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
            p.organization.id,
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
