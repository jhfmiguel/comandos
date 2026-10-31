package com.comandos.core.service;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.Person;
import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonCredential;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import com.comandos.core.model.PersonQualification;
import com.comandos.core.model.PersonRoleAssignment;
import com.comandos.core.model.RoleData;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operational migration service for the COMANDOS master-data persistence
 * cutover from legacy erp_* tables to canonical Faria Miguel fm_* tables.
 */
@Service
public class MasterDataMigrationService {

    public record ReadinessIssue(
        String code,
        String severity,
        long count,
        String detail
    ) {}

    public record ReadinessReport(
        boolean ready,
        List<ReadinessIssue> issues
    ) {}

    public record BackfillReport(
        long people,
        long organizations,
        long units,
        long addresses,
        long phones,
        long emails,
        long roles,
        long roleDataRefreshes,
        long credentials,
        long qualifications,
        long skippedProductRoles
    ) {
        public long mirroredTotal() {
            return people + organizations + units + addresses + phones + emails
                + roles + roleDataRefreshes + credentials + qualifications;
        }
    }

    private final EntityManager entityManager;
    private final CanonicalMasterDataMirrorService mirror;

    public MasterDataMigrationService(
            EntityManager entityManager,
            CanonicalMasterDataMirrorService mirror) {
        this.entityManager = entityManager;
        this.mirror = mirror;
    }

    @Transactional(readOnly = true)
    public ReadinessReport readiness() {
        List<ReadinessIssue> issues = new ArrayList<>();

        blocker(
            issues,
            "DUPLICATE_PERSON_TAX_ID",
            duplicatePersonTaxIds(),
            "Duplicate person tax identifiers would violate the canonical tenant/tax-id uniqueness constraint."
        );

        blocker(
            issues,
            "DUPLICATE_ORGANIZATION_TAX_ID",
            duplicateOrganizationTaxIds(),
            "Duplicate organization tax identifiers would violate the canonical tenant/tax-id uniqueness constraint."
        );

        blocker(
            issues,
            "INVALID_PERSON_ADDRESS",
            count(
                "select count(a) from PersonAddress a "
                    + "where a.archived=false and "
                    + "(a.person is null or a.street is null or a.number is null or a.city is null)"
            ),
            "Active addresses require person, street, number and city for canonical mapping."
        );

        blocker(
            issues,
            "INVALID_PERSON_PHONE",
            count(
                "select count(p) from PersonPhone p "
                    + "where p.person is null or p.number is null"
            ),
            "Phones require person and number for canonical mapping."
        );

        blocker(
            issues,
            "INVALID_PERSON_EMAIL",
            count(
                "select count(e) from PersonEmail e "
                    + "where e.person is null or e.email is null"
            ),
            "Emails require person and address value for canonical mapping."
        );

        blocker(
            issues,
            "INVALID_ORGANIZATIONAL_UNIT",
            count(
                "select count(u) from OrganizationalUnit u "
                    + "where u.organization is null or u.code is null or u.name is null"
            ),
            "Organizational units require organization, code and name."
        );

        blocker(
            issues,
            "INVALID_PERSON_CREDENTIAL",
            count(
                "select count(c) from PersonCredential c "
                    + "where c.person is null or c.number is null"
            ),
            "Credentials require person and number."
        );

        blocker(
            issues,
            "INVALID_PERSON_QUALIFICATION",
            count(
                "select count(q) from PersonQualification q "
                    + "where q.person is null or q.category is null"
            ),
            "Professional qualifications require person and category."
        );

        long specificRoles = entityManager.createQuery(
                "select count(a) from PersonRoleAssignment a "
                    + "where upper(a.role.code) not in "
                    + "('CUSTOMER','CLIENT','CLIENTE','SUPPLIER','FORNECEDOR',"
                    + "'PARTNER','PARCEIRO','SERVICE_PROVIDER','SERVICEPROVIDER',"
                    + "'PRESTADOR','PRESTADOR_DE_SERVICO','PRESTADOR_SERVICO','PROVIDER')",
                Long.class)
            .getSingleResult();

        if (specificRoles > 0) {
            issues.add(new ReadinessIssue(
                "PRODUCT_SPECIFIC_ROLES",
                "INFO",
                specificRoles,
                "Security-domain roles stay COMANDOS-owned and are intentionally not copied to the generic PartyRoleType enum."
            ));
        }

        boolean ready = issues.stream()
            .noneMatch(issue -> "BLOCKER".equals(issue.severity()));

        return new ReadinessReport(ready, List.copyOf(issues));
    }

