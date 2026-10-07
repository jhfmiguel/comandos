package com.comandos.core.service;

import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.contact.ContactRepository;
import com.fariamiguel.enterprise.contact.EmailContact;
import com.fariamiguel.enterprise.contact.PhoneContact;
import com.fariamiguel.enterprise.contact.PostalAddressContact;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Canonical person-contact directory.
 *
 * <p>During migration it can project the legacy COMANDOS tables. Once the
 * canonical read cutover is enabled, reads come directly from the Faria Miguel
 * ContactRepository.</p>
 */
@Service
@Transactional(readOnly = true)
public class CanonicalContactDirectory {

    private final EntityManager entityManager;
    private final ContactRepository canonicalContacts;
    private final boolean canonicalReadEnabled;

    public CanonicalContactDirectory(
            EntityManager entityManager,
            ContactRepository canonicalContacts,
            @Value("${comandos.master-data.canonical-read.enabled:false}")
            boolean canonicalReadEnabled) {
        this.entityManager = entityManager;
        this.canonicalContacts = canonicalContacts;
        this.canonicalReadEnabled = canonicalReadEnabled;
    }

    public List<PostalAddressContact> addresses(long personId, TenantId tenantId) {
        if (personId <= 0) return List.of();

        if (canonicalReadEnabled) {
            return canonicalContacts.addresses(tenantId, personRef(personId));
        }

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

        if (canonicalReadEnabled) {
            return canonicalContacts.phones(tenantId, personRef(personId));
        }

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

        if (canonicalReadEnabled) {
            return canonicalContacts.emails(tenantId, personRef(personId));
        }

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

    private static PartyRef personRef(long personId) {
        return new PartyRef(
            BusinessId.of("comandos:person:" + personId),
            PartyKind.PERSON
        );
    }
}
