package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.contact.ContactRepository;
import com.fariamiguel.enterprise.organization.OrganizationRepository;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyMasterDataFacade;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.enterprise.people.PersonRepository;
import com.fariamiguel.tenancy.api.OrganizationalUnitRepository;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CanonicalSharedDirectoryFacadeTests {
    private static final TenantId TENANT = TenantId.of("comandos");
    private static final BusinessId PERSON_ID = BusinessId.of("comandos:person:9");
    private static final PartyRef PERSON = new PartyRef(PERSON_ID, PartyKind.PERSON);

    @Test void canonicalContactsUseSharedFacadeForAllThreeContactTypes() {
        var facade = mock(PartyMasterDataFacade.class);
        var underlying = mock(ContactRepository.class);
        var entityManager = mock(EntityManager.class);
        var directory = new CanonicalContactDirectory(entityManager, underlying, true);
        directory.setEnterprise(facade);
        assertEquals(List.of(), directory.emails(9, TENANT));
        assertEquals(List.of(), directory.phones(9, TENANT));
        assertEquals(List.of(), directory.addresses(9, TENANT));
        verify(facade).emails(TENANT, PERSON);
        verify(facade).phones(TENANT, PERSON);
        verify(facade).addresses(TENANT, PERSON);
        verifyNoInteractions(underlying, entityManager);
    }

    @Test void canonicalPersonAndOrganizationLookupsUseSharedFacade() {
        var facade = mock(PartyMasterDataFacade.class);
        var people = mock(PersonRepository.class);
        var organizations = mock(OrganizationRepository.class);
        var manager = mock(EntityManager.class);
        var directory = new CanonicalMasterDataDirectory(
            manager,
            mock(CanonicalContactDirectory.class),
            people,
            organizations,
            mock(OrganizationalUnitRepository.class),
            mock(ContactRepository.class),
            true);
        directory.setEnterprise(facade);

        assertEquals(Optional.empty(), directory.findPerson(9, TENANT));
        assertEquals(Optional.empty(), directory.findOrganization(12, TENANT));
        verify(facade).findPerson(TENANT, PERSON_ID);
        verify(facade).findOrganization(TENANT, BusinessId.of("comandos:organization:12"));
        verifyNoInteractions(manager, people, organizations);
    }

    @Test void neverQueriesLegacyForInvalidIdentifiers() {
        var manager = mock(EntityManager.class);
        var directory = new CanonicalContactDirectory(manager, mock(ContactRepository.class), true);
        assertTrue(directory.addresses(0, TENANT).isEmpty());
        assertTrue(directory.phones(-3, TENANT).isEmpty());
        assertTrue(directory.emails(0, TENANT).isEmpty());
        verifyNoInteractions(manager);
    }
}
