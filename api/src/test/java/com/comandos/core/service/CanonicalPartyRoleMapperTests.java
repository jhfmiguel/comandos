package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.comandos.core.model.Organization;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonRole;
import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.core.model.OrganizationalUnit;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.enterprise.party.PartyRoleType;
import com.fariamiguel.tenancy.api.TenantId;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalPartyRoleMapperTests {

    private static final TenantId TENANT = TenantId.of("comandos");

    @Test
    void mapsReusableBusinessRolesToCanonicalTypes() {
        assertEquals(PartyRoleType.CUSTOMER, CanonicalPartyRoleMapper.roleType(role("CLIENTE")).orElseThrow());
        assertEquals(PartyRoleType.SUPPLIER, CanonicalPartyRoleMapper.roleType(role("FORNECEDOR")).orElseThrow());
        assertEquals(PartyRoleType.PARTNER, CanonicalPartyRoleMapper.roleType(role("PARCEIRO")).orElseThrow());
        assertEquals(PartyRoleType.SERVICE_PROVIDER,
            CanonicalPartyRoleMapper.roleType(role("PRESTADOR_DE_SERVICO")).orElseThrow());
    }

    @Test
    void keepsSecuritySpecificRolesOutOfGenericFoundationContract() {
        assertTrue(CanonicalPartyRoleMapper.roleType(role("CUSTODIANTE_ARMAMENTO")).isEmpty());
        assertTrue(CanonicalPartyRoleMapper.roleType(role("AGENTE_ESCOLTA")).isEmpty());
    }

    @Test
    void mapsAssignmentAndPreservesLegacyScopeAndRoleData() {
        var assignment = assignment("CLIENTE", "ACTIVE");
        assignment.id = 90L;
        assignment.unit = unit(300L);
        assignment.startDate = LocalDate.of(2026, 1, 1);

        var mapped = CanonicalPartyRoleMapper.assignment(
            assignment,
            TENANT,
            Map.of("segment", "institutional", "priority", "high")
        ).orElseThrow();

        assertEquals("comandos:person-role-assignment:90", mapped.id().value());
        assertEquals("comandos:person:10", mapped.party().id().value());
        assertEquals(PartyRoleType.CUSTOMER, mapped.role());
        assertEquals(LifecycleStatus.ACTIVE, mapped.status());
        assertEquals("200", mapped.attributes().get("legacyOrganizationId"));
        assertEquals("300", mapped.attributes().get("legacyUnitId"));
        assertEquals("institutional", mapped.attributes().get("detail.segment"));
        assertEquals("high", mapped.attributes().get("detail.priority"));
    }

    @Test
    void mapsLegacyLifecycleStatesWithoutLosingMeaning() {
        assertEquals(LifecycleStatus.ACTIVE, CanonicalPartyRoleMapper.status("ATIVO", null));
        assertEquals(LifecycleStatus.INACTIVE, CanonicalPartyRoleMapper.status("SUSPENSO", null));
        assertEquals(LifecycleStatus.CLOSED, CanonicalPartyRoleMapper.status("ENCERRADO", null));
        assertEquals(LifecycleStatus.CANCELLED, CanonicalPartyRoleMapper.status("CANCELADO", null));
    }

    @Test
    void doesNotCanonicalizeUnsupportedAssignment() {
        var assignment = assignment("AGENTE_ESCOLTA", "ACTIVE");
        assignment.id = 91L;

        assertFalse(CanonicalPartyRoleMapper.assignment(assignment, TENANT, Map.of()).isPresent());
    }

    private static PersonRole role(String code) {
        var role = new PersonRole();
        role.id = 1L;
        role.code = code;
        role.name = code;
        return role;
    }

    private static PersonRoleAssignment assignment(String code, String status) {
        var assignment = new PersonRoleAssignment();
        assignment.person = new Person();
        assignment.person.id = 10L;
        assignment.role = role(code);
        assignment.organization = new Organization();
        assignment.organization.id = 200L;
        assignment.startDate = LocalDate.of(2026, 1, 1);
        assignment.status = status;
        return assignment;
    }

    private static OrganizationalUnit unit(long id) {
        var unit = new OrganizationalUnit();
        unit.id = id;
        return unit;
    }
}
