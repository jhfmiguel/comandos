package com.comandos.core.service;

import com.comandos.core.model.MasterDataReference;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistent registry that resolves legacy numeric master-data identifiers to
 * canonical Faria Miguel identifiers.
 */
@Service
public class MasterDataReferenceService {

    public static final String PERSON = "PERSON";
    public static final String ORGANIZATION = "ORGANIZATION";
    public static final String UNIT = "UNIT";
    public static final String ADDRESS = "ADDRESS";
    public static final String PHONE = "PHONE";
    public static final String EMAIL = "EMAIL";
    public static final String PARTY_ROLE = "PARTY_ROLE";
    public static final String CREDENTIAL = "CREDENTIAL";
    public static final String QUALIFICATION = "QUALIFICATION";

    private final EntityManager entityManager;

    public MasterDataReferenceService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public MasterDataReference upsert(
            String resourceType,
            long legacyId,
            String canonicalId,
            String note) {

        String type = normalizeType(resourceType);
        String canonical = required(canonicalId, "canonicalId");
        if (legacyId <= 0) {
            throw new IllegalArgumentException("legacyId must be positive");
        }

        MasterDataReference reference = findEntity(type, legacyId)
            .orElseGet(MasterDataReference::new);

        if (reference.id == null) {
            reference.resourceType = type;
            reference.legacyId = legacyId;
            entityManager.persist(reference);
        }

        reference.canonicalId = canonical;
        reference.canonicalTenantId = "comandos";
        reference.active = true;
        reference.lastSynchronizedAt = LocalDateTime.now();
        reference.migrationNote = normalizeNote(note);

        entityManager.flush();
        return reference;
    }

    @Transactional
    public boolean deactivate(String resourceType, long legacyId, String note) {
        Optional<MasterDataReference> existing =
            findEntity(normalizeType(resourceType), legacyId);

        if (existing.isEmpty()) return false;

        MasterDataReference reference = existing.orElseThrow();
        reference.active = false;
        reference.lastSynchronizedAt = LocalDateTime.now();
        reference.migrationNote = normalizeNote(note);
        entityManager.flush();
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<String> resolveCanonicalId(
            String resourceType,
            long legacyId) {

        return findEntity(normalizeType(resourceType), legacyId)
            .filter(reference -> Boolean.TRUE.equals(reference.active))
            .map(reference -> reference.canonicalId);
    }

    @Transactional(readOnly = true)
    public Optional<Long> resolveLegacyId(
            String resourceType,
            String canonicalId) {

        String type = normalizeType(resourceType);
        String canonical = required(canonicalId, "canonicalId");

        List<Long> result = entityManager.createQuery(
                "select r.legacyId from MasterDataReference r "
                    + "where r.resourceType=:type "
                    + "and r.canonicalId=:canonicalId "
                    + "and r.active=true",
                Long.class)
            .setParameter("type", type)
            .setParameter("canonicalId", canonical)
            .setMaxResults(2)
            .getResultList();

        if (result.size() > 1) {
            throw new NonUniqueResultException(
                "Multiple active master-data references for canonical id "
                    + canonical
            );
        }

        return result.stream().findFirst();
    }

    @Transactional(readOnly = true)
    public List<MasterDataReference> list(String resourceType, boolean activeOnly) {
        String type = normalizeType(resourceType);

        return entityManager.createQuery(
                "select r from MasterDataReference r "
                    + "where r.resourceType=:type "
                    + (activeOnly ? "and r.active=true " : "")
                    + "order by r.legacyId",
                MasterDataReference.class)
            .setParameter("type", type)
            .getResultList();
    }

    private Optional<MasterDataReference> findEntity(
            String resourceType,
            long legacyId) {

        return entityManager.createQuery(
                "select r from MasterDataReference r "
                    + "where r.resourceType=:type and r.legacyId=:legacyId",
                MasterDataReference.class)
            .setParameter("type", resourceType)
            .setParameter("legacyId", legacyId)
            .getResultStream()
            .findFirst();
    }

    private static String normalizeType(String value) {
        return required(value, "resourceType")
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalizeNote(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;
        return normalized.length() <= 500
            ? normalized
            : normalized.substring(0, 500);
    }
}
