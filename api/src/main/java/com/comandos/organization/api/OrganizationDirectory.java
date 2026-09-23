package com.comandos.organization.api;

import java.util.Optional;

public interface OrganizationDirectory {
    Optional<OrganizationView> findOrganization(long id);
    Optional<OrganizationalUnitView> findUnit(long id);
}
