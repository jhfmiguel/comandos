package com.comandos.maintenance.service;

import com.comandos.core.service.UnitScopeGuard;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MaintenanceUnitScopeGuard implements UnitScopeGuard {
    private final EntityManager em;

    public MaintenanceUnitScopeGuard(EntityManager em) {
        this.em = em;
    }

    @Override
    public void validateOrganizationChange(long unit, long organization) {
        long count = em.createQuery(
                "select count(w) from WorkOrder w "
                    + "where w.unitLegacyId=:unit "
                    + "and w.organizationLegacyId<>:organization",
                Long.class)
            .setParameter("unit", unit)
            .setParameter("organization", organization)
            .getSingleResult();

        if (count > 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The unit has maintenance history."
            );
        }
    }
}
