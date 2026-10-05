package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.comandos.core.model.EconomicActivity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationNature;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.OrganizationalUnitType;
import com.comandos.core.model.Person;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CanonicalMasterDataMapperTests {

    private static final TenantId TENANT = TenantId.of("comandos");
    private static final CompanyId COMPANY = CompanyId.of("comandos:organization:10");

    @Test
    void mapsLegacyPersonToCanonicalEnterprisePerson() {
        var source = new Person();
        source.id = 25L;
        source.personType = "PF";
        source.fullName = "Ana Operadora";
        source.taxId = "12345678900";
        source.birthDate = LocalDate.of(1990, 5, 10);
        source.email = "ana@example.com";
        source.phone = "62999999999";
        source.active = true;

        var mapped = CanonicalMasterDataMapper.person(source, TENANT);

        assertEquals("comandos:person:25", mapped.id().value());
        assertEquals(TENANT, mapped.tenantId());
        assertEquals("Ana Operadora", mapped.name());
        assertEquals("12345678900", mapped.taxId());
        assertEquals(LocalDate.of(1990, 5, 10), mapped.birthDate());
        assertEquals(LifecycleStatus.ACTIVE, mapped.status());
        assertEquals(java.util.Set.of("ana@example.com"), mapped.emails());
        assertEquals(java.util.Set.of("62999999999"), mapped.phones());
        assertEquals("PF", mapped.attributes().get("legacyPersonType"));
    }

    @Test
    void mapsLegacyOrganizationWithoutChangingPersistenceIdentity() {
        var nature = new OrganizationNature();
        nature.id = 1L;
        nature.code = "PUBLIC";
        nature.name = "Public organization";

        var activity = new EconomicActivity();
        activity.id = 2L;
        activity.code = "SECURITY";
        activity.description = "Public security";

        var source = new Organization();
        source.id = 10L;
        source.nature = nature;
        source.economicActivity = activity;
        source.name = "Policia Penal";
        source.acronym = "PPGO";
        source.taxId = "00123456000199";
        source.publicOrganization = true;
        source.active = true;

        var mapped = CanonicalMasterDataMapper.organization(source, TENANT, COMPANY);

        assertEquals("comandos:organization:10", mapped.id().value());
        assertEquals("Policia Penal", mapped.legalName());
        assertEquals("PPGO", mapped.tradeName());
        assertEquals("Public organization", mapped.attributes().get("nature"));
        assertEquals("Public security", mapped.attributes().get("economicActivity"));
        assertEquals("true", mapped.attributes().get("publicOrganization"));
        assertEquals(LifecycleStatus.ACTIVE, mapped.status());
    }

    @Test
    void mapsLegacyUnitAndFallsBackToOtherForProductSpecificUnitTypes() {
        var organization = new Organization();
        organization.id = 10L;
        organization.name = "Policia Penal";

        var type = new OrganizationalUnitType();
        type.id = 5L;
        type.code = "REGIONAL_COMMAND";
        type.name = "Regional command";

        var parent = new OrganizationalUnit();
        parent.id = 100L;
        parent.organization = organization;
        parent.code = "ROOT";
        parent.name = "Root";
        parent.type = "OTHER";
        parent.active = true;

        var source = new OrganizationalUnit();
        source.id = 101L;
        source.organization = organization;
        source.parentUnit = parent;
        source.code = "UNIT-01";
        source.name = "Operational Unit";
        source.unitType = type;
        source.type = type.name;
        source.active = false;

        var mapped = CanonicalMasterDataMapper.unit(source, TENANT, COMPANY);

        assertEquals("comandos:unit:101", mapped.id().value());
        assertEquals("comandos:unit:100", mapped.parentUnitId().value());
        assertEquals("UNIT-01", mapped.code());
        assertEquals("Operational Unit", mapped.name());
        assertEquals(com.fariamiguel.tenancy.api.OrganizationalUnitType.OTHER, mapped.type());
        assertFalse(mapped.active());
        assertEquals("Regional command", mapped.attributes().get("legacyType"));
    }

    @Test
    void keepsMissingOptionalOrganizationTradeNameCanonical() {
        var source = new Organization();
        source.id = 20L;
        source.name = "Organization Without Acronym";
        source.legacyNature = "PRIVATE";
        source.active = false;

        var mapped = CanonicalMasterDataMapper.organization(
            source,
            TENANT,
            CompanyId.of("comandos:organization:20")
        );

        assertEquals("Organization Without Acronym", mapped.tradeName());
        assertEquals(LifecycleStatus.INACTIVE, mapped.status());
        assertEquals("PRIVATE", mapped.attributes().get("nature"));
    }
}
