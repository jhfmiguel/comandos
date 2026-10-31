package com.comandos.core.service;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonCredential;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.comandos.core.model.PersonQualification;
import com.comandos.core.model.PersonRoleAssignment;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.contact.ContactRepository;
import com.fariamiguel.enterprise.organization.OrganizationRepository;
import com.fariamiguel.enterprise.party.PartyDocumentRepository;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.enterprise.party.PartyRoleRepository;
import com.fariamiguel.enterprise.people.PersonRepository;
import com.fariamiguel.enterprise.people.ProfessionalQualificationRepository;
import com.fariamiguel.tenancy.api.OrganizationalUnitRepository;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies parity between legacy COMANDOS master-data persistence and the
 * canonical Faria Miguel persistence model.
 *
 * <p>This service is intentionally read-only. A persistence cutover must not be
 * considered ready until all blocking mismatches are zero.</p>
 */
@Service
@Transactional(readOnly = true)
public class MasterDataParityService {

    public record ParityMismatch(
        String resource,
        String legacyId,
        String canonicalId,
        String detail
    ) {}

    public record ParityReport(
        boolean consistent,
        long checked,
        List<ParityMismatch> mismatches
    ) {}

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

    public MasterDataParityService(
            EntityManager entityManager,
            PersonRepository people,
            OrganizationRepository organizations,
            OrganizationalUnitRepository units,
            ContactRepository contacts,
            PartyRoleRepository roles,
            PartyDocumentRepository documents,
            ProfessionalQualificationRepository qualifications,
            MasterDataReferenceService references) {
        this.entityManager = entityManager;
        this.people = people;
        this.organizations = organizations;
        this.units = units;
        this.contacts = contacts;
        this.roles = roles;
        this.documents = documents;
        this.qualifications = qualifications;
        this.references = references;
    }

    public ParityReport verify() {
        List<ParityMismatch> mismatches = new ArrayList<>();
        long checked = 0;

        checked += verifyPeople(mismatches);
        checked += verifyOrganizations(mismatches);
        checked += verifyUnits(mismatches);
        checked += verifyContacts(mismatches);
        checked += verifyRoles(mismatches);
        checked += verifyDocuments(mismatches);
        checked += verifyQualifications(mismatches);

        return new ParityReport(
            mismatches.isEmpty(),
            checked,
            List.copyOf(mismatches)
        );
    }

    private long verifyPeople(List<ParityMismatch> mismatches) {
        long checked = 0;
        for (Person legacy : all(Person.class)) {
            checked++;
            var expected = CanonicalMasterDataMapper.person(legacy, TENANT);
            verifyCrosswalk(
                mismatches,
                MasterDataReferenceService.PERSON,
                legacy.id,
                expected.id().value()
            );

            var actual = people.find(TENANT, expected.id());

            if (actual.isEmpty()) {
                missing(mismatches, "person", legacy.id, expected.id().value());
                continue;
            }

            var value = actual.orElseThrow();
            if (!Objects.equals(expected.name(), value.name())
                    || !Objects.equals(expected.taxId(), value.taxId())
                    || !Objects.equals(expected.birthDate(), value.birthDate())
                    || expected.status() != value.status()) {
                mismatch(
                    mismatches,
                    "person",
                    legacy.id,
                    expected.id().value(),
                    "Canonical person fields differ from legacy projection."
                );
            }
        }
        return checked;
    }

    private long verifyOrganizations(List<ParityMismatch> mismatches) {
        long checked = 0;
        for (Organization legacy : all(Organization.class)) {
            checked++;
            var expected = CanonicalMasterDataMapper.organization(
                legacy,
                TENANT,
                com.fariamiguel.tenancy.api.CompanyId.of(
                    "comandos:organization:" + legacy.id
                )
            );
            verifyCrosswalk(
                mismatches,
                MasterDataReferenceService.ORGANIZATION,
                legacy.id,
                expected.id().value()
            );

            var actual = organizations.find(TENANT, expected.id());

            if (actual.isEmpty()) {
                missing(
                    mismatches,
                    "organization",
                    legacy.id,
                    expected.id().value()
                );
                continue;
            }

            var value = actual.orElseThrow();
            if (!Objects.equals(expected.legalName(), value.legalName())
                    || !Objects.equals(expected.tradeName(), value.tradeName())
                    || !Objects.equals(expected.taxId(), value.taxId())
                    || expected.status() != value.status()) {
                mismatch(
                    mismatches,
                    "organization",
                    legacy.id,
                    expected.id().value(),
                    "Canonical organization fields differ from legacy projection."
                );
            }
        }
        return checked;
    }

