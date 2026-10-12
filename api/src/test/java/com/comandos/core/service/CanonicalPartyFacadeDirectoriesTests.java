package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.party.*;
import com.fariamiguel.enterprise.people.ProfessionalQualificationRepository;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalPartyFacadeDirectoriesTests {
    private final TenantId tenant = TenantId.of("comandos");
    private final PartyRef party = new PartyRef(BusinessId.of("comandos:person:7"), PartyKind.PERSON);
    private final EntityManager em = mock(EntityManager.class);
    private final PartyMasterDataFacade facade = mock(PartyMasterDataFacade.class);

    @Test void roleDirectoryQueriesSharedFacade() {
        var directory = new CanonicalPartyRoleDirectory(
            em, mock(PartyRoleRepository.class), facade, true);
        assertEquals(List.of(), directory.roles(7, tenant));
        verify(facade).roles(tenant, party);
        directory.roles(7, tenant, PartyRoleType.CUSTOMER);
        verify(facade).rolesByType(tenant, PartyRoleType.CUSTOMER);
        verifyNoInteractions(em);
    }

    @Test void credentialDirectoryQueriesSharedFacade() {
        var directory = new CanonicalPartyDocumentDirectory(
            em, mock(PartyDocumentRepository.class), facade, true);
        assertEquals(List.of(), directory.credentials(7, tenant));
        verify(facade).documents(tenant, party);
        verifyNoInteractions(em);
    }

    @Test void qualificationDirectoryQueriesSharedFacade() {
        var directory = new CanonicalProfessionalQualificationDirectory(
            em, mock(ProfessionalQualificationRepository.class), facade, true);
        assertEquals(List.of(), directory.qualifications(7, tenant));
        verify(facade).qualifications(tenant, party);
        verifyNoInteractions(em);
    }
}
