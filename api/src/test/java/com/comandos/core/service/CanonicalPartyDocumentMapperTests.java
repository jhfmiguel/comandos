package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.comandos.core.model.Person;
import com.comandos.core.model.PersonCredential;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.enterprise.party.PartyDocumentType;
import com.fariamiguel.tenancy.api.TenantId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CanonicalPartyDocumentMapperTests {

    private static final TenantId TENANT = TenantId.of("comandos");

    @Test
    void mapsKnownCredentialTypesToCanonicalDocumentTypes() {
        assertEquals(PartyDocumentType.CPF, CanonicalPartyDocumentMapper.type("CPF"));
        assertEquals(PartyDocumentType.RG, CanonicalPartyDocumentMapper.type("identidade"));
        assertEquals(PartyDocumentType.CNH, CanonicalPartyDocumentMapper.type("Carteira Nacional de Habilitação"));
        assertEquals(PartyDocumentType.PASSPORT, CanonicalPartyDocumentMapper.type("passaporte"));
        assertEquals(PartyDocumentType.PROFESSIONAL_REGISTRATION,
            CanonicalPartyDocumentMapper.type("registro profissional"));
        assertEquals(PartyDocumentType.OTHER, CanonicalPartyDocumentMapper.type("credencial funcional"));
    }

    @Test
    void mapsLegacyCredentialWithoutLosingOriginalType() {
        var person = new Person();
        person.id = 10L;

        var source = new PersonCredential();
        source.id = 44L;
        source.person = person;
        source.type = "Credencial Funcional";
        source.number = "PPGO-123";
        source.validUntil = LocalDate.of(2030, 12, 31);

        var mapped = CanonicalPartyDocumentMapper.credential(source, TENANT);

        assertEquals("comandos:person-credential:44", mapped.id().value());
        assertEquals("comandos:person:10", mapped.party().id().value());
        assertEquals(PartyDocumentType.OTHER, mapped.type());
        assertEquals("PPGO-123", mapped.number());
        assertEquals(LocalDate.of(2030, 12, 31), mapped.validUntil());
        assertEquals(LifecycleStatus.ACTIVE, mapped.status());
        assertEquals("Credencial Funcional", mapped.attributes().get("legacyCredentialType"));
    }
}
