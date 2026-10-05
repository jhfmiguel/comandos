package com.comandos.core.service;

import com.comandos.core.model.CoreEntity;
import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.Person;
import com.comandos.security.service.AccessPolicy;
import com.fariamiguel.enterprise.common.LifecycleStatus;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Compatibility read service that serves the existing COMANDOS Core HTTP shape
 * while deriving master-data values from canonical Faria Miguel contracts.
 *
 * <p>Writes remain in {@link CoreService} until shared persistence is cut over.
 * Reads for people, organizations and units are isolated here so the generic
 * CoreService can be reduced incrementally without breaking the frontend.</p>
 */
@Service
@Transactional(readOnly = true)
public class CanonicalCoreReadService {

    private static final TenantId TENANT = TenantId.of("comandos");
    private static final Set<String> SUPPORTED = Set.of(
        "people",
        "organizations",
        "units"
    );

    private final EntityManager entityManager;
    private final AccessPolicy access;

    public CanonicalCoreReadService(
            EntityManager entityManager,
            AccessPolicy access) {
        this.entityManager = entityManager;
        this.access = access;
    }

    public boolean supports(String resource) {
        return SUPPORTED.contains(resource);
    }

    public CorePageResult list(
            String resource,
            String search,
            int page,
            int size,
            Long organizationId,
            Map<String, String> requestParams) {

        var spec = supported(resource);

        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            bad("Invalid pagination.");
        }

        List<String> clauses = new ArrayList<>();
        Map<String, Object> parameters = new LinkedHashMap<>();

        if (search != null && !search.isBlank()) {
            List<String> expressions = new ArrayList<>();
            expressions.add("cast(e.id as string)");

            spec.fields().stream()
                .filter(field -> Set.of("text", "email", "choice").contains(field.type()))
                .forEach(field -> expressions.add("lower(e." + field.property() + ")"));

            clauses.add(
                "(" + String.join(
                    " or ",
                    expressions.stream()
                        .map(expression -> expression + " like :globalSearch escape '!'")
                        .toList()
                ) + ")"
            );
            parameters.put("globalSearch", likeTerm(search));
        }

        int filterIndex = 0;
        for (var entry : requestParams.entrySet()) {
            if (!entry.getKey().startsWith("filter.")) continue;

            String fieldName = entry.getKey().substring("filter.".length());
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (value.isBlank()) continue;

            String parameter = "columnFilter" + filterIndex++;

            if ("id".equals(fieldName)) {
                clauses.add("cast(e.id as string) like :" + parameter + " escape '!'");
                parameters.put(parameter, likeTerm(value));
                continue;
            }

            var field = spec.fields().stream()
                .filter(candidate -> candidate.name().equals(fieldName))
                .findFirst()
                .orElse(null);

            if (field == null || "password".equals(field.type())) continue;

            List<String> expressions = new ArrayList<>();

            if ("reference".equals(field.type())) {
                String relation = "e." + field.property();
                expressions.add("cast(" + relation + ".id as string)");

                var target = CoreCatalog.get(field.reference());
                target.fields().stream()
                    .filter(targetField ->
                        Set.of("text", "email", "choice").contains(targetField.type())
                    )
                    .limit(4)
                    .forEach(targetField ->
                        expressions.add(
                            "lower(" + relation + "." + targetField.property() + ")"
                        )
                    );
            } else {
                expressions.add(
                    "lower(cast(e." + field.property() + " as string))"
                );
            }

            clauses.add(
                "(" + String.join(
                    " or ",
                    expressions.stream()
                        .map(expression ->
                            expression + " like :" + parameter + " escape '!'"
                        )
                        .toList()
                ) + ")"
            );
            parameters.put(parameter, likeTerm(value));
        }

        if (organizationId != null && spec.entity() == OrganizationalUnit.class) {
            clauses.add("e.organization.id = :organizationId");
            parameters.put("organizationId", organizationId);
        }

        clauses.add(access.predicate("core/" + resource, "READ", "e"));

        String where = " where " + String.join(" and ", clauses);

        var query = entityManager.createQuery(
            "select e from " + spec.entity().getSimpleName() + " e"
                + where + " order by e.id",
            spec.entity()
        );

        var count = entityManager.createQuery(
            "select count(e) from " + spec.entity().getSimpleName() + " e" + where,
            Long.class
        );

        parameters.forEach((name, value) -> {
            query.setParameter(name, value);
            count.setParameter(name, value);
        });

