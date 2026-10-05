package com.comandos.organization.api;

import java.util.Optional;

@Deprecated(forRemoval = true)
public interface OrganizationDirectory {
    Optional<OrganizationView> findOrganization(long id);
    Optional<OrganizationalUnitView> findUnit(long id);
}