    private long verifyUnits(List<ParityMismatch> mismatches) {
        long checked = 0;
        for (OrganizationalUnit legacy : all(OrganizationalUnit.class)) {
            checked++;
            var expected = CanonicalMasterDataMapper.unit(
                legacy,
                TENANT,
                com.fariamiguel.tenancy.api.CompanyId.of(
                    "comandos:organization:" + legacy.organization.id
                )
            );

            verifyCrosswalk(
                mismatches,
                MasterDataReferenceService.UNIT,
                legacy.id,
                expected.id().value()
            );

            var actual = units.find(TENANT, expected.id());
            if (actual.isEmpty()) {
                missing(mismatches, "unit", legacy.id, expected.id().value());
                continue;
            }

            var value = actual.orElseThrow();
            if (!Objects.equals(expected.companyId(), value.companyId())
                    || !Objects.equals(expected.parentUnitId(), value.parentUnitId())
                    || !Objects.equals(expected.code(), value.code())
                    || !Objects.equals(expected.name(), value.name())
                    || expected.type() != value.type()
                    || expected.active() != value.active()) {
                mismatch(
                    mismatches,
                    "unit",
                    legacy.id,
                    expected.id().value(),
                    "Canonical organizational unit differs from legacy projection."
                );
            }
        }
        return checked;
    }

    private long verifyContacts(List<ParityMismatch> mismatches) {
        long checked = 0;

        for (Person person : all(Person.class)) {
            PartyRef party = new PartyRef(
                BusinessId.of("comandos:person:" + person.id),
                PartyKind.PERSON
            );

            Set<String> canonicalAddressIds = contacts.addresses(TENANT, party)
                .stream()
                .map(contact -> contact.id().value())
                .collect(java.util.stream.Collectors.toSet());

            Set<String> canonicalPhoneIds = contacts.phones(TENANT, party)
                .stream()
                .map(contact -> contact.id().value())
                .collect(java.util.stream.Collectors.toSet());

            Set<String> canonicalEmailIds = contacts.emails(TENANT, party)
                .stream()
                .map(contact -> contact.id().value())
                .collect(java.util.stream.Collectors.toSet());

            for (PersonAddress address : entityManager.createQuery(
                    "select a from PersonAddress a "
                        + "where a.person.id=:personId and a.archived=false order by a.id",
                    PersonAddress.class)
                .setParameter("personId", person.id)
                .getResultList()) {
                checked++;
                String expected = "comandos:person-address:" + address.id;
                verifyCrosswalk(
                    mismatches,
                    MasterDataReferenceService.ADDRESS,
                    address.id,
                    expected
                );
                if (!canonicalAddressIds.contains(expected)) {
                    missing(mismatches, "person-address", address.id, expected);
                }
            }

            for (PersonPhone phone : entityManager.createQuery(
                    "select p from PersonPhone p where p.person.id=:personId order by p.id",
                    PersonPhone.class)
                .setParameter("personId", person.id)
                .getResultList()) {
                checked++;
                String expected = "comandos:person-phone:" + phone.id;
                verifyCrosswalk(
                    mismatches,
                    MasterDataReferenceService.PHONE,
                    phone.id,
                    expected
                );
                if (!canonicalPhoneIds.contains(expected)) {
                    missing(mismatches, "person-phone", phone.id, expected);
                }
            }

            for (PersonEmail email : entityManager.createQuery(
                    "select e from PersonEmail e where e.person.id=:personId order by e.id",
                    PersonEmail.class)
                .setParameter("personId", person.id)
                .getResultList()) {
                checked++;
                String expected = "comandos:person-email:" + email.id;
                verifyCrosswalk(
                    mismatches,
                    MasterDataReferenceService.EMAIL,
                    email.id,
                    expected
                );
                if (!canonicalEmailIds.contains(expected)) {
                    missing(mismatches, "person-email", email.id, expected);
                }
            }
        }

        return checked;
    }

