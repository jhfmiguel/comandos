package com.comandos.core.service;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.Person;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.OrganizationalUnitId;
import com.fariamiguel.tenancy.api.OrganizationalUnitType;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps legacy COMANDOS master-data entities to canonical Faria Miguel domain
 * records without changing the existing Oracle schema.
 *
 * <p>This is a migration seam: persistence remains legacy until the shared
 * repositories become the source of truth, while new consumers can already
 * operate on canonical contracts.</p>
 */
public final class CanonicalMasterDataMapper {

    private CanonicalMasterDataMapper() {
    }

    public static com.fariamiguel.enterprise.people.Person person(
            Person source,
            TenantId tenantId) {

        requirePersisted(source == null ? null : source.id, "person");
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");

        Map<String, String> attributes = new LinkedHashMap<>();
        put(attributes, "legacyPersonType", source.personType);
        put(attributes, "legacyAddress", source.address);

        Set<String> emails = nonBlankSet(source.email);
        Set<String> phones = nonBlankSet(source.phone);

        return new com.fariamiguel.enterprise.people.Person(
            BusinessId.of("comandos:person:" + source.id),
            tenantId,
            source.fullName,
            source.taxId,
            source.birthDate,
            java.util.List.of(),
            emails,
            phones,
            lifecycle(source.active),
            attributes
        );
    }

    public static com.fariamiguel.enterprise.organization.Organization organization(
            Organization source,
            TenantId tenantId,
            CompanyId companyId) {

        requirePersisted(source == null ? null : source.id, "organization");
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");

        Map<String, String> attributes = new LinkedHashMap<>();
        put(
            attributes,
            "nature",
            source.nature == null ? source.legacyNature : source.nature.name
        );
        put(
            attributes,
            "economicActivity",
            source.economicActivity == null
                ? null
                : source.economicActivity.description
        );
        put(attributes, "acronym", source.acronym);
        put(attributes, "publicOrganization", String.valueOf(Boolean.TRUE.equals(source.publicOrganization)));

        return new com.fariamiguel.enterprise.organization.Organization(
            BusinessId.of("comandos:organization:" + source.id),
            tenantId,
            companyId,
            source.name,
            blankToNull(source.acronym),
            source.taxId,
            lifecycle(source.active),
            attributes
        );
    }

    public static com.fariamiguel.tenancy.api.OrganizationalUnit unit(
            OrganizationalUnit source,
            TenantId tenantId,
            CompanyId companyId) {

        requirePersisted(source == null ? null : source.id, "organizational unit");
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
        if (companyId == null) throw new IllegalArgumentException("companyId is required");

        Map<String, String> attributes = new LinkedHashMap<>();
        put(
            attributes,
            "legacyType",
            source.unitType == null ? source.type : source.unitType.name
        );
        put(attributes, "legacyOrganizationId", String.valueOf(source.organization.id));

        return new com.fariamiguel.tenancy.api.OrganizationalUnit(
            OrganizationalUnitId.of("comandos:unit:" + source.id),
            tenantId,
            companyId,
            null,
            source.parentUnit == null
                ? null
                : OrganizationalUnitId.of("comandos:unit:" + source.parentUnit.id),
            source.code,
            source.name,
            unitType(source),
            Boolean.TRUE.equals(source.active),
            attributes
        );
    }

    private static LifecycleStatus lifecycle(Boolean active) {
        return Boolean.TRUE.equals(active)
            ? LifecycleStatus.ACTIVE
            : LifecycleStatus.INACTIVE;
    }

    private static OrganizationalUnitType unitType(OrganizationalUnit source) {
        String raw = source.unitType == null ? source.type : source.unitType.code;
        if (raw == null || raw.isBlank()) return OrganizationalUnitType.OTHER;

        String normalized = raw.trim()
            .toUpperCase(java.util.Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');

        try {
            return OrganizationalUnitType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return OrganizationalUnitType.OTHER;
        }
    }

    private static Set<String> nonBlankSet(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? Set.of() : Set.of(normalized);
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void put(Map<String, String> target, String key, String value) {
        String normalized = blankToNull(value);
        if (normalized != null) target.put(key, normalized);
    }

    private static void requirePersisted(Long id, String type) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(type + " must be persisted before canonical mapping");
        }
    }
}
