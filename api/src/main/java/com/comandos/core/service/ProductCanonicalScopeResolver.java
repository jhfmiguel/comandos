package com.comandos.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resolves legacy COMANDOS scope identifiers to canonical Faria Miguel IDs for
 * product-domain read paths during the canonical-id-primary cutover.
 */
@Service
public class ProductCanonicalScopeResolver {

    public record Scope(String organizationId, String unitId) {}

    private final MasterDataReferenceService references;
    private final boolean enabled;

    public ProductCanonicalScopeResolver(
            MasterDataReferenceService references,
            @Value("${comandos.master-data.product-reference-primary-read.enabled:false}")
            boolean enabled) {
        this.references = references;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public String organization(Long legacyId) {
        if (legacyId == null) {
            throw new IllegalArgumentException("Organization id is required.");
        }
        return references.resolveCanonicalId(
                MasterDataReferenceService.ORGANIZATION,
                legacyId)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical organization reference is missing for legacy id " + legacyId
            ));
    }

    public String person(Long legacyId) {
        if (legacyId == null) {
            throw new IllegalArgumentException("Person id is required.");
        }
        return references.resolveCanonicalId(
                MasterDataReferenceService.PERSON,
                legacyId)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical person reference is missing for legacy id " + legacyId
            ));
    }

    public String unit(Long legacyId) {
        if (legacyId == null) return null;
        return references.resolveCanonicalId(
                MasterDataReferenceService.UNIT,
                legacyId)
            .orElseThrow(() -> new IllegalStateException(
                "Canonical organizational unit reference is missing for legacy id " + legacyId
            ));
    }

    public Scope scope(Long organizationLegacyId, Long unitLegacyId) {
        return new Scope(
            organization(organizationLegacyId),
            unit(unitLegacyId)
        );
    }
}
