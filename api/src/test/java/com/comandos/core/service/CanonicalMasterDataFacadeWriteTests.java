package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.comandos.core.model.*;
import com.fariamiguel.enterprise.banking.*;
import com.fariamiguel.enterprise.common.*;
import com.fariamiguel.enterprise.contact.*;
import com.fariamiguel.enterprise.organization.OrganizationRepository;
import com.fariamiguel.enterprise.party.*;
import com.fariamiguel.enterprise.people.PersonRepository;
import com.fariamiguel.enterprise.people.ProfessionalQualificationRepository;
import com.fariamiguel.tenancy.api.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CanonicalMasterDataFacadeWriteTests {
    private final EntityManager em = mock(EntityManager.class);
    private final PersonRepository people = mock(PersonRepository.class);
    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final OrganizationalUnitRepository units = mock(OrganizationalUnitRepository.class);
    private final ContactRepository contacts = mock(ContactRepository.class);
    private final PartyRoleRepository roles = mock(PartyRoleRepository.class);
    private final PartyDocumentRepository documents = mock(PartyDocumentRepository.class);
    private final ProfessionalQualificationRepository qualifications = mock(ProfessionalQualificationRepository.class);
    private final PartyMasterDataFacade facade = mock(PartyMasterDataFacade.class);
    private final TenantId tenant = TenantId.of("comandos");

    private CanonicalMasterDataWriteService writer() {
        var writer = new CanonicalMasterDataWriteService(em, people, organizations,
                units, contacts, roles, documents, qualifications, true);
        writer.setEnterprise(facade);
        return writer;
    }

    @Test void routesPeopleOrganizationsAndContactWritesToSharedFacade() {
        var writer = writer();
        var person = new Person();
        person.id = 1L;
        person.personType = "PF";
        person.fullName = "Pessoa Teste";
        person.active = true;
        var org = new Organization();
        org.id = 2L;
        org.name = "Organizacao Teste";
        org.active = true;
        org.publicOrganization = false;
        assertTrue(writer.usingSharedFacade());
        assertTrue(writer.synchronize(person));
        assertTrue(writer.synchronize(org));
        verify(facade).savePerson(any(com.fariamiguel.enterprise.people.Person.class));
        verify(facade).saveOrganization(any(com.fariamiguel.enterprise.organization.Organization.class));
        verify(people, never()).save(any());
        verify(organizations, never()).save(any());
    }

    @Test void rejectsUnpersistedIdentifierBeforeCanonicalWrite() {
        var writer = writer();
        var person = new Person();
        person.fullName = "Sem ID";
        assertThrows(IllegalStateException.class, () -> writer.synchronize(person));
        verifyNoInteractions(facade);
    }

    @Test void routesChildDeletesThroughTenantScopedCanonicalFacade() {
        var writer = writer();
        var address = new PersonAddress();
        address.id = 11L;
        var email = new PersonEmail();
        email.id = 12L;
        var phone = new PersonPhone();
        phone.id = 13L;
        var credential = new PersonCredential();
        credential.id = 14L;
        var qualification = new PersonQualification();
        qualification.id = 15L;
        assertTrue(writer.delete(address));
        assertTrue(writer.delete(email));
        assertTrue(writer.delete(phone));
        assertTrue(writer.delete(credential));
        assertTrue(writer.delete(qualification));
        verify(facade).deleteContact(tenant, BusinessId.of("comandos:person-address:11"));
        verify(facade).deleteContact(tenant, BusinessId.of("comandos:person-email:12"));
        verify(facade).deleteContact(tenant, BusinessId.of("comandos:person-phone:13"));
        verify(facade).deleteDocument(tenant, BusinessId.of("comandos:person-credential:14"));
        verify(facade).deleteQualification(tenant, BusinessId.of("comandos:person-qualification:15"));
        verifyNoInteractions(contacts, documents, qualifications);
    }
}
