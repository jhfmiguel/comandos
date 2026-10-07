package com.comandos.core.service;

import com.comandos.core.model.PersonCredential;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.party.PartyDocument;
import com.fariamiguel.enterprise.party.PartyDocumentRepository;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Canonical directory for reusable person documents. */
@Service
@Transactional(readOnly = true)
public class CanonicalPartyDocumentDirectory {

    private final EntityManager entityManager;
    private final PartyDocumentRepository canonicalDocuments;
    private final boolean canonicalReadEnabled;

    public CanonicalPartyDocumentDirectory(
            EntityManager entityManager,
            PartyDocumentRepository canonicalDocuments,
            @Value("${comandos.master-data.canonical-read.enabled:false}")
            boolean canonicalReadEnabled) {
        this.entityManager = entityManager;
        this.canonicalDocuments = canonicalDocuments;
        this.canonicalReadEnabled = canonicalReadEnabled;
    }

    public List<PartyDocument> credentials(long personId, TenantId tenantId) {
        if (personId <= 0) return List.of();

        if (canonicalReadEnabled) {
            return canonicalDocuments.findByParty(tenantId, personRef(personId));
        }

        return entityManager.createQuery(
                "select c from PersonCredential c where c.person.id=:personId order by c.id",
                PersonCredential.class)
            .setParameter("personId", personId)
            .getResultList()
            .stream()
            .map(credential ->
                CanonicalPartyDocumentMapper.credential(credential, tenantId)
            )
            .toList();
    }

    private static PartyRef personRef(long personId) {
        return new PartyRef(
            BusinessId.of("comandos:person:" + personId),
            PartyKind.PERSON
        );
    }
}
