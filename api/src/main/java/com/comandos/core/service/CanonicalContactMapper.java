package com.comandos.core.service;

import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.fariamiguel.enterprise.common.Address;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.contact.ContactPurpose;
import com.fariamiguel.enterprise.contact.EmailContact;
import com.fariamiguel.enterprise.contact.PhoneContact;
import com.fariamiguel.enterprise.contact.PostalAddressContact;
import com.fariamiguel.enterprise.party.PartyKind;
import com.fariamiguel.enterprise.party.PartyRef;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.Locale;

/**
 * Converts COMANDOS legacy person contact rows to canonical Faria Miguel
 * enterprise contacts without changing the existing Oracle tables.
 */
public final class CanonicalContactMapper {

    private CanonicalContactMapper() {
    }

    public static PostalAddressContact address(PersonAddress source, TenantId tenantId) {
        requirePersisted(source == null ? null : source.id, "person address");
        requirePerson(source.person == null ? null : source.person.id);
        requireTenant(tenantId);

        String line1 = joinAddressLine(source.street, source.number);
        String line2 = blankToNull(source.complement);
        String countryCode = countryCode(source.country, Boolean.TRUE.equals(source.foreignAddress));

        return new PostalAddressContact(
            BusinessId.of("comandos:person-address:" + source.id),
            tenantId,
            party(source.person.id),
            purpose(source.contactType == null ? source.type : source.contactType.code),
            new Address(
                line1,
                line2,
                blankToNull(source.district),
                required(source.city, "city"),
                blankToNull(source.state),
                blankToNull(source.postalCode),
                countryCode
            ),
            Boolean.TRUE.equals(source.primaryAddress)
        );
    }

    public static PhoneContact phone(PersonPhone source, TenantId tenantId) {
        requirePersisted(source == null ? null : source.id, "person phone");
        requirePerson(source.person == null ? null : source.person.id);
        requireTenant(tenantId);

        String countryCode = blankToNull(source.countryCode);
        if (countryCode == null) countryCode = "55";

        return new PhoneContact(
            BusinessId.of("comandos:person-phone:" + source.id),
            tenantId,
            party(source.person.id),
            purpose(source.contactType == null ? source.type : source.contactType.code),
            countryCode,
            required(source.number, "phone number"),
            Boolean.TRUE.equals(source.whatsapp),
            Boolean.TRUE.equals(source.primaryPhone)
        );
    }

    public static EmailContact email(PersonEmail source, TenantId tenantId) {
        requirePersisted(source == null ? null : source.id, "person email");
        requirePerson(source.person == null ? null : source.person.id);
        requireTenant(tenantId);

        return new EmailContact(
            BusinessId.of("comandos:person-email:" + source.id),
            tenantId,
            party(source.person.id),
            purpose(source.contactType == null ? source.type : source.contactType.code),
            required(source.email, "email"),
            Boolean.TRUE.equals(source.primaryEmail)
        );
    }

    public static ContactPurpose purpose(String value) {
        String normalized = normalizeToken(value);
        if (normalized == null) return ContactPurpose.GENERAL;

        if (normalized.contains("EMERGEN")) return ContactPurpose.EMERGENCY;
        if (normalized.contains("COMERC") || normalized.contains("BUSINESS")) return ContactPurpose.COMMERCIAL;
        if (normalized.contains("COBR") || normalized.contains("BILL")) return ContactPurpose.BILLING;
        if (normalized.contains("FINANC")) return ContactPurpose.FINANCE;
        if (normalized.contains("TECN") || normalized.contains("TECH")) return ContactPurpose.TECHNICAL;
        if (normalized.contains("JUR") || normalized.contains("LEGAL")) return ContactPurpose.LEGAL;

        return ContactPurpose.GENERAL;
    }

    private static PartyRef party(Long personId) {
        return new PartyRef(
            BusinessId.of("comandos:person:" + personId),
            PartyKind.PERSON
        );
    }

    private static String joinAddressLine(String street, String number) {
        String normalizedStreet = required(street, "street");
        String normalizedNumber = required(number, "address number");
        return normalizedStreet + ", " + normalizedNumber;
    }

    private static String countryCode(String country, boolean foreignAddress) {
        String normalized = blankToNull(country);
        if (normalized == null) return foreignAddress ? "XX" : "BR";

        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.equals("BR") || upper.equals("BRA") || upper.equals("BRASIL") || upper.equals("BRAZIL")) {
            return "BR";
        }

        if (upper.length() == 2) return upper;

        // Preserve unknown legacy country information in a valid, explicit
        // migration placeholder. The original text remains in Oracle.
        return "XX";
    }

    private static String normalizeToken(String value) {
        String normalized = blankToNull(value);
        return normalized == null
            ? null
            : normalized.toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private static void requireTenant(TenantId tenantId) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
    }

    private static void requirePerson(Long personId) {
        requirePersisted(personId, "contact person");
    }

    private static void requirePersisted(Long id, String type) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(type + " must be persisted before canonical mapping");
        }
    }

    private static String required(String value, String field) {
        String normalized = blankToNull(value);
        if (normalized == null) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
