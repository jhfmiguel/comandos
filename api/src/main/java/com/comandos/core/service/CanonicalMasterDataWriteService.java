package com.comandos.core.service;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonCredential;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.comandos.core.model.PersonQualification;
import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.core.model.RoleData;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.contact.ContactRepository;
import com.fariamiguel.enterprise.organization.OrganizationRepository;
import com.fariamiguel.enterprise.party.PartyDocumentRepository;
import com.fariamiguel.enterprise.party.PartyRoleRepository;
import com.fariamiguel.enterprise.people.PersonRepository;
import com.fariamiguel.enterprise.people.ProfessionalQualificationRepository;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.OrganizationalUnitId;
import com.fariamiguel.tenancy.api.OrganizationalUnitRepository;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import com.fariamiguel.enterprise.party.PartyMasterDataFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional canonical master-data persistence adapter.
 *
 * Existing COMANDOS persistence compatibility remains transactional; the canonical
 * repositories are updated through the stable CanonicalMasterDataWritePort.
 * Historical backfill remains an explicit maintenance operation until cutover.
 */
@Service
public class CanonicalMasterDataWriteService implements CanonicalMasterDataWritePort {

    private static final TenantId TENANT = TenantId.of("comandos");

    private final EntityManager entityManager;
    private final PersonRepository people;
    private final OrganizationRepository organizations;
    private final OrganizationalUnitRepository units;
    private final ContactRepository contacts;
    private final PartyRoleRepository roles;
    private final PartyDocumentRepository documents;
    private final ProfessionalQualificationRepository qualifications;
    private final boolean enabled;
    private PartyMasterDataFacade enterprise;

    @Autowired(required = false)
    public void setEnterprise(PartyMasterDataFacade enterprise) {
        this.enterprise = enterprise;
    }

