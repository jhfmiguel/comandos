package com.comandos.identity.service;

import com.comandos.core.model.Person;
import com.fariamiguel.identity.api.IdentitySubject;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compatibility bridge between the legacy COMANDOS person identity lookup and
 * the canonical Faria Miguel identity contract.
 *
 * <p>The legacy interface remains implemented while callers are migrated. New
 * integrations should depend on {@link com.fariamiguel.identity.api.IdentityDirectory}.</p>
 */
@Service
@Transactional(readOnly = true)
public class JpaIdentityDirectory
    implements com.fariamiguel.identity.api.IdentityDirectory {

    public static final String LOCAL_PROVIDER = "COMANDOS";

    private final EntityManager entityManager;

    public JpaIdentityDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<IdentitySubject> find(String provider, String subject) {
        if (!LOCAL_PROVIDER.equalsIgnoreCase(normalize(provider))) {
            return Optional.empty();
        }

        Long personId = numericSubject(subject);
        if (personId == null) return Optional.empty();

        Person person = entityManager.find(Person.class, personId);
        if (person == null) return Optional.empty();

        Map<String, String> attributes = new LinkedHashMap<>();
        put(attributes, "legacyPersonId", String.valueOf(person.id));
        put(attributes, "personType", person.personType);
        put(attributes, "taxId", person.taxId);
        put(attributes, "birthDate", person.birthDate == null ? null : person.birthDate.toString());
        put(attributes, "phone", person.phone);
        put(attributes, "active", String.valueOf(Boolean.TRUE.equals(person.active)));

        return Optional.of(new IdentitySubject(
            LOCAL_PROVIDER,
            String.valueOf(person.id),
            person.fullName,
            person.email,
            attributes
        ));
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
