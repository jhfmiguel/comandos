package com.comandos.architecture;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SharedEnterpriseFacadeOwnershipTest {
    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/com/comandos/core/service").resolve(file));
    }

    @Test void reusableMasterDataQueriesUseFariaMiguelFacade() throws Exception {
        String core = source("CanonicalMasterDataDirectory.java");
        String contacts = source("CanonicalContactDirectory.java");
        String snapshot = source("CanonicalPartySnapshotDirectory.java");
        String roles = source("CanonicalPartyRoleDirectory.java");
        String documents = source("CanonicalPartyDocumentDirectory.java");
        String qualifications = source("CanonicalProfessionalQualificationDirectory.java");
        assertTrue(core.contains("enterprise.searchPeople("));
        assertTrue(core.contains("enterprise.listOrganizations("));
        assertTrue(contacts.contains("enterprise.addresses("));
        assertTrue(snapshot.contains("enterprise.snapshot("));
        assertTrue(roles.contains("enterprise.rolesByType("));
        assertTrue(documents.contains("enterprise.documents("));
        assertTrue(qualifications.contains("enterprise.qualifications("));
    }

    @Test void sharedMasterDataMutationsStayBehindFacade() throws Exception {
        String writer = source("CanonicalMasterDataWriteService.java");
        assertTrue(writer.contains("enterprise.savePerson("));
        assertTrue(writer.contains("enterprise.saveOrganization("));
        assertTrue(writer.contains("enterprise.saveQualification("));
        assertTrue(writer.contains("enterprise.deleteQualification("));
        assertFalse(source("CoreService.java").contains("new CanonicalMasterDataWriteService("));
    }
}
