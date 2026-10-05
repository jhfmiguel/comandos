package com.comandos.core.service;

import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyMasterDataService;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates the legacy COMANDOS master-data persistence into the canonical
 * Faria Miguel PartySnapshot shape for read-only migration consumers.
 */
@Service
@Transactional(readOnly = true)
public class CanonicalPartySnapshotDirectory {

    private final CanonicalMasterDataDirectory masterData;
    private final CanonicalContactDirectory contacts;
    private final CanonicalPartyRoleDirectory roles;
    private final CanonicalPartyDocumentDirectory documents;

    public CanonicalPartySnapshotDirectory(
            CanonicalMasterDataDirectory masterData,
            CanonicalContactDirectory contacts,
            CanonicalPartyRoleDirectory roles,
            CanonicalPartyDocumentDirectory documents) {
        this.masterData = masterData;
        this.contacts = contacts;
        this.roles = roles;
        this.documents = documents;
    }

    public Optional<PartyMasterDataService.PartySnapshot> person(
            long personId,
            TenantId tenantId) {

        return masterData.findPerson(personId, tenantId)
            .map(person -> {
                PartyRef party = new PartyRef(
                    BusinessId.of("comandos:person:" + personId),
                    PartyKind.PERSON
                );

                return new PartyMasterDataService.PartySnapshot(
                    party,
                    roles.roles(personId, tenantId),
                    contacts.emails(personId, tenantId),
                    contacts.phones(personId, tenantId),
                    contacts.addresses(personId, tenantId),
                    java.util.List.of(),
                    documents.credentials(personId, tenantId),
                    java.util.List.of()
                );
            });
    }
}
