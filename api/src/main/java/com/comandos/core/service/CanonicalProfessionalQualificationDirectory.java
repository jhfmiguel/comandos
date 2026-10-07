package com.comandos.core.service;

import com.comandos.core.model.PersonQualification;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.enterprise.people.ProfessionalQualification;
import com.fariamiguel.enterprise.people.ProfessionalQualificationRepository;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Canonical directory for reusable professional qualifications. */
@Service
@Transactional(readOnly = true)
public class CanonicalProfessionalQualificationDirectory {

    private final EntityManager entityManager;
    private final ProfessionalQualificationRepository canonicalQualifications;
    private final boolean canonicalReadEnabled;

    public CanonicalProfessionalQualificationDirectory(
            EntityManager entityManager,
            ProfessionalQualificationRepository canonicalQualifications,
            @Value("${comandos.master-data.canonical-read.enabled:false}")
            boolean canonicalReadEnabled) {
        this.entityManager = entityManager;
        this.canonicalQualifications = canonicalQualifications;
        this.canonicalReadEnabled = canonicalReadEnabled;
    }

    public List<ProfessionalQualification> qualifications(
            long personId,
            TenantId tenantId) {

        if (personId <= 0) return List.of();

        if (canonicalReadEnabled) {
            return canonicalQualifications.findByPerson(
                tenantId,
                personRef(personId)
            );
        }

        return entityManager.createQuery(
                "select q from PersonQualification q "
                    + "where q.person.id=:personId order by q.id",
                PersonQualification.class)
            .setParameter("personId", personId)
            .getResultList()
            .stream()
            .map(source ->
                CanonicalProfessionalQualificationMapper.qualification(
                    source,
                    tenantId
                )
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
