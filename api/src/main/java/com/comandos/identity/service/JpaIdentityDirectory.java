package com.comandos.identity.service;

import com.comandos.core.service.CanonicalMasterDataDirectory;
import com.fariamiguel.enterprise.people.Person;
import com.fariamiguel.identity.api.IdentitySubject;
import com.fariamiguel.tenancy.api.TenantId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compatibility bridge between COMANDOS identity subjects and the canonical
 * Faria Miguel identity contract.
 *
 * <p>Identity resolution no longer reads the legacy COMANDOS Person entity
 * directly. The canonical master-data directory decides whether reads come
 * from legacy projection or canonical persistence according to the controlled
 * master-data cutover.</p>
 */
@Service
@Transactional(readOnly = true)
public class JpaIdentityDirectory
    implements com.fariamiguel.identity.api.IdentityDirectory {

    public static final String LOCAL_PROVIDER = "COMANDOS";
    private static final TenantId TENANT = TenantId.of("comandos");

    private final CanonicalMasterDataDirectory masterData;

    public JpaIdentityDirectory(CanonicalMasterDataDirectory masterData) {
        this.masterData = masterData;
    }

    @Override
    public Optional<IdentitySubject> find(String provider, String subject) {
        if (!LOCAL_PROVIDER.equalsIgnoreCase(normalize(provider))) {
            return Optional.empty();
        }

        Long personId = numericSubject(subject);
        if (personId == null) return Optional.empty();

        return masterData.findPerson(personId, TENANT)
            .map(person -> identitySubject(personId, person));
    }

    private static IdentitySubject identitySubject(long legacyPersonId, Person person) {
        Map<String, String> attributes = new LinkedHashMap<>(person.attributes());
        put(attributes, "legacyPersonId", String.valueOf(legacyPersonId));
        put(attributes, "taxId", person.taxId());
        put(attributes, "birthDate", person.birthDate() == null ? null : person.birthDate().toString());
        put(attributes, "phone", person.phones().stream().findFirst().orElse(null));
        put(attributes, "active", String.valueOf(person.status() == com.fariamiguel.enterprise.common.LifecycleStatus.ACTIVE));

        String email = person.emails().stream().findFirst().orElse(null);

        return new IdentitySubject(
            LOCAL_PROVIDER,
            String.valueOf(legacyPersonId),
            person.name(),
            email,
            attributes
        );
    }

    private static Long numericSubject(String value) {
        String normalized = normalize(value);
        if (normalized == null) return null;

        try {
            long id = Long.parseLong(normalized);
            return id > 0 ? id : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void put(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) target.put(key, value);
    }
}
