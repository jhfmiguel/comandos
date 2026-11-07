package com.comandos.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.comandos.security.service.ComandosAccessPolicy;
import com.fariamiguel.enterprise.contracts.Contract;
import com.fariamiguel.enterprise.contracts.ContractLegalDirectory;
import com.fariamiguel.enterprise.contracts.ContractLegalService;
import com.fariamiguel.enterprise.contracts.InMemoryContractLegalDirectory;
import com.fariamiguel.enterprise.contracts.ContractStatus;
import com.fariamiguel.security.api.CurrentActor;
import com.fariamiguel.security.api.CurrentActorProvider;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComandosContractLegalControllerTests {
    @Mock private ContractLegalService service;
    @Mock private ComandosAccessPolicy access;

    private ComandosContractLegalController controller;

    @BeforeEach
    void setUp() {
        CurrentActor actor = new CurrentActor("authenticated-actor", "User", Set.of(), Set.of(), Set.of(), "session", true);
        CurrentActorProvider actors = () -> actor;
        controller = new ComandosContractLegalController(service, access, actors);
    }

    @Test
    void createsDraftContractWithAuthenticatedActorAndServerOwnedScopeMetadata() {
        var request = new ComandosContractLegalController.CreateContract("contract-1", 44L, 3L,
                "supplier-1", "C-100", "Supply agreement", LocalDate.parse("2026-11-07"), null,
                null, null, ContractLegalDirectory.PartyRole.SUPPLIER,
                Map.of("organizationId", "999", "unitId", "999", "source", "procurement"));
        when(service.createContract(any(), eq(ContractLegalDirectory.PartyRole.SUPPLIER), eq("authenticated-actor")))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Contract saved = controller.create(request);

        verify(access).requireScope("contracts", "CREATE", 44L, 3L);
        assertEquals(ContractStatus.DRAFT, saved.status());
        assertEquals("44", saved.references().get("organizationId"));
        assertEquals("3", saved.references().get("unitId"));
        assertEquals("procurement", saved.references().get("source"));
        verify(service).createContract(any(), eq(ContractLegalDirectory.PartyRole.SUPPLIER), eq("authenticated-actor"));
    }

    @Test
    void listReturnsOnlyRecordsWithinAuthorizedOrganizationAndUnit() {
        var sameScope = contract("one", 44, 3L);
        var otherUnit = contract("two", 44, 4L);
        var otherOrganization = contract("three", 45, 3L);
        when(service.listContracts("comandos")).thenReturn(List.of(sameScope, otherUnit, otherOrganization));

        var rows = controller.contracts(44, 3L);

        assertEquals(List.of(sameScope), rows);
        verify(access).requireScope("contracts", "READ", 44L, 3L);
    }

    @Test
    void deniedScopePreventsAllContractReads() {
        doThrow(new org.springframework.security.access.AccessDeniedException("denied"))
                .when(access).requireScope("contracts", "READ", 44L, 3L);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.contracts(44, 3L));
        verify(service, never()).listContracts(any());
    }

    @Test
    void listsAmendmentsOnlyAfterVerifyingContractOrganizationAndUnit() {
        var contract = contract("one", 44, 3L);
        var directory = new InMemoryContractLegalDirectory();
        directory.save(new ContractLegalDirectory.Amendment("amend-1", "comandos", "one", 1,
                "Price update", LocalDate.parse("2026-12-01"), null, null, null,
                ContractLegalDirectory.AmendmentStatus.DRAFT, "requester", java.time.Instant.parse("2026-11-07T00:00:00Z"),
                null, null, null, Map.of()));
        when(service.findContract("comandos", "one")).thenReturn(java.util.Optional.of(contract));
        when(service.directory()).thenReturn(directory);

        var rows = controller.amendments("one", 44, 3L);

        assertEquals(1, rows.size());
        assertEquals("amend-1", rows.getFirst().id());
        verify(access).requireScope("contracts", "READ", 44L, 3L);
    }

    @Test
    void rejectsLegalMatterLinkedToContractOutsideSelectedOrganization() {
        var foreignContract = contract("foreign", 45, 3L);
        when(service.findContract("comandos", "foreign")).thenReturn(java.util.Optional.of(foreignContract));
        var request = new ComandosContractLegalController.MatterRequest("matter-1", 44, 3L,
                "L-1", ContractLegalDirectory.LegalMatterType.ADMINISTRATIVE, null, "Administrative case",
                null, "BR", "foreign", null, null, null, Map.of());

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.createMatter(request));
        verify(service, never()).saveMatter(any());
    }

    @Test
    void requiresExplicitPrivilegedScopeAndUsesAuthenticatedOpinionRequester() {
        var request = new ComandosContractLegalController.OpinionRequest("op-1", 44, 3L, "OP-1",
                "matter-1", null, "Privileged review", true, Map.of());
        var directory = new InMemoryContractLegalDirectory();
        directory.save(new ContractLegalDirectory.LegalMatter("matter-1", "comandos", "L-1",
                ContractLegalDirectory.LegalMatterType.ADMINISTRATIVE, null, "Administrative matter", null,
                "BR", ContractLegalDirectory.LegalMatterStatus.OPEN, null, null, null, null, null, null,
                Map.of("organizationId", "44", "unitId", "3")));
        when(service.directory()).thenReturn(directory);
        when(service.requestOpinion(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var opinion = controller.requestOpinion(request);

        verify(access).requireScope("legal/opinions", "CREATE", 44L, 3L);
        verify(access).requireScope("legal/opinions", "PRIVILEGED", 44L, 3L);
        assertEquals("authenticated-actor", opinion.requestedBy());
        assertEquals("44", opinion.metadata().get("organizationId"));
        assertEquals("3", opinion.metadata().get("unitId"));
    }

    @Test
    void filtersPrivilegedOpinionsForUsersWithoutSeparateGrant() {
        var ordinary = opinion("ordinary", false, 44, 3L);
        var privileged = opinion("private", true, 44, 3L);
        when(service.opinions("comandos", null, null)).thenReturn(List.of(ordinary, privileged));
        doAnswer(invocation -> {
            if ("PRIVILEGED".equals(invocation.getArgument(1)))
                throw new org.springframework.security.access.AccessDeniedException("denied");
            return null;
        }).when(access).requireScope(eq("legal/opinions"), anyString(), eq(44L), eq(3L));

        var visible = controller.opinions(44, 3L, null, null);

        assertEquals(List.of(ordinary), visible);
        verify(access).requireScope("legal/opinions", "READ", 44L, 3L);
    }

    @Test
    void indexedAdjustmentUsesAuthenticatedActorAndFinanceGrant() {
        when(service.findContract("comandos", "one")).thenReturn(java.util.Optional.of(contract("one", 44, 3L)));
        var request = new ComandosContractLegalController.AdjustmentRequest("IPCA",
                new java.math.BigDecimal("4.25"), LocalDate.parse("2027-01-01"), Map.of());

        controller.requestAdjustment("one", 44, 3L, request);

        verify(access).requireScope("contracts", "FINANCE", 44L, 3L);
        verify(service).requestAdjustment("comandos", "one", "IPCA", new java.math.BigDecimal("4.25"),
                LocalDate.parse("2027-01-01"), "authenticated-actor", Map.of());
    }

    @Test
    void rejectsAdjustmentProcessingOutsideTheOwnedUnit() {
        when(service.findContract("comandos", "one")).thenReturn(java.util.Optional.of(contract("one", 44, 4L)));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.applyDueAdjustments("one", 44, 3L));
        verify(service, never()).applyDueAdjustments(any(), any(String.class));
    }

    private static ContractLegalDirectory.LegalOpinion opinion(String id, boolean privileged, long organizationId, Long unitId) {
        Map<String,String> metadata = unitId == null ? Map.of("organizationId", Long.toString(organizationId))
                : Map.of("organizationId", Long.toString(organizationId), "unitId", Long.toString(unitId));
        return new ContractLegalDirectory.LegalOpinion(id, "comandos", id, "matter-1", null, id,
                "requester", null, null, ContractLegalDirectory.OpinionStatus.REQUESTED,
                java.time.Instant.parse("2026-11-07T00:00:00Z"), null, null, privileged, metadata);
    }

    private static Contract contract(String id, long organizationId, Long unitId) {
        Map<String,String> refs = unitId == null ? Map.of("organizationId", Long.toString(organizationId))
                : Map.of("organizationId", Long.toString(organizationId), "unitId", Long.toString(unitId));
        return new Contract(com.fariamiguel.enterprise.common.BusinessId.of(id), TenantId.of("comandos"),
                CompanyId.of(Long.toString(organizationId)), null, id, "Agreement", ContractStatus.DRAFT,
                LocalDate.parse("2026-11-07"), null, null, refs);
    }
}
