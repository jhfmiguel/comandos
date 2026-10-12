package com.comandos.core.service;

import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyMasterDataService;
import com.fariamiguel.enterprise.party.PartyMasterDataFacade;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final PartyMasterDataFacade enterprise;
    private final boolean canonicalReadEnabled;

    public CanonicalPartySnapshotDirectory(
            CanonicalMasterDataDirectory masterData,
            CanonicalContactDirectory contacts,
            CanonicalPartyRoleDirectory roles,
            CanonicalPartyDocumentDirectory documents) {
        this.masterData = masterData;
        this.contacts = contacts;
        this.roles = roles;
        this.documents = documents;
        this.enterprise = null;
        this.canonicalReadEnabled = false;
    }

    @Autowired
    public CanonicalPartySnapshotDirectory(
            CanonicalMasterDataDirectory masterData,
            CanonicalContactDirectory contacts,
            CanonicalPartyRoleDirectory roles,
            CanonicalPartyDocumentDirectory documents,
            PartyMasterDataFacade enterprise,
            @Value("${comandos.master-data.canonical-read.enabled:false}")
            boolean canonicalReadEnabled) {
        this.masterData = masterData;
        this.contacts = contacts;
        this.roles = roles;
        this.documents = documents;
        this.enterprise = enterprise;
        this.canonicalReadEnabled = canonicalReadEnabled;
    }

    public Optional<PartyMasterDataService.PartySnapshot> person(
            long personId,
            TenantId tenantId) {

        if (personId <= 0) return Optional.empty();
        if (canonicalReadEnabled) {
            if (enterprise == null) throw new IllegalStateException("Canonical master-data facade is required");
            PartyRef ref = new PartyRef(BusinessId.of("comandos:person:" + personId), PartyKind.PERSON);
            return enterprise.findPerson(tenantId, ref.id()).map(ignored -> enterprise.snapshot(tenantId, ref));
        }

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
