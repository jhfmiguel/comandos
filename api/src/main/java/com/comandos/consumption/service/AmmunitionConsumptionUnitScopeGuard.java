package com.comandos.consumption.service;

import com.comandos.core.service.UnitScopeGuard;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AmmunitionConsumptionUnitScopeGuard implements UnitScopeGuard {
    private final EntityManager em;
    public AmmunitionConsumptionUnitScopeGuard(EntityManager em) { this.em = em; }
    @Override public void validateOrganizationChange(long unitId, long organizationId) {
        long count = em.createQuery("select count(c) from AmmunitionConsumption c where c.unit.id = :unit and c.organization.id <> :organization", Long.class)
            .setParameter("unit", unitId).setParameter("organization", organizationId).getSingleResult();
        if (count > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "The unit has ammunition consumption records in its current organization.");
    }
}
