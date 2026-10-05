package com.comandos.core.service;

import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.core.model.RoleData;
import com.fariamiguel.enterprise.party.PartyRoleAssignment;
import com.fariamiguel.enterprise.party.PartyRoleType;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Canonical read directory for reusable party roles while COMANDOS still owns
 * the legacy role-assignment tables.
 */
@Service
@Transactional(readOnly = true)
public class CanonicalPartyRoleDirectory {

    private final EntityManager entityManager;

    public CanonicalPartyRoleDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<PartyRoleAssignment> roles(long personId, TenantId tenantId) {
        if (personId <= 0) return List.of();

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

        return roles(personId, tenantId).stream()
            .filter(assignment -> assignment.role() == role)
            .toList();
    }

    public List<PersonRoleAssignment> productSpecificRoles(long personId) {
        if (personId <= 0) return List.of();

        return assignments(personId).stream()
            .filter(source -> CanonicalPartyRoleMapper.roleType(source.role).isEmpty())
            .toList();
    }

    public boolean hasRole(long personId, TenantId tenantId, PartyRoleType role) {
        if (role == null) return false;
        return roles(personId, tenantId, role).stream()
            .anyMatch(assignment ->
                assignment.status() == com.fariamiguel.enterprise.common.LifecycleStatus.ACTIVE
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
}
