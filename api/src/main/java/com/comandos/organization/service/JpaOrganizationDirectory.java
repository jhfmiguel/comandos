package com.comandos.organization.service;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.organization.api.OrganizationDirectory;
import com.comandos.organization.api.OrganizationView;
import com.comandos.organization.api.OrganizationalUnitView;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JpaOrganizationDirectory implements OrganizationDirectory {

    private final EntityManager entityManager;

    public JpaOrganizationDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<OrganizationView> findOrganization(long id) {
        Organization organization = entityManager.find(Organization.class, id);

        if (organization == null) return Optional.empty();

        return Optional.of(new OrganizationView(
            organization.id,
            organization.nature,
            organization.name,
            organization.acronym,
            organization.taxId,
            Boolean.TRUE.equals(organization.publicOrganization),
            Boolean.TRUE.equals(organization.active)
        ));
    }

    @Override
    public Optional<OrganizationalUnitView> findUnit(long id) {
        OrganizationalUnit unit = entityManager.find(OrganizationalUnit.class, id);

        if (unit == null) return Optional.empty();

        return Optional.of(new OrganizationalUnitView(
            unit.id,
            unit.organization.id,
            unit.parentUnit == null ? null : unit.parentUnit.id,
            unit.code,
            unit.name,
            unit.type,
            Boolean.TRUE.equals(unit.active)
        ));
    }
}
