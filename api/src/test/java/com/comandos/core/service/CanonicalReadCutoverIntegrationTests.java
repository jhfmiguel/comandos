package com.comandos.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.comandos.core.model.Person;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.people.PersonRepository;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:canonical-read-cutover;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false",
    "comandos.master-data.shadow-write.enabled=false",
    "comandos.master-data.canonical-read.enabled=true",
    "comandos.master-data.readiness-on-startup=false",
    "comandos.master-data.backfill-on-startup=false",
    "comandos.master-data.parity-on-startup=false",
    "comandos.demo.seed=false"
})
@Transactional
class CanonicalReadCutoverIntegrationTests {

    private static final TenantId TENANT = TenantId.of("comandos");

    @Autowired EntityManager entityManager;
    @Autowired MasterDataMigrationService migration;
    @Autowired MasterDataParityService parity;
    @Autowired PersonRepository people;
    @Autowired CanonicalCoreReadService reads;

    @Test
    void readsBusinessFieldsFromCanonicalPersistenceWhenCutoverIsEnabled() {
        var legacy = new Person();
        legacy.personType = "PF";
        legacy.fullName = "Nome Legado";
        legacy.taxId = "98765432100";
        legacy.birthDate = LocalDate.of(1991, 2, 3);
        legacy.active = true;

        entityManager.persist(legacy);
        entityManager.flush();

        var report = migration.backfill();
        assertThat(report.people()).isEqualTo(1);
        assertThat(parity.verify().consistent()).isTrue();

        var canonicalId = BusinessId.of("comandos:person:" + legacy.id);
        var canonical = people.find(TENANT, canonicalId).orElseThrow();

        people.save(new com.fariamiguel.enterprise.people.Person(
            canonical.id(),
            canonical.tenantId(),
            "Nome Canonico",
            canonical.taxId(),
            canonical.birthDate(),
            List.of(),
            Set.of(),
            Set.of(),
            canonical.status(),
            Map.copyOf(canonical.attributes())
        ));

        entityManager.clear();

        var view = reads.get("people", legacy.id);

        assertThat(view.get("id")).isEqualTo(legacy.id);
        assertThat(view.get("fullName")).isEqualTo("Nome Canonico");
        assertThat(
            entityManager.find(Person.class, legacy.id).fullName
        ).isEqualTo("Nome Legado");
    }
}
