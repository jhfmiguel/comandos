package com.comandos.core.service;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.Person;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.contact.ContactRepository;
import com.fariamiguel.enterprise.organization.OrganizationRepository;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.enterprise.people.PersonRepository;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.OrganizationalUnitId;
import com.fariamiguel.tenancy.api.OrganizationalUnitRepository;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final CanonicalContactDirectory contacts;
    private final PersonRepository canonicalPeople;
    private final OrganizationRepository canonicalOrganizations;
    private final OrganizationalUnitRepository canonicalUnits;
    private final ContactRepository canonicalContacts;
    private final boolean canonicalReadEnabled;

    @Autowired
    public CanonicalMasterDataDirectory(
            EntityManager entityManager,
            CanonicalContactDirectory contacts,
            PersonRepository canonicalPeople,
            OrganizationRepository canonicalOrganizations,
            OrganizationalUnitRepository canonicalUnits,
            ContactRepository canonicalContacts,
            @Value("${comandos.master-data.canonical-read.enabled:false}")
            boolean canonicalReadEnabled) {
        this.entityManager = entityManager;
        this.contacts = contacts;
        this.canonicalPeople = canonicalPeople;
        this.canonicalOrganizations = canonicalOrganizations;
        this.canonicalUnits = canonicalUnits;
        this.canonicalContacts = canonicalContacts;
        this.canonicalReadEnabled = canonicalReadEnabled;
    }

    /**
     * Transitional constructor for focused unit tests using legacy projection.
     */
    @Deprecated
    public CanonicalMasterDataDirectory(
            EntityManager entityManager,
            CanonicalContactDirectory contacts) {
        this.entityManager = entityManager;
        this.contacts = contacts;
        this.canonicalPeople = null;
        this.canonicalOrganizations = null;
        this.canonicalUnits = null;
        this.canonicalContacts = null;
        this.canonicalReadEnabled = false;
    }

    public boolean canonicalReadEnabled() {
        return canonicalReadEnabled;
    }

    public Optional<com.fariamiguel.enterprise.people.Person> findPerson(
            long id,
            TenantId tenantId) {

        if (id <= 0) return Optional.empty();

        if (canonicalReadEnabled) {
            return canonicalPeople
                .find(tenantId, BusinessId.of("comandos:person:" + id))
                .map(person -> enrichCanonicalPerson(person, tenantId));
        }

        Person source = entityManager.find(Person.class, id);
        return source == null
            ? Optional.empty()
            : Optional.of(mapPerson(source, tenantId));
    }

    public Optional<com.fariamiguel.enterprise.organization.Organization> findOrganization(
            long id,
            TenantId tenantId) {
        return findOrganization(
            id,
            tenantId,
            CompanyId.of("comandos:organization:" + id)
        );
    }

    public Optional<com.fariamiguel.enterprise.organization.Organization> findOrganization(
            long id,
            TenantId tenantId,
            CompanyId companyId) {

        if (id <= 0) return Optional.empty();

        if (canonicalReadEnabled) {
            return canonicalOrganizations.find(
                tenantId,
                BusinessId.of("comandos:organization:" + id)
            );
        }

        Organization source = entityManager.find(Organization.class, id);
        return source == null
            ? Optional.empty()
            : Optional.of(CanonicalMasterDataMapper.organization(source, tenantId, companyId));
    }

    public Optional<com.fariamiguel.tenancy.api.OrganizationalUnit> findUnit(
            long id,
            TenantId tenantId) {

        if (id <= 0) return Optional.empty();

        if (canonicalReadEnabled) {
            return canonicalUnits.find(
                tenantId,
                OrganizationalUnitId.of("comandos:unit:" + id)
            );
        }

        OrganizationalUnit source = entityManager.find(OrganizationalUnit.class, id);
        if (source == null || source.organization == null || source.organization.id == null) {
            return Optional.empty();
        }

        return Optional.of(CanonicalMasterDataMapper.unit(
            source,
            tenantId,
            CompanyId.of("comandos:organization:" + source.organization.id)
        ));
    }

    public Optional<com.fariamiguel.tenancy.api.OrganizationalUnit> findUnit(
            long id,
            TenantId tenantId,
            CompanyId companyId) {

        if (id <= 0) return Optional.empty();

        if (canonicalReadEnabled) {
            return canonicalUnits.find(
                tenantId,
                OrganizationalUnitId.of("comandos:unit:" + id)
            );
        }

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

        if (canonicalReadEnabled) {
            return canonicalPeople
                .search(tenantId, normalized == null ? "" : normalized, safeLimit)
                .stream()
                .map(person -> enrichCanonicalPerson(person, tenantId))
                .toList();
        }

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

        if (canonicalReadEnabled) {
            return canonicalOrganizations.list(tenantId).stream()
                .filter(org -> org.status() == com.fariamiguel.enterprise.common.LifecycleStatus.ACTIVE)
                .toList();
        }

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

        if (canonicalReadEnabled) {
            return canonicalUnits.findByCompany(tenantId, companyId).stream()
                .filter(com.fariamiguel.tenancy.api.OrganizationalUnit::active)
                .toList();
        }

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

    private com.fariamiguel.enterprise.people.Person enrichCanonicalPerson(
            com.fariamiguel.enterprise.people.Person person,
            TenantId tenantId) {

        PartyRef party = new PartyRef(person.id(), PartyKind.PERSON);

        var addresses = canonicalContacts.addresses(tenantId, party).stream()
            .map(contact -> contact.address())
            .toList();

        var emails = canonicalContacts.emails(tenantId, party).stream()
            .map(contact -> contact.email())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

        var phones = canonicalContacts.phones(tenantId, party).stream()
            .map(contact -> contact.number())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

        return new com.fariamiguel.enterprise.people.Person(
            person.id(),
            person.tenantId(),
            person.name(),
            person.taxId(),
            person.birthDate(),
            addresses,
            emails,
            phones,
            person.status(),
            person.attributes()
        );
    }

    private com.fariamiguel.enterprise.people.Person mapPerson(
            Person source,
            TenantId tenantId) {

        var addresses = contacts.addresses(source.id, tenantId).stream()
            .map(contact -> contact.address())
            .toList();

        var phoneValues = contacts.phones(source.id, tenantId).stream()
            .map(contact -> contact.number())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

        var emailValues = contacts.emails(source.id, tenantId).stream()
            .map(contact -> contact.email())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

        return CanonicalMasterDataMapper.person(
            source,
            tenantId,
            addresses,
            phoneValues,
            emailValues
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
