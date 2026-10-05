package com.comandos.core.service;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonCredential;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.comandos.core.model.PersonQualification;
import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.core.model.RoleData;
import com.fariamiguel.enterprise.contact.ContactRepository;
import com.fariamiguel.enterprise.organization.OrganizationRepository;
import com.fariamiguel.enterprise.party.PartyDocumentRepository;
import com.fariamiguel.enterprise.party.PartyRoleRepository;
import com.fariamiguel.enterprise.people.PersonRepository;
import com.fariamiguel.enterprise.people.ProfessionalQualificationRepository;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private final ContactRepository contacts;
    private final PartyRoleRepository roles;
    private final PartyDocumentRepository documents;
    private final ProfessionalQualificationRepository qualifications;

    public CanonicalMasterDataMirrorService(
            EntityManager entityManager,
            PersonRepository people,
            OrganizationRepository organizations,
            ContactRepository contacts,
            PartyRoleRepository roles,
            PartyDocumentRepository documents,
            ProfessionalQualificationRepository qualifications) {
        this.entityManager = entityManager;
        this.people = people;
        this.organizations = organizations;
        this.contacts = contacts;
        this.roles = roles;
        this.documents = documents;
        this.qualifications = qualifications;
    }

    @Transactional
    public boolean mirror(CoreEntity entity) {
        if (entity instanceof Person person) {
            people.save(CanonicalMasterDataMapper.person(person, TENANT));
            return true;
        }

        if (entity instanceof Organization organization) {
            organizations.save(CanonicalMasterDataMapper.organization(
                organization,
                TENANT,
                CompanyId.of("comandos:organization:" + organization.id)
            ));
            return true;
        }

        if (entity instanceof PersonAddress address) {
            contacts.save(CanonicalContactMapper.address(address, TENANT));
            return true;
        }

        if (entity instanceof PersonPhone phone) {
            contacts.save(CanonicalContactMapper.phone(phone, TENANT));
            return true;
        }

        if (entity instanceof PersonEmail email) {
            contacts.save(CanonicalContactMapper.email(email, TENANT));
            return true;
        }

        if (entity instanceof PersonRoleAssignment assignment) {
            return CanonicalPartyRoleMapper.assignment(
                    assignment,
                    TENANT,
                    roleData(assignment.id)
                )
                .map(role -> {
                    roles.save(role);
                    return true;
                })
                .orElse(false);
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
