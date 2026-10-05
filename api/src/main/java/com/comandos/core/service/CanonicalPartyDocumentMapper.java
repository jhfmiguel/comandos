package com.comandos.core.service;

import com.comandos.core.model.PersonCredential;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.enterprise.party.PartyDocument;
import com.fariamiguel.enterprise.party.PartyDocumentType;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.tenancy.api.TenantId;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps legacy COMANDOS person credentials to canonical Faria Miguel party
 * document metadata. Binary document storage remains handled by the shared
 * documents capability.
 */
public final class CanonicalPartyDocumentMapper {

    private CanonicalPartyDocumentMapper() {
    }

    public static PartyDocument credential(PersonCredential source, TenantId tenantId) {
        requirePersisted(source == null ? null : source.id, "person credential");
        requirePersisted(source.person == null ? null : source.person.id, "credential person");
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");

        Map<String, String> attributes = new LinkedHashMap<>();
        put(attributes, "legacyCredentialType", source.type);

        return new PartyDocument(
            BusinessId.of("comandos:person-credential:" + source.id),
            tenantId,
            new PartyRef(
                BusinessId.of("comandos:person:" + source.person.id),
                PartyKind.PERSON
            ),
            type(source.type),
            required(source.number, "credential number"),
            null,
            null,
            source.validUntil,
            null,
            LifecycleStatus.ACTIVE,
            attributes
        );
    }

    public static PartyDocumentType type(String legacyType) {
        String token = normalize(legacyType);
        if (token == null) return PartyDocumentType.OTHER;

        return switch (token) {
            case "CPF" -> PartyDocumentType.CPF;
            case "CNPJ" -> PartyDocumentType.CNPJ;
            case "RG", "IDENTIDADE", "IDENTITY" -> PartyDocumentType.RG;
            case "CNH", "DRIVER_LICENSE", "CARTEIRA_NACIONAL_DE_HABILITACAO" ->
                PartyDocumentType.CNH;
            case "PASSPORT", "PASSAPORTE" -> PartyDocumentType.PASSPORT;
            case "STATE_REGISTRATION", "INSCRICAO_ESTADUAL" ->
                PartyDocumentType.STATE_REGISTRATION;
            case "MUNICIPAL_REGISTRATION", "INSCRICAO_MUNICIPAL" ->
                PartyDocumentType.MUNICIPAL_REGISTRATION;
            case "PROFESSIONAL_REGISTRATION", "REGISTRO_PROFISSIONAL", "CONSELHO_PROFISSIONAL" ->
                PartyDocumentType.PROFESSIONAL_REGISTRATION;
            default -> PartyDocumentType.OTHER;
        };
    }

    private static String normalize(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) return null;

        return Normalizer.normalize(normalized, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
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