    private long verifyRoles(List<ParityMismatch> mismatches) {
        long checked = 0;

        for (Person person : all(Person.class)) {
            PartyRef party = new PartyRef(
                BusinessId.of("comandos:person:" + person.id),
                PartyKind.PERSON
            );

            Set<String> canonicalIds = roles.findByParty(TENANT, party)
                .stream()
                .map(role -> role.id().value())
                .collect(java.util.stream.Collectors.toSet());

            for (PersonRoleAssignment assignment : entityManager.createQuery(
                    "select a from PersonRoleAssignment a "
                        + "where a.person.id=:personId order by a.id",
                    PersonRoleAssignment.class)
                .setParameter("personId", person.id)
                .getResultList()) {

                var canonicalType =
                    CanonicalPartyRoleMapper.roleType(assignment.role);

                if (canonicalType.isEmpty()) continue;

                checked++;
                String expected =
                    "comandos:person-role-assignment:" + assignment.id;

                verifyCrosswalk(
                    mismatches,
                    MasterDataReferenceService.PARTY_ROLE,
                    assignment.id,
                    expected
                );

                if (!canonicalIds.contains(expected)) {
                    missing(
                        mismatches,
                        "person-role",
                        assignment.id,
                        expected
                    );
                }
            }
        }

        return checked;
    }

    private long verifyDocuments(List<ParityMismatch> mismatches) {
        long checked = 0;

        for (Person person : all(Person.class)) {
            PartyRef party = new PartyRef(
                BusinessId.of("comandos:person:" + person.id),
                PartyKind.PERSON
            );

            Set<String> canonicalIds = documents.findByParty(TENANT, party)
                .stream()
                .map(document -> document.id().value())
                .collect(java.util.stream.Collectors.toSet());

            for (PersonCredential credential : entityManager.createQuery(
                    "select c from PersonCredential c "
                        + "where c.person.id=:personId order by c.id",
                    PersonCredential.class)
                .setParameter("personId", person.id)
                .getResultList()) {
                checked++;
                String expected =
                    "comandos:person-credential:" + credential.id;

                verifyCrosswalk(
                    mismatches,
                    MasterDataReferenceService.CREDENTIAL,
                    credential.id,
                    expected
                );

                if (!canonicalIds.contains(expected)) {
                    missing(
                        mismatches,
                        "person-credential",
                        credential.id,
                        expected
                    );
                }
            }
        }

        return checked;
    }

    private long verifyQualifications(List<ParityMismatch> mismatches) {
        long checked = 0;

        for (PersonQualification qualification :
                all(PersonQualification.class)) {
            checked++;

            String canonicalId =
                "comandos:person-qualification:" + qualification.id;

            verifyCrosswalk(
                mismatches,
                MasterDataReferenceService.QUALIFICATION,
                qualification.id,
                canonicalId
            );

            if (qualifications.find(
                    TENANT,
                    BusinessId.of(canonicalId)
                ).isEmpty()) {
                missing(
                    mismatches,
                    "person-qualification",
                    qualification.id,
                    canonicalId
                );
            }
        }

        return checked;
    }

    private <T> List<T> all(Class<T> type) {
        return entityManager.createQuery(
                "select e from " + type.getSimpleName() + " e order by e.id",
                type)
            .getResultList();
    }

    private void verifyCrosswalk(
            List<ParityMismatch> mismatches,
            String resourceType,
            Long legacyId,
            String canonicalId) {

        if (legacyId == null || legacyId <= 0) {
            mismatch(
                mismatches,
                "crosswalk:" + resourceType.toLowerCase(java.util.Locale.ROOT),
                legacyId,
                canonicalId,
                "Legacy identifier is invalid for crosswalk verification."
            );
            return;
        }

        var resolved = references.resolveCanonicalId(resourceType, legacyId);

        if (resolved.isEmpty()) {
            mismatch(
                mismatches,
                "crosswalk:" + resourceType.toLowerCase(java.util.Locale.ROOT),
                legacyId,
                canonicalId,
                "Active legacy-to-canonical crosswalk is missing."
            );
            return;
        }

        if (!canonicalId.equals(resolved.orElseThrow())) {
            mismatch(
                mismatches,
                "crosswalk:" + resourceType.toLowerCase(java.util.Locale.ROOT),
                legacyId,
                canonicalId,
                "Crosswalk points to a different canonical identifier: "
                    + resolved.orElseThrow()
            );
        }
    }

    private static void missing(
            List<ParityMismatch> mismatches,
            String resource,
            Object legacyId,
            String canonicalId) {
        mismatch(
            mismatches,
            resource,
            legacyId,
            canonicalId,
            "Canonical record is missing."
        );
    }

    private static void mismatch(
            List<ParityMismatch> mismatches,
            String resource,
            Object legacyId,
            String canonicalId,
            String detail) {
        mismatches.add(new ParityMismatch(
            resource,
            String.valueOf(legacyId),
            canonicalId,
            detail
        ));
    }
}
