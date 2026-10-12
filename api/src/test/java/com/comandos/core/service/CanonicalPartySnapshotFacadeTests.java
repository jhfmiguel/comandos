package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.party.*;
import com.fariamiguel.enterprise.people.Person;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CanonicalPartySnapshotFacadeTests {
    @Test
    void canonicalModeUsesFariaMiguelFacadeWithoutLegacyQueries() {
        TenantId tenant = TenantId.of("comandos");
        PartyRef ref = new PartyRef(BusinessId.of("comandos:person:42"), PartyKind.PERSON);
        PartyMasterDataFacade facade = mock(PartyMasterDataFacade.class);
        when(facade.findPerson(tenant, ref.id())).thenReturn(Optional.of(mock(Person.class)));
        PartyMasterDataService.PartySnapshot expected =
            new PartyMasterDataService.PartySnapshot(ref, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
        when(facade.snapshot(tenant, ref)).thenReturn(expected);
        var masterData = mock(CanonicalMasterDataDirectory.class);
        var contacts = mock(CanonicalContactDirectory.class);
        var roles = mock(CanonicalPartyRoleDirectory.class);
        var documents = mock(CanonicalPartyDocumentDirectory.class);
        var directory = new CanonicalPartySnapshotDirectory(
            masterData, contacts, roles, documents, facade, true);

        assertEquals(Optional.of(expected), directory.person(42, tenant));
        assertEquals(Optional.empty(), directory.person(-1, tenant));
        verify(facade).snapshot(tenant, ref);
        verifyNoInteractions(masterData, contacts, roles, documents);
    }

    @Test
    void canonicalModeReturnsEmptyWhenPersonIsMissing() {
        TenantId tenant = TenantId.of("comandos");
        var facade = mock(PartyMasterDataFacade.class);
        var directory = new CanonicalPartySnapshotDirectory(
            mock(CanonicalMasterDataDirectory.class),
            mock(CanonicalContactDirectory.class),
            mock(CanonicalPartyRoleDirectory.class),
            mock(CanonicalPartyDocumentDirectory.class), facade, true);
        assertTrue(directory.person(99, tenant).isEmpty());
        verify(facade, never()).snapshot(any(), any());
    }
}
