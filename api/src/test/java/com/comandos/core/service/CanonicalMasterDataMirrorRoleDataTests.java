package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.comandos.core.model.Organization;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonRole;
import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.core.model.RoleData;
import com.fariamiguel.enterprise.contact.ContactRepository;
import com.fariamiguel.enterprise.organization.OrganizationRepository;
import com.fariamiguel.enterprise.party.PartyDocumentRepository;
import com.fariamiguel.enterprise.party.PartyRoleAssignment;
import com.fariamiguel.enterprise.party.PartyRoleRepository;
import com.fariamiguel.enterprise.people.PersonRepository;
import com.fariamiguel.enterprise.people.ProfessionalQualificationRepository;
import com.fariamiguel.tenancy.api.OrganizationalUnitRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CanonicalMasterDataMirrorRoleDataTests {

    @Test
    void deletingRoleDetailRebuildsCanonicalRoleWithoutDeletedAttribute() {
        EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<RoleData> query = mock(TypedQuery.class);
        PartyRoleRepository roles = mock(PartyRoleRepository.class);

        PersonRoleAssignment assignment = new PersonRoleAssignment();
        assignment.id = 41L;
        assignment.person = new Person();
        assignment.person.id = 11L;
        assignment.organization = new Organization();
        assignment.organization.id = 12L;
        assignment.role = new PersonRole();
        assignment.role.id = 13L;
        assignment.role.code = "CUSTOMER";
        assignment.startDate = LocalDate.of(2026, 1, 1);
        assignment.status = "ACTIVE";

        RoleData retained = new RoleData();
        retained.id = 2L;
        retained.personRole = assignment;
        retained.key = "region";
        retained.value = "GO";

        RoleData removed = new RoleData();
        removed.id = 3L;
        removed.personRole = assignment;
        removed.key = "obsolete";
        removed.value = "delete-me";

        when(entityManager.createQuery(anyString(), eq(RoleData.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(retained));
        when(roles.save(any(PartyRoleAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CanonicalMasterDataMirrorService mirror = new CanonicalMasterDataMirrorService(
            entityManager,
            mock(PersonRepository.class),
            mock(OrganizationRepository.class),
            mock(OrganizationalUnitRepository.class),
            mock(ContactRepository.class),
            roles,
            mock(PartyDocumentRepository.class),
            mock(ProfessionalQualificationRepository.class),
            true
        );

        assertTrue(mirror.delete(removed));

        ArgumentCaptor<String> hql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createQuery(hql.capture(), eq(RoleData.class));
        assertTrue(hql.getValue().contains("d.id <> :excludedDetailId"));
        verify(query).setParameter("excludedDetailId", removed.id);
        ArgumentCaptor<PartyRoleAssignment> saved = ArgumentCaptor.forClass(PartyRoleAssignment.class);
        verify(roles).save(saved.capture());
        assertTrue(saved.getValue().attributes().containsKey("detail.region"));
        assertFalse(saved.getValue().attributes().containsKey("detail.obsolete"));
    }
}