        return new CorePageResult(
            query.setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(entity -> view(resource, (CoreEntity) entity))
                .toList(),
            count.getSingleResult(),
            page,
            size
        );
    }

    public Map<String, Object> get(String resource, long id) {
        var spec = supported(resource);
        access.requireAny("core/" + resource, "READ");

        CoreEntity entity = entityManager.find(spec.entity(), id);
        if (entity == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                spec.label() + ": record not found."
            );
        }

        access.requireEntity("core/" + resource, "READ", entity);
        return view(resource, entity);
    }

    private Map<String, Object> view(String resource, CoreEntity entity) {
        return switch (resource) {
            case "people" -> personView((Person) entity);
            case "organizations" -> organizationView((Organization) entity);
            case "units" -> unitView((OrganizationalUnit) entity);
            default -> throw new IllegalArgumentException(
                "Unsupported canonical Core read resource: " + resource
            );
        };
    }

    private Map<String, Object> personView(Person source) {
        var canonical = CanonicalMasterDataMapper.person(source, TENANT);
        Map<String, Object> result = metadata(source, canonical.name());

        result.put(
            "personTypeRefId",
            source.personTypeRef == null ? null : source.personTypeRef.id
        );
        result.put("fullName", canonical.name());
        result.put("taxId", canonical.taxId());
        result.put("birthDate", canonical.birthDate());
        result.put("active", canonical.status() == LifecycleStatus.ACTIVE);
        result.put(
            "personTypeCode",
            source.personTypeRef == null
                ? source.personType
                : source.personTypeRef.code
        );

        Map<String, String> labels = new LinkedHashMap<>();
        if (source.personTypeRef != null) {
            labels.put(
                "personTypeRefId",
                source.personTypeRef.name + " (#" + source.personTypeRef.id + ")"
            );
        } else if (source.personType != null && !source.personType.isBlank()) {
            labels.put("personTypeRefId", source.personType);
        }

        result.put("referenceLabels", labels);
        return result;
    }

    private Map<String, Object> organizationView(Organization source) {
        var canonical = CanonicalMasterDataMapper.organization(
            source,
            TENANT,
            CompanyId.of("comandos:organization:" + source.id)
        );
        Map<String, Object> result = metadata(source, canonical.legalName());

        result.put("natureId", source.nature == null ? null : source.nature.id);
        result.put(
            "economicActivityId",
            source.economicActivity == null ? null : source.economicActivity.id
        );
        result.put("name", canonical.legalName());
        result.put("acronym", source.acronym);
        result.put("taxId", canonical.taxId());
        result.put("publicOrganization", source.publicOrganization);
        result.put("active", canonical.status() == LifecycleStatus.ACTIVE);

        Map<String, String> labels = new LinkedHashMap<>();
        if (source.nature != null) {
            labels.put(
                "natureId",
                source.nature.name + " (#" + source.nature.id + ")"
            );
        } else if (source.legacyNature != null && !source.legacyNature.isBlank()) {
            labels.put("natureId", source.legacyNature);
        }
        if (source.economicActivity != null) {
            labels.put(
                "economicActivityId",
                source.economicActivity.code + " - "
                    + source.economicActivity.description + " (#"
                    + source.economicActivity.id + ")"
            );
        }

        result.put("referenceLabels", labels);
        return result;
    }

    private Map<String, Object> unitView(OrganizationalUnit source) {
        var canonical = CanonicalMasterDataMapper.unit(
            source,
            TENANT,
            CompanyId.of("comandos:organization:" + source.organization.id)
        );
        Map<String, Object> result = metadata(source, canonical.name());

        result.put("organizationId", source.organization.id);
        result.put(
            "parentUnitId",
            source.parentUnit == null ? null : source.parentUnit.id
        );
        result.put("code", canonical.code());
        result.put("name", canonical.name());
        result.put("unitTypeId", source.unitType == null ? null : source.unitType.id);
        result.put("active", canonical.active());

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(
            "organizationId",
            source.organization.name + " (#" + source.organization.id + ")"
        );
        if (source.parentUnit != null) {
            labels.put(
                "parentUnitId",
                source.parentUnit.name + " (#" + source.parentUnit.id + ")"
            );
        }
        if (source.unitType != null) {
            labels.put(
                "unitTypeId",
                source.unitType.name + " (#" + source.unitType.id + ")"
            );
        } else if (source.type != null && !source.type.isBlank()) {
            labels.put("unitTypeId", source.type);
        }

        result.put("referenceLabels", labels);
        return result;
    }

    private static Map<String, Object> metadata(CoreEntity source, String labelValue) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", source.id);
        result.put("version", source.version);
        result.put("createdAt", source.createdAt);
        result.put("updatedAt", source.updatedAt);
        result.put("label", labelValue + " (#" + source.id + ")");
        return result;
    }

    private static CoreCatalog.Resource supported(String resource) {
        if (!SUPPORTED.contains(resource)) {
            throw new IllegalArgumentException(
                "Unsupported canonical Core read resource: " + resource
            );
        }
        return CoreCatalog.get(resource);
    }

    private static String likeTerm(String raw) {
        return "%" + raw.trim()
            .toLowerCase(Locale.ROOT)
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_") + "%";
    }

    private static void bad(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
