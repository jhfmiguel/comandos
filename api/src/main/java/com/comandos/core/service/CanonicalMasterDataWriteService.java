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
            people.save(
                CanonicalMasterDataMapper.person(person, TENANT)
            );
            return true;
        }

        if (entity instanceof Organization organization) {
            organizations.save(
                CanonicalMasterDataMapper.organization(
                    organization,
                    TENANT,
                    CompanyId.of("comandos:organization:" + organization.id)
                )
            );
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
            contacts.save(
                CanonicalContactMapper.address(address, TENANT)
            );
            return true;
        }

        if (entity instanceof PersonPhone phone) {
            contacts.save(
                CanonicalContactMapper.phone(phone, TENANT)
            );
            return true;
        }

        if (entity instanceof PersonEmail email) {
            contacts.save(
                CanonicalContactMapper.email(email, TENANT)
            );
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
            documents.save(
                CanonicalPartyDocumentMapper.credential(credential, TENANT)
            );
            return true;
        }

        if (entity instanceof PersonQualification qualification) {
            qualifications.save(
                CanonicalProfessionalQualificationMapper.qualification(
                    qualification,
                    TENANT
                )
            );
            return true;
        }

        return false;
    }


    @Override
    @Transactional
    public boolean delete(CoreEntity entity) {
        if (!enabled || entity == null) return false;

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
            contacts.deletePartyContact(
                TENANT,
                BusinessId.of("comandos:person-address:" + address.id)
            );
            return true;
        }

        if (entity instanceof PersonPhone phone) {
            contacts.deletePartyContact(
                TENANT,
                BusinessId.of("comandos:person-phone:" + phone.id)
            );
            return true;
        }

        if (entity instanceof PersonEmail email) {
            contacts.deletePartyContact(
                TENANT,
                BusinessId.of("comandos:person-email:" + email.id)
            );
            return true;
        }

        if (entity instanceof PersonRoleAssignment assignment) {
            if (CanonicalPartyRoleMapper.roleType(assignment.role).isEmpty()) {
                return false;
            }
            roles.delete(
                TENANT,
                BusinessId.of(
                    "comandos:person-role-assignment:" + assignment.id
                )
            );
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
            documents.delete(
                TENANT,
                BusinessId.of(
                    "comandos:person-credential:" + credential.id
                )
            );
            return true;
        }

        if (entity instanceof PersonQualification qualification) {
            qualifications.delete(
                TENANT,
                BusinessId.of(
                    "comandos:person-qualification:" + qualification.id
                )
            );
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
                    roles.save(role);
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
