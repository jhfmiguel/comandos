package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:master-data-reference;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "comandos.demo.seed=false"
})
@Transactional
class MasterDataReferenceServiceTests {

    @Autowired MasterDataReferenceService references;
    @Autowired EntityManager entityManager;

    @Test
    void resolvesLegacyAndCanonicalIdentifiersBidirectionally() {
        references.upsert(
            MasterDataReferenceService.PERSON,
            42L,
            "comandos:person:42",
            "test"
        );

        assertEquals(
            "comandos:person:42",
            references.resolveCanonicalId(
                MasterDataReferenceService.PERSON,
                42L
            ).orElseThrow()
        );

        assertEquals(
            42L,
            references.resolveLegacyId(
                MasterDataReferenceService.PERSON,
                "comandos:person:42"
            ).orElseThrow()
        );
    }

    @Test
    void upsertIsIdempotentAndDeletionKeepsHistoricalCrosswalk() {
        var first = references.upsert(
            MasterDataReferenceService.ORGANIZATION,
            10L,
            "comandos:organization:10",
            "initial"
        );

        var updated = references.upsert(
            MasterDataReferenceService.ORGANIZATION,
            10L,
            "comandos:organization:10",
            "updated"
        );

        assertEquals(first.id, updated.id);
        assertTrue(Boolean.TRUE.equals(updated.active));

        assertTrue(references.deactivate(
            MasterDataReferenceService.ORGANIZATION,
            10L,
            "removed"
        ));

        assertFalse(references.resolveCanonicalId(
            MasterDataReferenceService.ORGANIZATION,
            10L
        ).isPresent());

        var history = references.list(
            MasterDataReferenceService.ORGANIZATION,
            false
        );
        assertEquals(1, history.size());
        assertFalse(Boolean.TRUE.equals(history.getFirst().active));
    }
}
