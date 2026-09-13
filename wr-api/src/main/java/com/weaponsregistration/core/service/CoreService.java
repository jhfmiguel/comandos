package com.weaponsregistration.core.service;

import com.weaponsregistration.core.model.*;
import com.weaponsregistration.security.service.AccessPolicy;
import com.weaponsregistration.audit.service.AuditService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CoreService {
    private final EntityManager em;
    private final List<UnitScopeGuard> unitScopeGuards;
    private final AccessPolicy access;
    private final AuditService audit;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);

    public CoreService(EntityManager em, List<UnitScopeGuard> unitScopeGuards, AccessPolicy access, AuditService audit) {
        this.em = em;
        this.access = access;
        this.audit = audit;
        this.unitScopeGuards = unitScopeGuards;
    }

    public record PageResult(List<Map<String, Object>> content, long totalElements,
                             int page, int size) {}

    public PageResult list(String resource, String search, int page, int size, Long organizationId) {
        var spec = CoreCatalog.get(resource);
        if (page < 0 || page > 100000 || size < 1 || size > 100) bad("Invalid pagination.");
        List<String> searchExpressions = new ArrayList<>();
        searchExpressions.add("cast(e.id as string)");
        spec.fields().stream().filter(f -> Set.of("text", "email", "choice").contains(f.type()))
            .forEach(f -> searchExpressions.add("lower(e." + f.property() + ")"));
        if (spec.entity() == PersonRoleAssignment.class) {
            searchExpressions.add("lower(e.person.fullName)");
            searchExpressions.add("lower(e.role.name)");
        }
        if (spec.entity() == UserProfile.class) {
            searchExpressions.add("lower(e.user.login)");
            searchExpressions.add("lower(e.profile.name)");
        }
        if (spec.entity() == ProfilePermission.class) {
            searchExpressions.add("lower(e.profile.name)");
            searchExpressions.add("lower(e.permission.resource)");
            searchExpressions.add("lower(e.permission.action)");
        }
        String where = "";
        boolean filter = search != null && !search.isBlank();
        if (filter) where = " where (" + String.join(" or ", searchExpressions.stream()
            .map(expression -> expression + " like :search escape '!' ").toList()) + ")";
        boolean scoped = organizationId != null && spec.entity() == OrganizationalUnit.class;
        if (scoped) where += (filter ? " and " : " where ") + "e.organization.id = :organizationId";
        where += (where.isEmpty() ? " where " : " and ") + access.predicate("core/" + resource, "READ", "e");
        var query = em.createQuery("select e from " + spec.entity().getSimpleName() + " e" + where + " order by e.id", spec.entity());
        var count = em.createQuery("select count(e) from " + spec.entity().getSimpleName() + " e" + where, Long.class);
        if (filter) {
            String term = "%" + search.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            query.setParameter("search", term);
            count.setParameter("search", term);
        }
        if (scoped) {
            query.setParameter("organizationId", organizationId);
            count.setParameter("organizationId", organizationId);
        }
        return new PageResult(query.setFirstResult(page * size).setMaxResults(size)
            .getResultList().stream().map(e -> view(spec, e)).toList(), count.getSingleResult(), page, size);
    }

    public Map<String, Object> get(String resource, long id) {
        var spec = CoreCatalog.get(resource);
        access.requireAny("core/" + resource, "READ");
        var entity = find(spec, id);
        access.requireEntity("core/" + resource, "READ", entity);
        return view(spec, entity);
    }

    @Transactional
    public Map<String, Object> save(String resource, Long id, Map<String, Object> data) {
        var spec = CoreCatalog.get(resource);
        String action = id == null ? "CREATE" : "UPDATE";
        access.requireAny("core/" + resource, action);
        Set<String> allowed = new HashSet<>(spec.fields().stream().map(CoreCatalog.Field::name).toList());
        allowed.add("version");
        if (!allowed.containsAll(data.keySet())) bad("The form contains unsupported fields.");
        lockScope(spec, id, data);
        CoreEntity entity;
        try {
            entity = id == null ? spec.entity().getConstructor().newInstance() : find(spec, id);
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
        if (id != null) access.requireEntity("core/" + resource, action, entity);
        if (id != null && !Objects.equals(entity.version, positiveLong(data.get("version"), "Version", true))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This record has changed. Reload before saving.");
        }
        Map<String, Object> before = id == null ? null : view(spec, entity);
        for (var field : spec.fields()) {
            Object raw = data.get(field.name());
            if (field.type().equals("password")) {
                if (id != null && (raw == null || "".equals(raw))) continue;
                if (!(raw instanceof String)) bad("Enter a password.");
                String password = (String) raw;
                if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
                    bad("Password must contain at least 12 characters and at most 72 bytes.");
                ((SystemUser) entity).passwordHash = passwords.encode(password);
                continue;
            }
            if (raw instanceof String text) raw = text.trim().isEmpty() ? null : text.trim();
            if (raw == null && field.required()) bad(field.label() + " is required.");
            Object value = raw;
            if (raw != null) {
                switch (field.type()) {
                    case "reference" -> {
                        value = find(CoreCatalog.get(field.reference()), positiveLong(raw, field.label(), false));
                        access.requireEntity("core/" + field.reference(), "READ", (CoreEntity) value);
                    }
                    case "boolean" -> { if (!(raw instanceof Boolean)) bad(field.label() + " must be true or false."); }
                    case "date" -> {
                        try { value = LocalDate.parse(raw.toString()); }
                        catch (RuntimeException ex) { bad(field.label() + " must be a valid date."); }
                    }
                    default -> {
                        if (!(raw instanceof String) || raw.toString().length() > 255) bad(field.label() + " must contain at most 255 characters.");
                        if (!field.choices().isEmpty() && !field.choices().contains(raw)) bad(field.label() + " is invalid.");
                        if (field.type().equals("email") && !raw.toString().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) bad("Invalid email address.");
                    }
                }
            }
            write(entity, field.property(), value);
        }
        access.requireEntity("core/" + resource, action, entity);
        validate(entity);
        if (id == null) em.persist(entity);
        em.flush();
        var result = view(spec, entity);
        var auditResult = new LinkedHashMap<>(result);
        if (entity instanceof SystemUser && data.get("password") instanceof String password && !password.isEmpty()) auditResult.put("passwordChanged", true);
        audit.record("core/" + resource, entity.id, action, before, auditResult);
        return result;
    }

    @Transactional
    public void delete(String resource, long id, Long version) {
        access.requireAny("core/" + resource, "DELETE");
        var entity = find(CoreCatalog.get(resource), id);
        access.requireEntity("core/" + resource, "DELETE", entity);
        if (!Objects.equals(entity.version, version))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This record has changed. Reload before deleting.");
        var before = view(CoreCatalog.get(resource), entity);
        em.remove(entity);
        em.flush();
        audit.record("core/" + resource, id, "DELETE", before, null);
    }

    private void validate(CoreEntity entity) {
        if (entity instanceof Person person) {
            if (person.birthDate != null && person.birthDate.isAfter(LocalDate.now())) bad("Birth date cannot be in the future.");
            if ("LEGAL_ENTITY".equals(person.personType) && person.birthDate != null) bad("Legal entities cannot have a birth date.");
        }
        if (entity instanceof SystemUser user) user.login = user.login.toLowerCase(Locale.ROOT);
        if (entity instanceof OrganizationalUnit unit) {
            Set<Long> visited = new HashSet<>();
            if (unit.id != null) visited.add(unit.id);
            for (var parent = unit.parentUnit; parent != null; parent = parent.parentUnit) {
                if (!Objects.equals(parent.organization.id, unit.organization.id)) bad("Parent unit must belong to the same organization.");
                if (!visited.add(parent.id)) bad("The unit hierarchy cannot contain cycles.");
            }
            if (unit.id != null) {
                long links = em.createQuery("select count(u) from OrganizationalUnit u where u.parentUnit.id = :id and u.organization.id <> :org", Long.class)
                    .setParameter("id", unit.id).setParameter("org", unit.organization.id).getSingleResult();
                links += em.createQuery("select count(p) from PersonRoleAssignment p where p.unit.id = :id and p.organization.id <> :org", Long.class)
                    .setParameter("id", unit.id).setParameter("org", unit.organization.id).getSingleResult();
                links += em.createQuery("select count(p) from UserProfile p where p.unit.id = :id and p.organization.id <> :org", Long.class)
                    .setParameter("id", unit.id).setParameter("org", unit.organization.id).getSingleResult();
                if (links > 0) bad("The unit has assignments in its current organization.");
                unitScopeGuards.forEach(guard -> guard.validateOrganizationChange(unit.id, unit.organization.id));
            }
        }
        if (entity instanceof PersonRoleAssignment role) {
            sameOrganization(role.organization, role.unit);
            if (role.endDate != null && role.endDate.isBefore(role.startDate)) bad("End date must be on or after start date.");
            if ("ENDED".equals(role.status) && role.endDate == null) bad("An ended assignment requires an end date.");
        }
        if (entity instanceof UserProfile assignment) {
            sameOrganization(assignment.organization, assignment.unit);
            String unitClause = assignment.unit == null ? "p.unit is null" : "p.unit.id = :unit";
            var query = em.createQuery("select count(p) from UserProfile p where p.user.id = :user and p.profile.id = :profile and p.organization.id = :org and " + unitClause + " and p.id <> :id", Long.class)
                .setParameter("user", assignment.user.id).setParameter("profile", assignment.profile.id)
                .setParameter("org", assignment.organization.id).setParameter("id", assignment.id == null ? -1L : assignment.id);
            if (assignment.unit != null) query.setParameter("unit", assignment.unit.id);
            if (query.getSingleResult() > 0) bad("This profile is already assigned to this user in this scope.");
        }
    }

    private void sameOrganization(Organization organization, OrganizationalUnit unit) {
        if (unit != null && !Objects.equals(organization.id, unit.organization.id)) bad("Unit does not belong to the selected organization.");
    }

    // Acquire scope locks before loading related entities so hierarchy checks see
    // the latest committed relationships, including concurrent parent changes.
    private void lockScope(CoreCatalog.Resource spec, Long id, Map<String, Object> data) {
        if (spec.entity() != OrganizationalUnit.class && spec.entity() != PersonRoleAssignment.class
                && spec.entity() != UserProfile.class) return;
        Set<Long> organizations = new TreeSet<>();
        organizations.add(positiveLong(data.get("organizationId"), "Organization", false));
        if (id != null && spec.entity() == OrganizationalUnit.class) {
            organizations.addAll(em.createQuery("select u.organization.id from OrganizationalUnit u where u.id = :id", Long.class)
                .setParameter("id", id).getResultList());
        }
        for (Long organizationId : organizations) {
            var organization = em.find(Organization.class, organizationId, LockModeType.PESSIMISTIC_WRITE);
            if (organization == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found.");
        }
    }

    private CoreEntity find(CoreCatalog.Resource spec, long id) {
        var entity = em.find(spec.entity(), id);
        if (entity == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, spec.label() + ": record not found.");
        return entity;
    }

    private Map<String, Object> view(CoreCatalog.Resource spec, CoreEntity entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", entity.id);
        result.put("version", entity.version);
        result.put("createdAt", entity.createdAt);
        result.put("updatedAt", entity.updatedAt);
        result.put("label", label(spec, entity));
        Map<String, String> labels = new LinkedHashMap<>();
        for (var field : spec.fields()) {
            if (field.type().equals("password")) continue;
            Object value = read(entity, field.property());
            if (value instanceof CoreEntity related) {
                result.put(field.name(), related.id);
                labels.put(field.name(), label(CoreCatalog.get(field.reference()), related));
            } else result.put(field.name(), value);
        }
        result.put("referenceLabels", labels);
        return result;
    }

    private String label(CoreCatalog.Resource spec, CoreEntity entity) {
        if (entity instanceof Permission permission) return permission.resource + " / " + permission.action + " (#" + entity.id + ")";
        if (entity instanceof UserProfile assignment) return assignment.user.login + " / " + assignment.profile.name + " (#" + entity.id + ")";
        if (entity instanceof ProfilePermission assignment) return assignment.profile.name + " / " + assignment.permission.resource + " / " + assignment.permission.action + " (#" + entity.id + ")";
        for (String property : List.of("fullName", "name", "login", "code", "number", "key", "resource", "category")) {
            if (spec.fields().stream().anyMatch(f -> f.property().equals(property))) {
                Object value = read(entity, property);
                if (value != null) return value + " (#" + entity.id + ")";
            }
        }
        if (entity instanceof PersonRoleAssignment role) return role.person.fullName + " / " + role.role.name + " (#" + entity.id + ")";
        return spec.label() + " #" + entity.id;
    }

    private static Object read(CoreEntity entity, String property) {
        try { return entity.getClass().getField(property).get(entity); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    private static void write(CoreEntity entity, String property, Object value) {
        try { entity.getClass().getField(property).set(entity, value); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    private static Long positiveLong(Object raw, String label, boolean zeroAllowed) {
        try {
            long value = new BigDecimal(String.valueOf(raw)).longValueExact();
            if (value < (zeroAllowed ? 0 : 1)) throw new IllegalArgumentException();
            return value;
        } catch (RuntimeException ex) { bad(label + " is invalid."); return null; }
    }

    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
