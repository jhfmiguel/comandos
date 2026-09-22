package com.weaponsregistration.disposal.service;

import com.weaponsregistration.core.service.UnitScopeGuard;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DisposalUnitScopeGuard implements UnitScopeGuard {
    private final EntityManager em;
    public DisposalUnitScopeGuard(EntityManager em) { this.em = em; }
    public void validateOrganizationChange(long unitId, long organizationId) {
        long count = em.createQuery("select count(p) from DisposalProcess p where p.unit.id=:unit and p.organization.id<>:organization", Long.class)
            .setParameter("unit", unitId).setParameter("organization", organizationId).getSingleResult();
        if (count > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "The unit has disposal records in its current organization.");
    }
}
