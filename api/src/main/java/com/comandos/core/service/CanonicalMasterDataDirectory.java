package com.comandos.core.service;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.OrganizationalUnitId;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Canonical read bridge for COMANDOS master data during the migration period.
 *
 * <p>The existing Oracle entities remain the persistence source for now, but
 * callers receive Faria Miguel domain records instead of COMANDOS-owned master
 * data DTOs. This allows product modules to migrate before the database cutover.</p>
 */
@Service
@Transactional(readOnly = true)
public class CanonicalMasterDataDirectory {

    private final EntityManager entityManager;

    public CanonicalMasterDataDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<com.fariamiguel.enterprise.people.Person> findPerson(
            long id,
            TenantId tenantId) {

        if (id <= 0) return Optional.empty();

        Person source = entityManager.find(Person.class, id);
        return source == null
            ? Optional.empty()
            : Optional.of(mapPerson(source, tenantId));
    }

    public Optional<com.fariamiguel.enterprise.organization.Organization> findOrganization(
            long id,
            TenantId tenantId,
            CompanyId companyId) {

        if (id <= 0) return Optional.empty();

        Organization source = entityManager.find(Organization.class, id);
        return source == null
            ? Optional.empty()
            : Optional.of(CanonicalMasterDataMapper.organization(source, tenantId, companyId));
    }

    public Optional<com.fariamiguel.tenancy.api.OrganizationalUnit> findUnit(
            long id,
            TenantId tenantId,
            CompanyId companyId) {

        if (id <= 0) return Optional.empty();

        OrganizationalUnit source = entityManager.find(OrganizationalUnit.class, id);
        return source == null
            ? Optional.empty()
            : Optional.of(CanonicalMasterDataMapper.unit(source, tenantId, companyId));
    }

    public List<com.fariamiguel.enterprise.people.Person> searchPeople(
            TenantId tenantId,
            String query,
            int limit) {

        int safeLimit = boundedLimit(limit);
        String normalized = normalizeQuery(query);

        var jpql = new StringBuilder("select p from Person p where p.active = true");
        if (normalized != null) {
            jpql.append(" and (lower(p.fullName) like :query escape '!' or lower(p.taxId) like :query escape '!')");
        }
        jpql.append(" order by p.fullName, p.id");

        var typed = entityManager.createQuery(jpql.toString(), Person.class)
            .setMaxResults(safeLimit);

        if (normalized != null) {
            typed.setParameter("query", "%" + escapeLike(normalized) + "%");
        }

        return typed.getResultList().stream()
            .map(person -> mapPerson(person, tenantId))
            .toList();
    }

    public List<com.fariamiguel.enterprise.organization.Organization> listOrganizations(
            TenantId tenantId) {

        return entityManager.createQuery(
                "select o from Organization o where o.active = true order by o.name, o.id",
                Organization.class)
            .getResultList()
            .stream()
            .map(source -> CanonicalMasterDataMapper.organization(
                source,
                tenantId,
                CompanyId.of("comandos:organization:" + source.id)
            ))
            .toList();
    }

    public List<com.fariamiguel.tenancy.api.OrganizationalUnit> listUnits(
            long organizationId,
            TenantId tenantId,
            CompanyId companyId) {

        if (organizationId <= 0) return List.of();

        return entityManager.createQuery(
                "select u from OrganizationalUnit u "
                    + "where u.organization.id = :organizationId and u.active = true "
                    + "order by u.name, u.id",
                OrganizationalUnit.class)
            .setParameter("organizationId", organizationId)
            .getResultList()
            .stream()
            .map(source -> CanonicalMasterDataMapper.unit(source, tenantId, companyId))
            .toList();
    }

    private com.fariamiguel.enterprise.people.Person mapPerson(
            Person source,
            TenantId tenantId) {

        var addresses = entityManager.createQuery(
                "select a from PersonAddress a where a.person.id=:personId and a.archived=false order by a.primaryAddress desc, a.id",
                PersonAddress.class)
            .setParameter("personId", source.id)
            .getResultList();

        var phones = entityManager.createQuery(
                "select p from PersonPhone p where p.person.id=:personId order by p.primaryPhone desc, p.id",
                PersonPhone.class)
            .setParameter("personId", source.id)
            .getResultList();

        var emails = entityManager.createQuery(
                "select e from PersonEmail e where e.person.id=:personId order by e.primaryEmail desc, e.id",
                PersonEmail.class)
            .setParameter("personId", source.id)
            .getResultList();

        return CanonicalMasterDataMapper.person(
            source,
            tenantId,
            addresses,
            phones,
            emails
        );
    }

    public static long legacyPersonId(BusinessId id) {
        return legacyNumericId(id == null ? null : id.value(), "comandos:person:");
    }

    public static long legacyOrganizationId(BusinessId id) {
        return legacyNumericId(id == null ? null : id.value(), "comandos:organization:");
    }

    public static long legacyUnitId(OrganizationalUnitId id) {
        return legacyNumericId(id == null ? null : id.value(), "comandos:unit:");
    }

    private static long legacyNumericId(String value, String prefix) {
        if (value == null || !value.startsWith(prefix)) {
            throw new IllegalArgumentException("Identifier is not a COMANDOS compatibility id");
        }

        try {
            long id = Long.parseLong(value.substring(prefix.length()));
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid COMANDOS compatibility id: " + value, exception);
        }
    }

    private static int boundedLimit(int limit) {
        if (limit <= 0) return 20;
        return Math.min(limit, 100);
    }

    private static String normalizeQuery(String query) {
        if (query == null) return null;
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String escapeLike(String value) {
        return value
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
    }
}
