package com.weaponsregistration.audit.service;

import com.weaponsregistration.audit.model.AuditRecord;
import com.weaponsregistration.audit.model.AuditReference;
import com.weaponsregistration.inventory.model.StockLocation;
import com.weaponsregistration.security.service.AccountPrincipal;
import com.weaponsregistration.security.service.AccessPolicy;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional(readOnly = true)
public class AuditService {
    private final EntityManager em;
    private final AccessPolicy access;
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
    private static final Set<String> SECRETS = Set.of("password", "passwordhash", "token", "csrftoken", "requestfingerprint", "authorization");
    public AuditService(EntityManager em, AccessPolicy access) { this.em = em; this.access = access; }
    public record Actor(Long id, String login, String type) {}
    public record Summary(long id, Instant occurredAt, Long actorId, String actorLogin, String actorType, String resource, long recordId, String action) {}
    public record Detail(Summary event, JsonNode before, JsonNode after) {}
    public record Page(List<Summary> content, long totalElements, int page, int size) {}

    public Actor actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal)
            return new Actor(principal.accountId, principal.getUsername(), "ACCOUNT");
        return new Actor(null, null, "UNAUTHENTICATED");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String resource, long recordId, String action, Object before, Object after) {
        var actor = actor();
        var event = new AuditRecord();
        event.occurredAt = Instant.now();
        event.actorId = actor.id(); event.actorLogin = actor.login(); event.actorType = actor.type();
        event.resource = resource; event.recordId = recordId; event.action = action;
        event.beforeJson = snapshot(before); event.afterJson = snapshot(after);
        em.persist(event);
        Set<Reference> references = new HashSet<>();
        add(references, resource, recordId);
        collect(event.beforeJson == null ? null : json.readTree(event.beforeJson), references, resource);
        collect(event.afterJson == null ? null : json.readTree(event.afterJson), references, resource);
        // Resolve only at write time: moving/deleting an item cannot rewrite its history.
        for (var reference : List.copyOf(references)) {
            if (!reference.kind().equals("inventory/locations")) continue;
            var location = em.find(StockLocation.class, reference.id());
            if (location != null) {
                add(references, "organization", location.organization.id);
                if (location.unit != null) add(references, "unit", location.unit.id);
            }
        }
        for (var reference : references) {
            var row = new AuditReference();
            row.eventId = event.id; row.kind = reference.kind(); row.targetId = reference.id();
            em.persist(row);
        }
    }

    private record Reference(String kind, long id) {}
    private static void add(Set<Reference> references, String kind, long id) {
        if (id > 0) references.add(new Reference(kind, id));
    }
    private void collect(JsonNode node, Set<Reference> references, String resource) {
        if (node == null || node.isNull()) return;
        if (node.isArray()) { node.forEach(child -> collect(child, references, resource)); return; }
        if (!node.isObject()) return;
        if (node.hasNonNull("resource") && node.hasNonNull("recordId") && node.get("recordId").isIntegralNumber())
            add(references, node.get("resource").asText(), node.get("recordId").asLong());
        for (var field : node.properties()) {
            String kind = switch (field.getKey()) {
                case "assetId" -> "inventory/assets";
                case "lotId" -> "inventory/lots";
                case "organizationId" -> "organization";
                case "unitId", "sourceUnitId", "destinationUnitId" -> "unit";
                case "locationId", "openingLocationId", "sourceLocationId", "destinationLocationId" -> "inventory/locations";
                default -> null;
            };
            if (kind != null && field.getValue().isIntegralNumber()) add(references, kind, field.getValue().asLong());
            if (field.getKey().equals("recordIds") && field.getValue().isArray())
                field.getValue().forEach(id -> { if (id.isIntegralNumber()) add(references, resource, id.asLong()); });
            collect(field.getValue(), references, resource);
        }
    }

    private String snapshot(Object value) {
        if (value == null) return null;
        return json.writeValueAsString(redact(json.valueToTree(value)));
    }
    private JsonNode redact(JsonNode node) {
        if (node.isObject()) {
            var result = json.createObjectNode();
            for (var property : node.properties()) {
                String normalized = property.getKey().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
                if (!SECRETS.contains(normalized)) result.set(property.getKey(), redact(property.getValue()));
            }
            return result;
        }
        if (node.isArray()) {
            var result = json.createArrayNode();
            node.forEach(item -> result.add(redact(item)));
            return result;
        }
        return node;
    }

    public Page list(String resource, Long recordId, String action, String actor, Instant from, Instant until, int page) {
        return list(resource, recordId, action, actor, from, until, page, null, null, null, null, null);
    }

    public Page list(String resource, Long recordId, String action, String actor, Instant from, Instant until, int page,
            Long assetId, Long lotId, Long actorId, Long organizationId, Long unitId) {
        access.requireAny("audit", "READ");
        if (page < 0 || page > 100000 || from != null && until != null && from.isAfter(until)) bad("Invalid audit filters.");
        List<String> clauses = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        referenceFilter(clauses, parameters, "assetId", "inventory/assets", assetId);
        referenceFilter(clauses, parameters, "lotId", "inventory/lots", lotId);
        referenceFilter(clauses, parameters, "organizationId", "organization", organizationId);
        referenceFilter(clauses, parameters, "unitId", "unit", unitId);
        if (actorId != null) { clauses.add("a.actorId = :actorId"); parameters.put("actorId", actorId); }
        if (resource != null && !resource.isBlank()) { clauses.add("a.resource = :resource"); parameters.put("resource", resource.trim()); }
        if (recordId != null) { clauses.add("a.recordId = :recordId"); parameters.put("recordId", recordId); }
        if (action != null && !action.isBlank()) { clauses.add("a.action = :action"); parameters.put("action", action); }
        if (actor != null && !actor.isBlank()) {
            clauses.add("lower(a.actorLogin) like :actor escape '!'");
            parameters.put("actor", "%" + actor.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        if (from != null) { clauses.add("a.occurredAt >= :from"); parameters.put("from", from); }
        if (until != null) { clauses.add("a.occurredAt <= :until"); parameters.put("until", until); }
        String where = clauses.isEmpty() ? "" : " where " + String.join(" and ", clauses);
        var query = em.createQuery("select a from AuditRecord a" + where + " order by a.id desc", AuditRecord.class);
        var count = em.createQuery("select count(a) from AuditRecord a" + where, Long.class);
        parameters.forEach((key, value) -> { query.setParameter(key, value); count.setParameter(key, value); });
        return new Page(query.setFirstResult(page * 20).setMaxResults(20).getResultList().stream().map(this::summary).toList(), count.getSingleResult(), page, 20);
    }

    private void referenceFilter(List<String> clauses, Map<String, Object> parameters, String name, String kind, Long id) {
        if (id == null) return;
        if (id <= 0) bad("Invalid audit reference filter.");
        clauses.add("exists (select r.id from AuditReference r where r.eventId = a.id and r.kind = '" + kind + "' and r.targetId = :" + name + ")");
        parameters.put(name, id);
    }

    public Detail get(long id) {
        access.requireAny("audit", "READ");
        var event = em.find(AuditRecord.class, id);
        if (event == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit record not found.");
        return new Detail(summary(event), event.beforeJson == null ? null : json.readTree(event.beforeJson), event.afterJson == null ? null : json.readTree(event.afterJson));
    }
    private Summary summary(AuditRecord a) { return new Summary(a.id, a.occurredAt, a.actorId, a.actorLogin, a.actorType, a.resource, a.recordId, a.action); }
    private static void bad(String detail) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, detail); }
}
