package com.comandos.core.service;

import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.fariamiguel.enterprise.contact.EmailContact;
import com.fariamiguel.enterprise.contact.PhoneContact;
import com.fariamiguel.enterprise.contact.PostalAddressContact;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Canonical read view over the legacy COMANDOS person-contact tables.
 *
 * <p>This isolates JPA compatibility from consumers while the canonical Faria
 * Miguel ContactRepository persistence becomes the future source of truth.</p>
 */
@Service
@Transactional(readOnly = true)
public class CanonicalContactDirectory {

    private final EntityManager entityManager;

    public CanonicalContactDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<PostalAddressContact> addresses(long personId, TenantId tenantId) {
        if (personId <= 0) return List.of();

        return entityManager.createQuery(
                "select a from PersonAddress a "
                    + "where a.person.id=:personId and a.archived=false "
                    + "order by a.primaryAddress desc, a.id",
                PersonAddress.class)
            .setParameter("personId", personId)
            .getResultList()
            .stream()
            .map(address -> CanonicalContactMapper.address(address, tenantId))
            .toList();
    }

    public List<PhoneContact> phones(long personId, TenantId tenantId) {
        if (personId <= 0) return List.of();

        return entityManager.createQuery(
                "select p from PersonPhone p where p.person.id=:personId "
                    + "order by p.primaryPhone desc, p.id",
                PersonPhone.class)
            .setParameter("personId", personId)
            .getResultList()
            .stream()
            .map(phone -> CanonicalContactMapper.phone(phone, tenantId))
            .toList();
    }

    public List<EmailContact> emails(long personId, TenantId tenantId) {
        if (personId <= 0) return List.of();

        return entityManager.createQuery(
                "select e from PersonEmail e where e.person.id=:personId "
                    + "order by e.primaryEmail desc, e.id",
                PersonEmail.class)
            .setParameter("personId", personId)
            .getResultList()
            .stream()
            .map(email -> CanonicalContactMapper.email(email, tenantId))
            .toList();
    }
}
