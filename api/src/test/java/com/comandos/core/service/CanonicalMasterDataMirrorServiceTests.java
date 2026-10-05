package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonQualification;
import com.fariamiguel.enterprise.contact.ContactRepository;
import com.fariamiguel.enterprise.organization.OrganizationRepository;
import com.fariamiguel.enterprise.party.PartyDocumentRepository;
import com.fariamiguel.enterprise.party.PartyRoleRepository;
import com.fariamiguel.enterprise.people.PersonRepository;
import com.fariamiguel.enterprise.people.ProfessionalQualificationRepository;
import com.fariamiguel.tenancy.api.OrganizationalUnitRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CanonicalMasterDataMirrorServiceTests {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final PersonRepository people = mock(PersonRepository.class);
    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final OrganizationalUnitRepository units = mock(OrganizationalUnitRepository.class);
    private final ContactRepository contacts = mock(ContactRepository.class);
    private final PartyRoleRepository roles = mock(PartyRoleRepository.class);
    private final PartyDocumentRepository documents = mock(PartyDocumentRepository.class);
    private final ProfessionalQualificationRepository qualifications =
        mock(ProfessionalQualificationRepository.class);

    @Test
    void doesNothingWhenShadowWriteIsDisabled() {
        var mirror = service(false);

        var person = new Person();
        person.id = 10L;
        person.fullName = "Pessoa";
        person.active = true;

        assertFalse(mirror.mirror(person));
        verify(people, never()).save(any());
    }

    @Test
    void mirrorsPersonAndOrganizationWhenEnabled() {
        var mirror = service(true);

        var person = new Person();
        person.id = 10L;
        person.personType = "PF";
        person.fullName = "Pessoa Teste";
        person.active = true;

        var organization = new Organization();
        organization.id = 20L;
        organization.name = "Organizacao Teste";
        organization.acronym = "OT";
        organization.active = true;
        organization.publicOrganization = false;

        assertTrue(mirror.mirror(person));
        assertTrue(mirror.mirror(organization));

        verify(people).save(any());
        verify(organizations).save(any());
    }


    @Test
    void mirrorsOrganizationalUnitWhenEnabled() {
        var mirror = service(true);

        var organization = new Organization();
        organization.id = 20L;
        organization.name = "Organizacao Teste";

        var unit = new OrganizationalUnit();
        unit.id = 21L;
        unit.organization = organization;
        unit.code = "UNIT-01";
        unit.name = "Unidade Operacional";
        unit.type = "OPERATIONAL";
        unit.active = true;

        assertTrue(mirror.mirror(unit));
        verify(units).save(any());
    }

    @Test
    void mirrorsProfessionalQualificationWhenEnabled() {
        var mirror = service(true);

        var person = new Person();
        person.id = 11L;

        var qualification = new PersonQualification();
        qualification.id = 30L;
        qualification.person = person;
        qualification.category = "Instrutor";
        qualification.validUntil = LocalDate.of(2030, 1, 1);
        qualification.status = "ACTIVE";

        assertTrue(mirror.mirror(qualification));
        verify(qualifications).save(any());
    }


    @Test
    void mirrorsCanonicalDeletionWhenEnabled() {
        var mirror = service(true);

        var person = new Person();
        person.id = 99L;
        person.fullName = "Pessoa Removida";
        person.active = true;

        assertTrue(mirror.delete(person));

        verify(people).delete(
            com.fariamiguel.tenancy.api.TenantId.of("comandos"),
            com.fariamiguel.enterprise.common.BusinessId.of(
                "comandos:person:99"
            )
        );
    }

    private CanonicalMasterDataMirrorService service(boolean enabled) {
        return new CanonicalMasterDataMirrorService(
            entityManager,
            people,
            organizations,
            units,
            contacts,
            roles,
            documents,
            qualifications,
            enabled
        );
    }
}
