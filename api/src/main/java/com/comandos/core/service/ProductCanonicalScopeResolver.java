package com.comandos.core.service;

import com.fariamiguel.tenancy.api.TenantId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resolves legacy COMANDOS scope identifiers to canonical Faria Miguel IDs for
 * product-domain read paths during the canonical-id-primary cutover.
 */
@Service
public class ProductCanonicalScopeResolver {

    public record Scope(String organizationId, String unitId) {}

    private static final TenantId TENANT = TenantId.of("comandos");

    private final CanonicalMasterDataDirectory directory;
    private final boolean enabled;

    public ProductCanonicalScopeResolver(
            CanonicalMasterDataDirectory directory,
            @Value("${comandos.master-data.product-reference-primary-read.enabled:false}")
            boolean enabled) {
        this.directory = directory;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public String organization(Long legacyId) {
        if (legacyId == null) {
            throw new IllegalArgumentException("Organization id is required.");
        }
        return directory.findOrganization(legacyId, TENANT)
            .map(organization -> organization.id().value())
            .orElseThrow(() -> new IllegalStateException(
                "Canonical organization is missing for legacy id " + legacyId
            ));
    }

    public String person(Long legacyId) {
        if (legacyId == null) {
            throw new IllegalArgumentException("Person id is required.");
        }
        return directory.findPerson(legacyId, TENANT)
            .map(person -> person.id().value())
            .orElseThrow(() -> new IllegalStateException(
                "Canonical person is missing for legacy id " + legacyId
            ));
    }

    public String unit(Long legacyId) {
        if (legacyId == null) return null;
        return directory.findUnit(legacyId, TENANT)
            .map(unit -> unit.id().value())
            .orElseThrow(() -> new IllegalStateException(
                "Canonical organizational unit is missing for legacy id " + legacyId
            ));
    }

    /**
     * Retirement parity must never be satisfied by a synthesized legacy
     * projection when canonical reads have not yet been enabled.
     */
    public boolean persistedCanonicalExists(String resourceType, Long legacyId) {
        if (legacyId == null || legacyId <= 0) return false;
        return switch (resourceType) {
            case "ORGANIZATION" ->
                directory.containsPersistedCanonicalOrganization(legacyId, TENANT);
            case "PERSON" ->
                directory.containsPersistedCanonicalPerson(legacyId, TENANT);
            case "UNIT" ->
                directory.containsPersistedCanonicalUnit(legacyId, TENANT);
            default -> throw new IllegalArgumentException(
                "Unsupported canonical resource type: " + resourceType
            );
        };
    }

    public Scope scope(Long organizationLegacyId, Long unitLegacyId) {
        return new Scope(
            organization(organizationLegacyId),
            unit(unitLegacyId)
        );
    }
}
