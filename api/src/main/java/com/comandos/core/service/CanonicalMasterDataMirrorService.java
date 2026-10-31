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
 * Transactional shadow writer from COMANDOS legacy master-data tables to the
 * canonical Faria Miguel enterprise persistence model.
 *
 * <p>During this migration phase erp_* remains the operational source of truth.
 * CREATE/UPDATE operations are mirrored to fm_* in the same transaction. The
 * read cutover and delete semantics remain separate explicit migration steps.</p>
 */
@Service
public class CanonicalMasterDataMirrorService {

    private static final TenantId TENANT = TenantId.of("comandos");

    private final EntityManager entityManager;
    private final PersonRepository people;
    private final OrganizationRepository organizations;
    private final OrganizationalUnitRepository units;
    private final ContactRepository contacts;
    private final PartyRoleRepository roles;
    private final PartyDocumentRepository documents;
    private final ProfessionalQualificationRepository qualifications;
    private final MasterDataReferenceService references;
    private final boolean enabled;

    public CanonicalMasterDataMirrorService(
            EntityManager entityManager,
            PersonRepository people,
            OrganizationRepository organizations,
            OrganizationalUnitRepository units,
            ContactRepository contacts,
            PartyRoleRepository roles,
            PartyDocumentRepository documents,
            ProfessionalQualificationRepository qualifications,
            MasterDataReferenceService references,
            @Value("${comandos.master-data.shadow-write.enabled:false}") boolean enabled) {
        this.entityManager = entityManager;
        this.people = people;
        this.organizations = organizations;
        this.units = units;
        this.contacts = contacts;
        this.roles = roles;
        this.documents = documents;
        this.qualifications = qualifications;
        this.references = references;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    @Transactional
    public boolean mirror(CoreEntity entity) {
        if (!enabled) return false;
        return mirrorNow(entity);
    }

    @Transactional
    public boolean mirrorForBackfill(CoreEntity entity) {
        return mirrorNow(entity);
    }

    private boolean mirrorNow(CoreEntity entity) {
        if (entity == null) return false;
        if (entity instanceof Person person) {
            var canonical = people.save(
                CanonicalMasterDataMapper.person(person, TENANT)
            );
            track(
                MasterDataReferenceService.PERSON,
                person.id,
                canonical.id().value()
            );
            return true;
        }

        if (entity instanceof Organization organization) {
            var canonical = organizations.save(
                CanonicalMasterDataMapper.organization(
                    organization,
                    TENANT,
                    CompanyId.of("comandos:organization:" + organization.id)
                )
            );
            track(
                MasterDataReferenceService.ORGANIZATION,
                organization.id,
                canonical.id().value()
            );
            return true;
        }

        if (entity instanceof OrganizationalUnit unit) {
            var canonical = units.save(
                CanonicalMasterDataMapper.unit(
                    unit,
                    TENANT,
                    CompanyId.of("comandos:organization:" + unit.organization.id)
                )
            );
            track(
                MasterDataReferenceService.UNIT,
                unit.id,
                canonical.id().value()
            );
            return true;
        }

        if (entity instanceof PersonAddress address) {
            var canonical = contacts.save(
                CanonicalContactMapper.address(address, TENANT)
            );
            track(
                MasterDataReferenceService.ADDRESS,
                address.id,
                canonical.id().value()
            );
            return true;
        }

        if (entity instanceof PersonPhone phone) {
            var canonical = contacts.save(
                CanonicalContactMapper.phone(phone, TENANT)
            );
            track(
                MasterDataReferenceService.PHONE,
                phone.id,
                canonical.id().value()
            );
            return true;
        }

        if (entity instanceof PersonEmail email) {
            var canonical = contacts.save(
                CanonicalContactMapper.email(email, TENANT)
            );
            track(
                MasterDataReferenceService.EMAIL,
                email.id,
                canonical.id().value()
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
            var canonical = documents.save(
                CanonicalPartyDocumentMapper.credential(credential, TENANT)
            );
            track(
                MasterDataReferenceService.CREDENTIAL,
                credential.id,
                canonical.id().value()
            );
            return true;
        }

        if (entity instanceof PersonQualification qualification) {
            var canonical = qualifications.save(
                CanonicalProfessionalQualificationMapper.qualification(
                    qualification,
                    TENANT
                )
            );
            track(
                MasterDataReferenceService.QUALIFICATION,
                qualification.id,
                canonical.id().value()
            );
            return true;
        }

        return false;
    }


    @Transactional
    public boolean delete(CoreEntity entity) {
        if (!enabled || entity == null) return false;

        if (entity instanceof Person person) {
            people.delete(
                TENANT,
                BusinessId.of("comandos:person:" + person.id)
            );
            deactivate(MasterDataReferenceService.PERSON, person.id);
            return true;
        }

        if (entity instanceof Organization organization) {
            organizations.delete(
                TENANT,
                BusinessId.of("comandos:organization:" + organization.id)
            );
            deactivate(MasterDataReferenceService.ORGANIZATION, organization.id);
            return true;
        }

        if (entity instanceof OrganizationalUnit unit) {
            units.delete(
                TENANT,
                OrganizationalUnitId.of("comandos:unit:" + unit.id)
            );
            deactivate(MasterDataReferenceService.UNIT, unit.id);
            return true;
        }

        if (entity instanceof PersonAddress address) {
            contacts.deletePartyContact(
                TENANT,
                BusinessId.of("comandos:person-address:" + address.id)
            );
            deactivate(MasterDataReferenceService.ADDRESS, address.id);
            return true;
        }

        if (entity instanceof PersonPhone phone) {
            contacts.deletePartyContact(
                TENANT,
                BusinessId.of("comandos:person-phone:" + phone.id)
            );
            deactivate(MasterDataReferenceService.PHONE, phone.id);
            return true;
        }

        if (entity instanceof PersonEmail email) {
            contacts.deletePartyContact(
                TENANT,
                BusinessId.of("comandos:person-email:" + email.id)
            );
            deactivate(MasterDataReferenceService.EMAIL, email.id);
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
            deactivate(
                MasterDataReferenceService.PARTY_ROLE,
                assignment.id
            );
            return true;
        }

        if (entity instanceof RoleData roleData) {
            return roleData.personRole != null
                && mirrorRoleAssignment(roleData.personRole);
        }

        if (entity instanceof PersonCredential credential) {
            documents.delete(
                TENANT,
                BusinessId.of(
                    "comandos:person-credential:" + credential.id
                )
            );
            deactivate(MasterDataReferenceService.CREDENTIAL, credential.id);
            return true;
        }

        if (entity instanceof PersonQualification qualification) {
            qualifications.delete(
                TENANT,
                BusinessId.of(
                    "comandos:person-qualification:" + qualification.id
                )
            );
            deactivate(MasterDataReferenceService.QUALIFICATION, qualification.id);
            return true;
        }

        return false;
    }

    private boolean mirrorRoleAssignment(PersonRoleAssignment assignment) {
        return CanonicalPartyRoleMapper.assignment(
                    assignment,
                    TENANT,
                    roleData(assignment.id)
                )
                .map(role -> {
                    var canonical = roles.save(role);
                    track(
                        MasterDataReferenceService.PARTY_ROLE,
                        assignment.id,
                        canonical.id().value()
                    );
                    return true;
                })
                .orElse(false);
    }

    private void track(
            String resourceType,
            Long legacyId,
            String canonicalId) {
        if (legacyId == null || legacyId <= 0) {
            throw new IllegalStateException(
                "Legacy master-data entity must be persisted before crosswalk tracking."
            );
        }
        references.upsert(
            resourceType,
            legacyId,
            canonicalId,
            "Synchronized by canonical master-data mirror"
        );
    }

    private void deactivate(String resourceType, Long legacyId) {
        if (legacyId == null || legacyId <= 0) return;
        references.deactivate(
            resourceType,
            legacyId,
            "Deactivated by legacy master-data deletion"
        );
    }

    private Map<String, String> roleData(Long assignmentId) {
        if (assignmentId == null || assignmentId <= 0) return Map.of();

        Map<String, String> values = new LinkedHashMap<>();

        entityManager.createQuery(
                "select d from RoleData d "
                    + "where d.personRole.id=:assignmentId order by d.id",
                RoleData.class)
            .setParameter("assignmentId", assignmentId)
            .getResultList()
            .forEach(data -> {
                if (data.key != null && !data.key.isBlank()
                        && data.value != null && !data.value.isBlank()) {
                    values.put(data.key.trim(), data.value.trim());
                }
            });

        return Map.copyOf(values);
    }
}
