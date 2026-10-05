package com.comandos.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.comandos.core.model.Person;
import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonContactType;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.fariamiguel.enterprise.contact.ContactPurpose;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalContactMapperTests {

    private static final TenantId TENANT = TenantId.of("comandos");

    @Test
    void mapsRelationalAddressPhoneAndEmailToCanonicalContacts() {
        var person = person();

        var emergency = new PersonContactType();
        emergency.id = 1L;
        emergency.code = "EMERGENCY";
        emergency.name = "Emergency";

        var address = new PersonAddress();
        address.id = 10L;
        address.person = person;
        address.contactType = emergency;
        address.type = "HOME";
        address.street = "Rua 1";
        address.number = "100";
        address.complement = "Apto 2";
        address.district = "Centro";
        address.city = "Goiania";
        address.state = "GO";
        address.postalCode = "74000000";
        address.country = "Brasil";
        address.primaryAddress = true;
        address.archived = false;

        var phone = new PersonPhone();
        phone.id = 20L;
        phone.person = person;
        phone.contactType = emergency;
        phone.type = "MOBILE";
        phone.countryCode = "+55";
        phone.number = "(62) 99999-9999";
        phone.whatsapp = true;
        phone.primaryPhone = true;

        var email = new PersonEmail();
        email.id = 30L;
        email.person = person;
        email.contactType = emergency;
        email.type = "PERSONAL";
        email.email = "USER@EXAMPLE.COM";
        email.primaryEmail = true;

        var canonicalAddress = CanonicalContactMapper.address(address, TENANT);
        var canonicalPhone = CanonicalContactMapper.phone(phone, TENANT);
        var canonicalEmail = CanonicalContactMapper.email(email, TENANT);

        assertEquals("comandos:person-address:10", canonicalAddress.id().value());
        assertEquals("Rua 1, 100", canonicalAddress.address().line1());
        assertEquals("BR", canonicalAddress.address().countryCode());
        assertEquals(ContactPurpose.EMERGENCY, canonicalAddress.purpose());
        assertTrue(canonicalAddress.primary());

        assertEquals("+55", canonicalPhone.countryCode());
        assertEquals("6299999999", canonicalPhone.number());
        assertTrue(canonicalPhone.whatsapp());
        assertTrue(canonicalPhone.primary());

        assertEquals("user@example.com", canonicalEmail.email());
        assertTrue(canonicalEmail.primary());
    }

    @Test
    void enrichesCanonicalPersonWithRelationalContactsBeforeLegacyFallbacks() {
        var person = person();
        person.email = "legacy@example.com";
        person.phone = "111111111";

        var phone = new PersonPhone();
        phone.id = 21L;
        phone.person = person;
        phone.type = "MOBILE";
        phone.countryCode = "+55";
        phone.number = "62988887777";
        phone.whatsapp = false;
        phone.primaryPhone = true;

        var email = new PersonEmail();
        email.id = 31L;
        email.person = person;
        email.type = "WORK";
        email.email = "new@example.com";
        email.primaryEmail = true;

        var mapped = CanonicalMasterDataMapper.person(
            person,
            TENANT,
            List.of(),
            List.of(phone),
            List.of(email)
        );

        assertEquals(java.util.Set.of("new@example.com"), mapped.emails());
        assertEquals(java.util.Set.of("62988887777"), mapped.phones());
    }

    @Test
    void mapsKnownLegacyContactPurposesAndDefaultsUnknownOnes() {
        assertEquals(ContactPurpose.COMMERCIAL, CanonicalContactMapper.purpose("comercial"));
        assertEquals(ContactPurpose.BILLING, CanonicalContactMapper.purpose("cobranca"));
        assertEquals(ContactPurpose.FINANCE, CanonicalContactMapper.purpose("financeiro"));
        assertEquals(ContactPurpose.TECHNICAL, CanonicalContactMapper.purpose("tecnico"));
        assertEquals(ContactPurpose.LEGAL, CanonicalContactMapper.purpose("juridico"));
        assertEquals(ContactPurpose.GENERAL, CanonicalContactMapper.purpose("residencial"));
    }

    private static Person person() {
        var person = new Person();
        person.id = 7L;
        person.personType = "PF";
        person.fullName = "Pessoa Teste";
        person.active = true;
        return person;
    }
}
