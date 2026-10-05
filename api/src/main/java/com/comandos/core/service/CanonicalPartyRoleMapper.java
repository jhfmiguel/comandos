package com.comandos.core.service;

import com.comandos.core.model.PersonRole;
import com.comandos.core.model.PersonRoleAssignment;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.enterprise.party.PartyRoleAssignment;
import com.fariamiguel.enterprise.party.PartyRoleType;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps reusable COMANDOS person roles to canonical Faria Miguel party roles.
 *
 * <p>Only cross-product commercial/relationship roles are canonicalized here.
 * Product-specific public/private-security roles remain owned by COMANDOS.</p>
 */
public final class CanonicalPartyRoleMapper {

    private CanonicalPartyRoleMapper() {
    }

    public static Optional<PartyRoleType> roleType(PersonRole role) {
        if (role == null) return Optional.empty();

        String token = normalize(role.code);
        if (token == null) token = normalize(role.name);
        if (token == null) return Optional.empty();

        return switch (token) {
            case "CUSTOMER", "CLIENT", "CLIENTE" -> Optional.of(PartyRoleType.CUSTOMER);
            case "SUPPLIER", "FORNECEDOR" -> Optional.of(PartyRoleType.SUPPLIER);
            case "PARTNER", "PARCEIRO" -> Optional.of(PartyRoleType.PARTNER);
            case "SERVICE_PROVIDER", "SERVICEPROVIDER", "PRESTADOR", "PRESTADOR_DE_SERVICO",
                 "PRESTADOR_SERVICO", "PROVIDER" -> Optional.of(PartyRoleType.SERVICE_PROVIDER);
            default -> Optional.empty();
        };
    }

    public static Optional<PartyRoleAssignment> assignment(
            PersonRoleAssignment source,
            TenantId tenantId,
            Map<String, String> roleData) {

        requirePersisted(source == null ? null : source.id, "person role assignment");
        requirePersisted(source.person == null ? null : source.person.id, "person");
        requirePersisted(source.role == null ? null : source.role.id, "person role");
        requirePersisted(source.organization == null ? null : source.organization.id, "organization");
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");

        Optional<PartyRoleType> canonicalRole = roleType(source.role);
        if (canonicalRole.isEmpty()) return Optional.empty();

        Map<String, String> attributes = new LinkedHashMap<>();
        put(attributes, "legacyRoleCode", source.role.code);
        put(attributes, "legacyRoleName", source.role.name);
        put(attributes, "legacyStatus", source.status);
        put(attributes, "legacyOrganizationId", String.valueOf(source.organization.id));
        if (source.unit != null && source.unit.id != null) {
            put(attributes, "legacyUnitId", String.valueOf(source.unit.id));
        }
        if (roleData != null) {
            roleData.forEach((key, value) -> {
                String normalizedKey = blankToNull(key);
                String normalizedValue = blankToNull(value);
                if (normalizedKey != null && normalizedValue != null) {
                    attributes.put("detail." + normalizedKey, normalizedValue);
                }
            });
        }

        return Optional.of(new PartyRoleAssignment(
            BusinessId.of("comandos:person-role-assignment:" + source.id),
            tenantId,
            new PartyRef(
                BusinessId.of("comandos:person:" + source.person.id),
                PartyKind.PERSON
            ),
            canonicalRole.orElseThrow(),
            status(source.status, source.endDate),
            source.startDate,
            source.endDate,
            attributes
        ));
    }

    public static LifecycleStatus status(String legacyStatus, java.time.LocalDate endDate) {
        String token = normalize(legacyStatus);

        if (token == null) {
            return endDate == null ? LifecycleStatus.ACTIVE : LifecycleStatus.CLOSED;
        }

        return switch (token) {
            case "ACTIVE", "ATIVO", "ATIVA" -> LifecycleStatus.ACTIVE;
            case "SUSPENDED", "SUSPENSO", "SUSPENSA", "INACTIVE", "INATIVO", "INATIVA" ->
                LifecycleStatus.INACTIVE;
            case "ENDED", "ENCERRADO", "ENCERRADA", "CLOSED", "FINALIZADO", "FINALIZADA" ->
                LifecycleStatus.CLOSED;
            case "CANCELLED", "CANCELED", "CANCELADO", "CANCELADA" ->
                LifecycleStatus.CANCELLED;
            default -> LifecycleStatus.ACTIVE;
        };
    }

    private static String normalize(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) return null;

        return java.text.Normalizer
            .normalize(normalized, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
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