    /**
     * Idempotent because canonical repository save operations use stable
     * compatibility business identifiers derived from legacy IDs.
     */
    @Transactional
    public BackfillReport backfill() {
        ReadinessReport readiness = readiness();
        if (!readiness.ready()) {
            throw new IllegalStateException(
                "Master-data backfill is blocked by legacy data compatibility issues: "
                    + readiness.issues().stream()
                        .filter(issue -> "BLOCKER".equals(issue.severity()))
                        .map(issue -> issue.code() + "=" + issue.count())
                        .toList()
            );
        }

        long people = mirrorAll(Person.class);
        long organizations = mirrorAll(Organization.class);
        long units = mirrorAll(OrganizationalUnit.class);
        long addresses = mirrorAllActiveAddresses();
        long phones = mirrorAll(PersonPhone.class);
        long emails = mirrorAll(PersonEmail.class);

        long roles = 0;
        long skippedProductRoles = 0;
        for (PersonRoleAssignment assignment : all(PersonRoleAssignment.class)) {
            if (mirror.mirror(assignment)) roles++;
            else skippedProductRoles++;
        }

        long roleDataRefreshes = 0;
        for (RoleData data : all(RoleData.class)) {
            if (mirror.mirror(data)) roleDataRefreshes++;
        }

        long credentials = mirrorAll(PersonCredential.class);
        long qualifications = mirrorAll(PersonQualification.class);

        return new BackfillReport(
            people,
            organizations,
            units,
            addresses,
            phones,
            emails,
            roles,
            roleDataRefreshes,
            credentials,
            qualifications,
            skippedProductRoles
        );
    }

    private long mirrorAllActiveAddresses() {
        long mirrored = 0;
        var rows = entityManager.createQuery(
                "select a from PersonAddress a where a.archived=false order by a.id",
                PersonAddress.class)
            .getResultList();

        for (PersonAddress entity : rows) {
            if (mirror.mirror(entity)) mirrored++;
        }
        return mirrored;
    }

    private <T extends com.comandos.core.model.CoreEntity> long mirrorAll(
            Class<T> type) {
        long mirrored = 0;
        for (T entity : all(type)) {
            if (mirror.mirror(entity)) mirrored++;
        }
        return mirrored;
    }

    private <T> List<T> all(Class<T> type) {
        return entityManager.createQuery(
                "select e from " + type.getSimpleName() + " e order by e.id",
                type)
            .getResultList();
    }

    private long duplicatePersonTaxIds() {
        return entityManager.createQuery(
                "select count(taxId) from Person p "
                    + "where p.taxId is not null and trim(p.taxId) <> '' "
                    + "group by lower(p.taxId) having count(p.id) > 1",
                Long.class)
            .getResultList()
            .stream()
            .mapToLong(value -> 1L)
            .sum();
    }

    private long duplicateOrganizationTaxIds() {
        return entityManager.createQuery(
                "select count(taxId) from Organization o "
                    + "where o.taxId is not null and trim(o.taxId) <> '' "
                    + "group by lower(o.taxId) having count(o.id) > 1",
                Long.class)
            .getResultList()
            .stream()
            .mapToLong(value -> 1L)
            .sum();
    }

    private long count(String jpql) {
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    private static void blocker(
            List<ReadinessIssue> issues,
            String code,
            long count,
            String detail) {
        if (count > 0) {
            issues.add(new ReadinessIssue(code, "BLOCKER", count, detail));
        }
    }
}
