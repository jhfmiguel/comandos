package com.comandos.security.service;

import com.comandos.core.config.PlatformProperties;
import com.comandos.core.model.*;
import com.fariamiguel.security.api.AccessDeniedException;
import com.fariamiguel.security.api.ResourceAccessPolicy;
import com.fariamiguel.security.api.ScopeGrant;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccessPolicy implements ResourceAccessPolicy {
    private final EntityManager em;
    private final boolean enabled;
    private static final Set<String> ACCESS_RESOURCES = Set.of("core/users", "core/profile-levels", "core/permission-resources", "core/permission-actions", "core/profiles", "core/permissions", "core/user-profiles", "core/profile-permissions");
    private static final Map<String, String> CORE_PARAMETER_PARENT_RESOURCES = Map.of(
        "core/organization-natures", "core/organizations",
        "core/economic-activities", "core/organizations",
        "core/unit-types", "core/units",
        "core/person-types", "core/people",
        "core/contact-types", "core/people"
    );
    private static final Set<String> INVENTORY_PARAMETER_PARENT_RESOURCES = Set.of(
        "inventory/item-categories",
        "inventory/armament-types",
        "inventory/armament-classifications",
        "inventory/brands",
        "inventory/item-models",
        "inventory/technical-characteristics",
        "inventory/categories",
        "inventory/models",
        "inventory/characteristics"
    );
    private static final Set<String> INVENTORY_PARAMETER_RESOURCES = Set.of(
        "inventory/calibers",
        "inventory/ammunition-types",
        "inventory/projectile-types",
        "inventory/case-types",
        "inventory/primer-types",
        "inventory/grenade-types",
        "inventory/agents",
        "inventory/compositions",
        "inventory/protection-types",
        "inventory/protection-levels",
        "inventory/materials",
        "inventory/sizes",
        "inventory/cartridge-types",
        "inventory/optical-types",
        "inventory/shield-types",
        "inventory/locking-mechanisms",
        "inventory/component-types",
        "inventory/compatibilities",
        "inventory/interfaces"
    );
    public record Grant(String resource, String action, String scope, Long organizationId, Long unitId) {}
    public record AccessView(boolean enforced, List<Grant> grants) {}
    public record Scope(String organizationPath, String unitPath) {}

    @Autowired
    public AccessPolicy(EntityManager em, PlatformProperties properties) {
        this(
            em,
            properties.getSecurity().isEnforcePermissions(),
            properties.getSecurity().isRequireLogin()
        );
    }

    AccessPolicy(EntityManager em, boolean enabled, boolean requireLogin) {
        if (enabled && !requireLogin) {
            throw new IllegalStateException(
                "Permission enforcement requires login enforcement."
            );
        }
        this.em = em;
        this.enabled = enabled;
    }

    public AccessView current() { return new AccessView(enabled, enabled ? grants() : List.of()); }

    public Set<ScopeGrant> currentScopeGrants() {
        if (!enabled) return Set.of(ScopeGrant.system());

        return grants().stream()
            .map(grant -> switch (grant.scope()) {
                case "SYSTEM" -> ScopeGrant.system();
                case "ORGANIZATION" -> ScopeGrant.organization(String.valueOf(grant.organizationId()));
                case "UNIT" -> ScopeGrant.unit(
                    String.valueOf(grant.organizationId()),
                    String.valueOf(grant.unitId())
                );
                default -> null;
            })
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private List<Grant> grants() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) return List.of();
        // Fetch scalar grant data each time: revocation does not depend on a new login.
        var rows = em.createQuery("select p.permission.resource, p.permission.action, u.profile.level, u.organization.id, unit.id "
            + "from UserProfile u left join u.unit unit, ProfilePermission p where p.profile.id = u.profile.id "
            + "and u.user.id = :user and u.user.blocked = false and u.user.mfaEnabled = false "
            + "and u.user.person.active = true and u.organization.active = true", Object[].class)
            .setParameter("user", principal.accountId).getResultList();
        return rows.stream().map(row -> new Grant((String) row[0], (String) row[1], (String) row[2], (Long) row[3], (Long) row[4]))
            .filter(g -> Set.of("SYSTEM", "ORGANIZATION", "UNIT").contains(g.scope()))
            .filter(g -> "UNIT".equals(g.scope()) ? g.unitId() != null : g.unitId() == null).distinct().toList();
    }

    public static Scope scope(String resource) {
        return switch (resource) {
            case "core/organizations" -> new Scope("id", null);
            case "core/units" -> new Scope("organization.id", "id");
            case "core/person-roles" -> new Scope("organization.id", "unit.id");
            case "core/role-data" -> new Scope("personRole.organization.id", "personRole.unit.id");
            case "inventory/locations" -> new Scope("organizationLegacyId", "unitLegacyId");
            case "inventory/assets", "inventory/balances", "inventory/movements" -> new Scope("location.organizationLegacyId", "location.unitLegacyId");
            case "inventory/regulatory-controls" -> new Scope("asset.location.organizationLegacyId", "asset.location.unitLegacyId");
            case "inventory/expirations", "inventory/certifications", "inventory/recalls" -> new Scope("organization.id", "unit.id");
            case "inventory/recall-items" -> new Scope("recall.organization.id", "recall.unit.id");
            case "inventory/equipment-sets" -> new Scope("organization.id", "unit.id");
            case "inventory/equipment-set-components" -> new Scope("equipmentSet.organization.id", "equipmentSet.unit.id");
            case "inventory/lots" -> new Scope("openingLocation.organizationLegacyId", "openingLocation.unitLegacyId");
            case "inventory/item-values" -> new Scope("asset.location.organizationLegacyId", "asset.location.unitLegacyId");
            case "sales" -> new Scope("organizationLegacyId", "unitLegacyId");
            case "custodies" -> new Scope("organizationLegacyId", "unitLegacyId");
            case "ammunition-consumptions" -> new Scope("organization.id", "unit.id");
            case "donations" -> new Scope("organizationLegacyId", "unitLegacyId");
            case "transfers" -> new Scope("organizationLegacyId", "sourceUnitLegacyId");
            case "disposals" -> new Scope("organizationLegacyId", "unitLegacyId");
            case "maintenance" -> new Scope("organizationLegacyId", "unitLegacyId");
            case "reservations" -> new Scope("organizationLegacyId", "unitLegacyId");
            case "inventory-counts" -> new Scope("organizationLegacyId", "unitLegacyId");
            default -> new Scope(null, null);
        };
    }

    private List<Grant> matching(String resource, String action, Scope scope) {
        String requiredAction = ACCESS_RESOURCES.contains(resource) ? "MANAGE" : action;

        if (INVENTORY_PARAMETER_RESOURCES.contains(resource)) {
            return grants().stream()
                .filter(g -> (INVENTORY_PARAMETER_PARENT_RESOURCES.contains(g.resource()) || g.resource().equals("*"))
                    && (g.action().equals(requiredAction) || g.action().equals("*"))
                    && "SYSTEM".equals(g.scope()))
                .toList();
        }

        Set<String> requiredResources;
        if (ACCESS_RESOURCES.contains(resource)) {
            requiredResources = Set.of("security/access");
        } else if (Set.of("core/person-addresses", "core/person-phones", "core/person-emails").contains(resource)) {
            requiredResources = Set.of("core/people");
        } else if ("core/unit-types".equals(resource)) {
            requiredResources = Set.of("core/units", "core/organizations");
        } else if (CORE_PARAMETER_PARENT_RESOURCES.containsKey(resource)) {
            requiredResources = Set.of(CORE_PARAMETER_PARENT_RESOURCES.get(resource));
        } else {
            requiredResources = Set.of(resource);
        }

        var matched = grants().stream().filter(g ->
            (requiredResources.contains(g.resource()) || g.resource().equals("*"))
                && (g.action().equals(requiredAction) || g.action().equals("*"))
        );

        if (CORE_PARAMETER_PARENT_RESOURCES.containsKey(resource)) {
            return matched.toList();
        }

        return matched
            .filter(g -> "SYSTEM".equals(g.scope()) || scope.organizationPath() != null
                && ("ORGANIZATION".equals(g.scope()) || scope.unitPath() != null))
            .toList();
    }

    @Override
    public boolean can(String resource, String action) { return canAny(resource, action); }

    public boolean canAny(String resource, String action) { return !enabled || !matching(resource, action, scope(resource)).isEmpty(); }
    public List<String> actions(String resource) {
        return List.of("READ", "CREATE", "UPDATE", "DELETE").stream().filter(action -> canAny(resource, action)).toList();
    }
    @Override
    public void require(String resource, String action) { requireAny(resource, action); }

    public void requireAny(String resource, String action) { if (!canAny(resource, action)) denied(); }

    @Override
    public String predicate(String resource, String action, String alias) {
        return predicate(resource, action, alias, scope(resource));
    }
    public String predicate(String resource, String action, String alias, Scope scope) {
        if (!enabled) return "1 = 1";
        var matching = matching(resource, action, scope);
        if (matching.isEmpty()) denied();
        if (CORE_PARAMETER_PARENT_RESOURCES.containsKey(resource) || INVENTORY_PARAMETER_RESOURCES.contains(resource)) return "1 = 1";
        if (matching.stream().anyMatch(g -> "SYSTEM".equals(g.scope()))) return "1 = 1";
        // All values below are typed database IDs; callers supply fixed property paths.
        return "(" + String.join(" or ", matching.stream().map(g -> {
            String clause = alias + "." + scope.organizationPath() + " = " + g.organizationId();
            if ("UNIT".equals(g.scope())) clause += " and " + alias + "." + scope.unitPath() + " = " + g.unitId();
            return "(" + clause + ")";
        }).toList()) + ")";
    }

    @Override
    public void requireEntity(String resource, String action, Object entity) {
        if (!(entity instanceof CoreEntity coreEntity)) {
            throw new IllegalArgumentException("COMANDOS scoped access requires a CoreEntity during the migration period.");
        }
        if (CORE_PARAMETER_PARENT_RESOURCES.containsKey(resource) || INVENTORY_PARAMETER_RESOURCES.contains(resource)) {
            requireAny(resource, action);
            return;
        }
        var scope = scope(resource);
        requireScope(resource, action, scope, idAt(coreEntity, scope.organizationPath()), idAt(coreEntity, scope.unitPath()));
    }
    public void requireScope(String resource, String action, Long organizationId, Long unitId) {
        requireScope(resource, action, scope(resource), organizationId, unitId);
    }
    public boolean canScope(String resource, String action, Long organizationId, Long unitId) {
        if (!enabled) return true;
        var scope = scope(resource);
        return matching(resource, action, scope).stream().anyMatch(g -> "SYSTEM".equals(g.scope())
            || Objects.equals(g.organizationId(), organizationId)
                && ("ORGANIZATION".equals(g.scope()) || Objects.equals(g.unitId(), unitId)));
    }
    private void requireScope(String resource, String action, Scope scope, Long organizationId, Long unitId) {
        if (!enabled) return;
        boolean allowed = matching(resource, action, scope).stream().anyMatch(g -> "SYSTEM".equals(g.scope())
            || Objects.equals(g.organizationId(), organizationId) && ("ORGANIZATION".equals(g.scope()) || Objects.equals(g.unitId(), unitId)));
        if (!allowed) denied();
    }

    private Long idAt(Object entity, String path) {
        if (entity == null || path == null) return null;
        Object current = entity;
        for (String property : path.split("\\.")) {
            if (current == null) return null;
            try { current = current.getClass().getField(property).get(current); }
            catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
        }
        return (Long) current;
    }

    public boolean legacyAllowed(String path, String method) {
        if (!enabled) return true;
        String resource = null;
        for (String key : List.of("users", "weapons", "sales"))
            if (path.equals("/api/" + key) || path.startsWith("/api/" + key + "/")) resource = "legacy/" + key;
        if (resource == null) return true;
        String action = switch (method) { case "GET", "HEAD" -> "READ"; case "POST" -> "CREATE"; case "PUT" -> "UPDATE"; case "DELETE" -> "DELETE"; default -> "DENIED"; };
        return canAny(resource, action);
    }
    private static void denied() {
        throw new AccessDeniedException(
            "You do not have permission for this operation or scope."
        );
    }
}
