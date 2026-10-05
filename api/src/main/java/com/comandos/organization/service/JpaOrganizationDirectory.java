package com.comandos.organization.service;

import com.comandos.core.service.CanonicalMasterDataDirectory;
import com.comandos.organization.api.OrganizationDirectory;
import com.comandos.organization.api.OrganizationView;
import com.comandos.organization.api.OrganizationalUnitView;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legacy organization API adapter backed by the canonical master-data bridge.
 *
 * <p>The local API remains only for source compatibility. New consumers should
 * use Faria Miguel master-data contracts directly.</p>
 */
@Service
@Transactional(readOnly = true)
public class JpaOrganizationDirectory implements OrganizationDirectory {

    private static final TenantId TENANT = TenantId.of("comandos");

    private final CanonicalMasterDataDirectory canonical;

    public JpaOrganizationDirectory(CanonicalMasterDataDirectory canonical) {
        this.canonical = canonical;
    }

    @Override
    public Optional<OrganizationView> findOrganization(long id) {
        return canonical.findOrganization(id, TENANT)
            .map(organization -> new OrganizationView(
                CanonicalMasterDataDirectory.legacyOrganizationId(organization.id()),
                organization.attributes().get("nature"),
                organization.attributes().get("economicActivity"),
                organization.legalName(),
                organization.tradeName(),
                organization.taxId(),
                Boolean.parseBoolean(
                    organization.attributes().getOrDefault(
                        "publicOrganization",
                        "false"
                    )
                ),
                organization.status()
                    == com.fariamiguel.enterprise.common.LifecycleStatus.ACTIVE
            ));
    }

    @Override
    public Optional<OrganizationalUnitView> findUnit(long id) {
        return canonical.findUnit(id, TENANT)
            .map(unit -> new OrganizationalUnitView(
                CanonicalMasterDataDirectory.legacyUnitId(unit.id()),
                Long.parseLong(unit.attributes().get("legacyOrganizationId")),
                unit.parentUnitId() == null
                    ? null
                    : CanonicalMasterDataDirectory.legacyUnitId(unit.parentUnitId()),
                unit.code(),
                unit.name(),
                unit.attributes().get("legacyType"),
                unit.active()
            ));
    }
}
