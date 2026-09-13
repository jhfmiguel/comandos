package com.weaponsregistration.inventory.service;

import com.weaponsregistration.core.service.UnitScopeGuard;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InventoryUnitScopeGuard implements UnitScopeGuard {
    private final EntityManager em;
    public InventoryUnitScopeGuard(EntityManager em) { this.em = em; }
    @Override
    public void validateOrganizationChange(long unitId, long organizationId) {
        long count = em.createQuery("select count(l) from StockLocation l where l.unit.id = :unit and l.organization.id <> :organization", Long.class)
            .setParameter("unit", unitId).setParameter("organization", organizationId).getSingleResult();
        if (count > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "The unit has stock locations in its current organization.");
        count = em.createQuery("select count(s) from EquipmentSet s where s.unit.id = :unit and s.organization.id <> :organization", Long.class)
            .setParameter("unit", unitId).setParameter("organization", organizationId).getSingleResult();
        if (count > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "The unit has equipment sets in its current organization.");
    }
}
