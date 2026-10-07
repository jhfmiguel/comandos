package com.comandos.core.service;

import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.core.model.RoleData;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.enterprise.party.PartyRoleAssignment;
import com.fariamiguel.enterprise.party.PartyRoleRepository;
import com.fariamiguel.enterprise.party.PartyRoleType;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Canonical directory for reusable party roles.
 *
 * <p>Generic roles read directly from Faria Miguel after canonical cutover.
 * Product-specific role extensions remain COMANDOS-owned.</p>
 */
@Service
@Transactional(readOnly = true)
public class CanonicalPartyRoleDirectory {

    public record ProductSpecificRole(
        long assignmentId,
        long personId,
        String code,
        String name,
        String status,
        long organizationId,
        Long unitId,
        java.time.LocalDate validFrom,
        java.time.LocalDate validUntil,
        Map<String, String> attributes
    ) {}

    private final EntityManager entityManager;
    private final PartyRoleRepository canonicalRoles;
    private final boolean canonicalReadEnabled;

    public CanonicalPartyRoleDirectory(
            EntityManager entityManager,
            PartyRoleRepository canonicalRoles,
            @Value("${comandos.master-data.canonical-read.enabled:false}")
            boolean canonicalReadEnabled) {
        this.entityManager = entityManager;
        this.canonicalRoles = canonicalRoles;
        this.canonicalReadEnabled = canonicalReadEnabled;
    }

    public List<PartyRoleAssignment> roles(long personId, TenantId tenantId) {
        if (personId <= 0) return List.of();

        if (canonicalReadEnabled) {
            return canonicalRoles.findByParty(tenantId, personRef(personId));
        }

        return assignments(personId).stream()
            .map(source -> CanonicalPartyRoleMapper.assignment(
                source,
                tenantId,
                roleData(source.id)
            ))
            .flatMap(java.util.Optional::stream)
            .toList();
    }

    public List<PartyRoleAssignment> roles(
            long personId,
            TenantId tenantId,
            PartyRoleType role) {

        if (role == null) return roles(personId, tenantId);

        if (canonicalReadEnabled) {
            return canonicalRoles.findByRole(tenantId, role).stream()
                .filter(assignment -> assignment.party().equals(personRef(personId)))
                .toList();
        }

        return roles(personId, tenantId).stream()
            .filter(assignment -> assignment.role() == role)
            .toList();
    }

    public List<ProductSpecificRole> productSpecificRoles(long personId) {
        if (personId <= 0) return List.of();

        return assignments(personId).stream()
            .filter(source -> CanonicalPartyRoleMapper.roleType(source.role).isEmpty())
            .map(source -> new ProductSpecificRole(
                source.id,
                source.person.id,
                source.role.code,
                source.role.name,
                source.status,
                source.organization.id,
                source.unit == null ? null : source.unit.id,
                source.startDate,
                source.endDate,
                roleData(source.id)
            ))
            .toList();
    }

    public boolean hasRole(long personId, TenantId tenantId, PartyRoleType role) {
        if (role == null) return false;

        return roles(personId, tenantId, role).stream()
            .anyMatch(assignment ->
                assignment.status()
                    == com.fariamiguel.enterprise.common.LifecycleStatus.ACTIVE
            );
    }

    private List<PersonRoleAssignment> assignments(long personId) {
        return entityManager.createQuery(
                "select a from PersonRoleAssignment a "
                    + "join fetch a.role "
                    + "join fetch a.organization "
                    + "left join fetch a.unit "
                    + "where a.person.id=:personId "
                    + "order by a.startDate, a.id",
                PersonRoleAssignment.class)
            .setParameter("personId", personId)
            .getResultList();
    }

    private Map<String, String> roleData(Long assignmentId) {
        if (assignmentId == null || assignmentId <= 0) return Map.of();

        Map<String, String> result = new LinkedHashMap<>();
        entityManager.createQuery(
                "select d from RoleData d where d.personRole.id=:assignmentId order by d.id",
                RoleData.class)
            .setParameter("assignmentId", assignmentId)
            .getResultList()
            .forEach(data -> {
                if (data.key != null && !data.key.isBlank()
                        && data.value != null && !data.value.isBlank()) {
                    result.put(data.key.trim(), data.value.trim());
                }
            });

        return Map.copyOf(result);
    }

    private static PartyRef personRef(long personId) {
        return new PartyRef(
            BusinessId.of("comandos:person:" + personId),
            PartyKind.PERSON
        );
    }
}
