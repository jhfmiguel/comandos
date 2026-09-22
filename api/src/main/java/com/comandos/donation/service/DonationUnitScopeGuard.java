package com.comandos.donation.service;
import com.comandos.core.service.UnitScopeGuard;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
@Component public class DonationUnitScopeGuard implements UnitScopeGuard {
 private final EntityManager em; public DonationUnitScopeGuard(EntityManager em){this.em=em;}
 public void validateOrganizationChange(long unitId,long organizationId){long count=em.createQuery("select count(d) from Donation d where d.unit.id=:unit and d.organization.id<>:organization",Long.class).setParameter("unit",unitId).setParameter("organization",organizationId).getSingleResult();if(count>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The unit has donation records in its current organization.");}
}
