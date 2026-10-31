package com.comandos.core.service;

import com.comandos.core.model.PersonQualification;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.enterprise.people.ProfessionalQualification;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.LinkedHashMap;
import java.util.Map;

/** Maps legacy COMANDOS qualifications to the canonical Faria Miguel model. */
public final class CanonicalProfessionalQualificationMapper {

    private CanonicalProfessionalQualificationMapper() {
    }

    public static ProfessionalQualification qualification(
            PersonQualification source,
            TenantId tenantId) {

        requirePersisted(source == null ? null : source.id, "person qualification");
        requirePersisted(source.person == null ? null : source.person.id, "qualification person");
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");

        Map<String, String> attributes = new LinkedHashMap<>();
        put(attributes, "legacyStatus", source.status);

        return new ProfessionalQualification(
            BusinessId.of("comandos:person-qualification:" + source.id),
            tenantId,
            new PartyRef(
                BusinessId.of("comandos:person:" + source.person.id),
                PartyKind.PERSON
            ),
            required(source.category, "qualification category"),
            source.validUntil,
            CanonicalPartyRoleMapper.status(source.status, source.validUntil),
            attributes
        );
    }

    private static String required(String value, String field) {
        String normalized = blankToNull(value);
        if (normalized == null) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static void put(Map<String, String> target, String key, String value) {
        String normalized = blankToNull(value);
        if (normalized != null) target.put(key, normalized);
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void requirePersisted(Long id, String type) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(type + " must be persisted before canonical mapping");
        }
    }
}
