package com.comandos.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.comandos.core.model.Person;
import com.comandos.core.model.PersonQualification;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.people.PersonRepository;
import com.fariamiguel.enterprise.people.ProfessionalQualificationRepository;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:master-data-migration;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false",
    "comandos.master-data.shadow-write.enabled=false",
    "comandos.master-data.readiness-on-startup=false",
    "comandos.master-data.backfill-on-startup=false",
    "comandos.demo.seed=false"
})
@Transactional
class MasterDataMigrationIntegrationTests {

    private static final TenantId TENANT = TenantId.of("comandos");

    @Autowired EntityManager entityManager;
    @Autowired MasterDataMigrationService migration;
    @Autowired MasterDataParityService parity;
    @Autowired PersonRepository people;
    @Autowired ProfessionalQualificationRepository qualifications;

    @Test
    void backfillsLegacyMasterDataEvenWhenAutomaticShadowWriteIsDisabled() {
        var person = new Person();
        person.personType = "PF";
        person.fullName = "Pessoa Migracao";
        person.taxId = "12345678909";
        person.birthDate = LocalDate.of(1990, 1, 1);
        person.active = true;
        entityManager.persist(person);
        entityManager.flush();

        var qualification = new PersonQualification();
        qualification.person = person;
        qualification.category = "Instrutor";
        qualification.validUntil = LocalDate.of(2030, 12, 31);
        qualification.status = "ACTIVE";
        entityManager.persist(qualification);
        entityManager.flush();

        var readiness = migration.readiness();
        assertThat(readiness.ready()).isTrue();

        var report = migration.backfill();

        assertThat(report.people()).isEqualTo(1);
        assertThat(report.qualifications()).isEqualTo(1);
        assertThat(report.mirroredTotal()).isGreaterThanOrEqualTo(2);

        var canonicalPerson = people.find(
            TENANT,
            BusinessId.of("comandos:person:" + person.id)
        );
        assertThat(canonicalPerson).isPresent();
        assertThat(canonicalPerson.orElseThrow().name())
            .isEqualTo("Pessoa Migracao");

        assertThat(qualifications.findByPerson(
            TENANT,
            new com.fariamiguel.enterprise.party.PartyRef(
                BusinessId.of("comandos:person:" + person.id),
                com.fariamiguel.enterprise.party.PartyKind.PERSON
            )
        )).hasSize(1);

        var parityReport = parity.verify();
        assertThat(parityReport.consistent()).isTrue();
        assertThat(parityReport.mismatches()).isEmpty();
    }
}
