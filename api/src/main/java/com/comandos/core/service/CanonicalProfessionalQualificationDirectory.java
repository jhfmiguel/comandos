package com.comandos.core.service;

import com.comandos.core.model.PersonQualification;
import com.fariamiguel.enterprise.people.ProfessionalQualification;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Canonical read directory over legacy COMANDOS qualification rows. */
@Service
@Transactional(readOnly = true)
public class CanonicalProfessionalQualificationDirectory {

    private final EntityManager entityManager;

    public CanonicalProfessionalQualificationDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<ProfessionalQualification> qualifications(long personId, TenantId tenantId) {
        if (personId <= 0) return List.of();

        return entityManager.createQuery(
                "select q from PersonQualification q where q.person.id=:personId order by q.id",
                PersonQualification.class)
            .setParameter("personId", personId)
            .getResultList()
            .stream()
            .map(source -> CanonicalProfessionalQualificationMapper.qualification(source, tenantId))
            .toList();
    }
}
