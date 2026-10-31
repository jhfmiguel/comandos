package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.comandos.core.model.Person;
import com.comandos.core.model.PersonQualification;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.tenancy.api.TenantId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CanonicalProfessionalQualificationMapperTests {

    private static final TenantId TENANT = TenantId.of("comandos");

    @Test
    void mapsLegacyQualificationToCanonicalProfessionalQualification() {
        var person = new Person();
        person.id = 10L;

        var source = new PersonQualification();
        source.id = 77L;
        source.person = person;
        source.category = "Instrutor de Armamento";
        source.validUntil = LocalDate.of(2030, 6, 30);
        source.status = "ACTIVE";

        var mapped = CanonicalProfessionalQualificationMapper.qualification(source, TENANT);

        assertEquals("comandos:person-qualification:77", mapped.id().value());
        assertEquals("comandos:person:10", mapped.person().id().value());
        assertEquals("Instrutor de Armamento", mapped.category());
        assertEquals(LocalDate.of(2030, 6, 30), mapped.validUntil());
        assertEquals(LifecycleStatus.ACTIVE, mapped.status());
        assertEquals("ACTIVE", mapped.attributes().get("legacyStatus"));
    }

    @Test
    void preservesInactiveLegacyQualificationState() {
        var person = new Person();
        person.id = 11L;

        var source = new PersonQualification();
        source.id = 78L;
        source.person = person;
        source.category = "Certificacao";
        source.validUntil = LocalDate.of(2027, 1, 1);
        source.status = "SUSPENDED";

        var mapped = CanonicalProfessionalQualificationMapper.qualification(source, TENANT);

        assertEquals(LifecycleStatus.INACTIVE, mapped.status());
    }
}
