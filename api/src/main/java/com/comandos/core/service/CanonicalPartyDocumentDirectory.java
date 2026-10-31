package com.comandos.core.service;

import com.comandos.core.model.PersonCredential;
import com.fariamiguel.enterprise.party.PartyDocument;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Canonical read directory over legacy COMANDOS person credential rows.
 */
@Service
@Transactional(readOnly = true)
public class CanonicalPartyDocumentDirectory {

    private final EntityManager entityManager;

    public CanonicalPartyDocumentDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<PartyDocument> credentials(long personId, TenantId tenantId) {
        if (personId <= 0) return List.of();

        return entityManager.createQuery(
                "select c from PersonCredential c where c.person.id=:personId order by c.id",
                PersonCredential.class)
            .setParameter("personId", personId)
            .getResultList()
            .stream()
            .map(credential -> CanonicalPartyDocumentMapper.credential(credential, tenantId))
            .toList();
    }
}
