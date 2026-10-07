package com.comandos.custody.service;

import com.comandos.core.service.UnitScopeGuard;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CustodyUnitScopeGuard implements UnitScopeGuard {
    private final EntityManager em;
    public CustodyUnitScopeGuard(EntityManager em) { this.em = em; }
    @Override
    public void validateOrganizationChange(long unitId, long organizationId) {
        long count = em.createQuery(
            "select count(c) from Custody c "
                + "where (c.unitLegacyId = :unit or c.recipientUnitLegacyId = :unit) "
                + "and c.organizationLegacyId <> :organization",
            Long.class)
            .setParameter("unit", unitId).setParameter("organization", organizationId).getSingleResult();
        if (count > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "The unit has custody records in its current organization.");
    }
}