    public CanonicalMasterDataWriteService(
            EntityManager entityManager,
            PersonRepository people,
            OrganizationRepository organizations,
            OrganizationalUnitRepository units,
            ContactRepository contacts,
            PartyRoleRepository roles,
            PartyDocumentRepository documents,
            ProfessionalQualificationRepository qualifications,
            @Value("${comandos.master-data.shadow-write.enabled:false}") boolean enabled) {
        this.entityManager = entityManager;
        this.people = people;
        this.organizations = organizations;
        this.units = units;
        this.contacts = contacts;
        this.roles = roles;
        this.documents = documents;
        this.qualifications = qualifications;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    @Override
    @Transactional
    public boolean synchronize(CoreEntity entity) {
        if (!enabled || entity == null) return false;
        requirePersistedIdentity(entity);
        return mirrorNow(entity);
    }

    private static void requirePersistedIdentity(CoreEntity entity) {
        if (entity.id == null || entity.id <= 0) {
            throw new IllegalStateException(
                "Canonical master-data write requires a persisted business identifier: "
                    + entity.getClass().getSimpleName()
            );
        }
    }

    @Transactional
    public boolean synchronizeHistoricalBackfill(CoreEntity entity) {
        if (entity == null) return false;
        requirePersistedIdentity(entity);
        return mirrorNow(entity);
    }

    private boolean mirrorNow(CoreEntity entity) {
        if (entity == null) return false;
        if (entity instanceof Person person) {
            var canonical = CanonicalMasterDataMapper.person(person, TENANT);
            if (enterprise != null) enterprise.savePerson(canonical);
            else people.save(canonical);
            return true;
        }

        if (entity instanceof Organization organization) {
            var canonical = CanonicalMasterDataMapper.organization(organization, TENANT,
                CompanyId.of("comandos:organization:" + organization.id));
            if (enterprise != null) enterprise.saveOrganization(canonical);
            else organizations.save(canonical);
            return true;
        }

        if (entity instanceof OrganizationalUnit unit) {
            units.save(
                CanonicalMasterDataMapper.unit(
                    unit,
                    TENANT,
                    CompanyId.of("comandos:organization:" + unit.organization.id)
                )
            );
            return true;
        }

        if (entity instanceof PersonAddress address) {
            var canonical = CanonicalContactMapper.address(address, TENANT);
            if (enterprise != null) enterprise.saveAddress(canonical);
            else contacts.save(canonical);
            return true;
        }

        if (entity instanceof PersonPhone phone) {
            var canonical = CanonicalContactMapper.phone(phone, TENANT);
            if (enterprise != null) enterprise.savePhone(canonical);
            else contacts.save(canonical);
            return true;
        }

        if (entity instanceof PersonEmail email) {
            var canonical = CanonicalContactMapper.email(email, TENANT);
            if (enterprise != null) enterprise.saveEmail(canonical);
            else contacts.save(canonical);
            return true;
        }

        if (entity instanceof PersonRoleAssignment assignment) {
            return mirrorRoleAssignment(assignment);
        }

        if (entity instanceof RoleData roleData) {
            return roleData.personRole != null
                && mirrorRoleAssignment(roleData.personRole);
        }

        if (entity instanceof PersonCredential credential) {
            var canonical = CanonicalPartyDocumentMapper.credential(credential, TENANT);
            if (enterprise != null) enterprise.saveDocument(canonical);
            else documents.save(canonical);
            return true;
        }

        if (entity instanceof PersonQualification qualification) {
            var canonical = CanonicalProfessionalQualificationMapper.qualification(qualification, TENANT);
            if (enterprise != null) enterprise.saveQualification(canonical);
            else qualifications.save(canonical);
            return true;
        }

        return false;
    }


    @Override
    @Transactional
    public boolean delete(CoreEntity entity) {
        if (!enabled || entity == null) return false;
        requirePersistedIdentity(entity);

        if (entity instanceof Person person) {
            people.delete(
                TENANT,
                BusinessId.of("comandos:person:" + person.id)
            );
            return true;
        }

        if (entity instanceof Organization organization) {
            organizations.delete(
                TENANT,
                BusinessId.of("comandos:organization:" + organization.id)
            );
            return true;
        }

        if (entity instanceof OrganizationalUnit unit) {
            units.delete(
                TENANT,
                OrganizationalUnitId.of("comandos:unit:" + unit.id)
            );
            return true;
        }

        if (entity instanceof PersonAddress address) {
            if (enterprise != null) enterprise.deleteContact(TENANT, BusinessId.of("comandos:person-address:" + address.id));
            else contacts.deletePartyContact(TENANT, BusinessId.of("comandos:person-address:" + address.id));
            return true;
        }

        if (entity instanceof PersonPhone phone) {
            if (enterprise != null) enterprise.deleteContact(TENANT, BusinessId.of("comandos:person-phone:" + phone.id));
            else contacts.deletePartyContact(TENANT, BusinessId.of("comandos:person-phone:" + phone.id));
            return true;
        }

        if (entity instanceof PersonEmail email) {
            if (enterprise != null) enterprise.deleteContact(TENANT, BusinessId.of("comandos:person-email:" + email.id));
            else contacts.deletePartyContact(TENANT, BusinessId.of("comandos:person-email:" + email.id));
            return true;
        }

        if (entity instanceof PersonRoleAssignment assignment) {
            if (CanonicalPartyRoleMapper.roleType(assignment.role).isEmpty()) {
                return false;
            }
            if (enterprise != null) enterprise.deleteRole(TENANT, BusinessId.of("comandos:person-role-assignment:" + assignment.id));
            else roles.delete(TENANT, BusinessId.of("comandos:person-role-assignment:" + assignment.id));
            return true;
        }

        if (entity instanceof RoleData roleData) {
            return roleData.personRole != null
                && mirrorRoleAssignment(
                    roleData.personRole,
                    roleData(roleData.personRole.id, roleData.id)
                );
        }

        if (entity instanceof PersonCredential credential) {
            if (enterprise != null) enterprise.deleteDocument(TENANT, BusinessId.of("comandos:person-credential:" + credential.id));
            else documents.delete(TENANT, BusinessId.of("comandos:person-credential:" + credential.id));
            return true;
        }

        if (entity instanceof PersonQualification qualification) {
            if (enterprise != null) enterprise.deleteQualification(TENANT, BusinessId.of("comandos:person-qualification:" + qualification.id));
            else qualifications.delete(TENANT, BusinessId.of("comandos:person-qualification:" + qualification.id));
            return true;
        }

        return false;
    }

    private boolean mirrorRoleAssignment(PersonRoleAssignment assignment) {
        return mirrorRoleAssignment(assignment, roleData(assignment.id));
    }

    private boolean mirrorRoleAssignment(
            PersonRoleAssignment assignment,
            Map<String, String> details) {
        return CanonicalPartyRoleMapper.assignment(
                    assignment,
                    TENANT,
                    details
                )
                .map(role -> {
                    if (enterprise != null) enterprise.assignRole(role);
                    else roles.save(role);
                    return true;
                })
                .orElse(false);
    }

    private Map<String, String> roleData(Long assignmentId) {
        return roleData(assignmentId, null);
    }

    private Map<String, String> roleData(Long assignmentId, Long excludedDetailId) {
        if (assignmentId == null || assignmentId <= 0) return Map.of();

        Map<String, String> values = new LinkedHashMap<>();

        var query = entityManager.createQuery(
                "select d from RoleData d "
                    + "where d.personRole.id=:assignmentId "
                    + (excludedDetailId == null ? "" : "and d.id <> :excludedDetailId ")
                    + "order by d.id",
                RoleData.class)
            .setParameter("assignmentId", assignmentId);
        if (excludedDetailId != null) {
            query.setParameter("excludedDetailId", excludedDetailId);
        }
        query.getResultList()
            .forEach(data -> {
                if (data.key != null && !data.key.isBlank()
                        && data.value != null && !data.value.isBlank()) {
                    values.put(data.key.trim(), data.value.trim());
                }
            });

        return Map.copyOf(values);
    }
}
