package com.comandos.security.service;
import com.fariamiguel.app.security.service.AccessPolicy;

import com.fariamiguel.app.core.config.PlatformProperties;
import com.fariamiguel.app.core.model.CoreEntity;
import com.fariamiguel.security.api.AccessDeniedException;
import com.fariamiguel.security.api.ResourceAccessPolicy;
import com.fariamiguel.security.api.ScopeGrant;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional(readOnly = true)
public class ComandosAccessPolicy extends AccessPolicy implements ResourceAccessPolicy {
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
    @Autowired
    public ComandosAccessPolicy(EntityManager em, PlatformProperties properties) { super(em, properties); }

    ComandosAccessPolicy(EntityManager em, boolean enabled, boolean requireLogin) { super(em, enabled, requireLogin); }

    public static Scope scope(String resource) {
        if ("inventory/regulatory-controls".equals(resource))
            return new Scope("asset.location.organizationLegacyId", "asset.location.unitLegacyId");
        if (Set.of("contracts", "legal/matters", "legal/deadlines", "legal/authorities", "legal/opinions").contains(resource))
            return new Scope("organizationLegacyId", "unitLegacyId");
        if (Set.of("custodies", "ammunition-consumptions").contains(resource))
            return new Scope("organizationLegacyId", "unitLegacyId");
        if ("consumable-usages".equals(resource)) return new Scope(null, null);
        var generic = AccessPolicy.scope(resource);
        if (resource.startsWith("inventory/") || Set.of("sales", "donations", "transfers", "disposals",
                "maintenance", "reservations", "inventory-counts").contains(resource)) {
            return new Scope(productPath(generic.organizationPath()), productPath(generic.unitPath()));
        }
        return generic;
    }

    private static String productPath(String path) {
        return path == null ? null : path.replace("organization.id", "organizationLegacyId")
            .replace("unit.id", "unitLegacyId").replace("sourceUnit.id", "sourceUnitLegacyId");
    }

    @Override
    protected Scope resolveScope(String resource) { return scope(resource); }

    @Override
    protected boolean isGlobalParameterResource(String resource) {
        return super.isGlobalParameterResource(resource) || INVENTORY_PARAMETER_RESOURCES.contains(resource);
    }

    @Override
    protected List<Grant> matching(String resource, String action, Scope scope) {
        if (!INVENTORY_PARAMETER_RESOURCES.contains(resource)) return super.matching(resource, action, scope);
        String requiredAction = ACCESS_RESOURCES.contains(resource) ? "MANAGE" : action;
        return grants().stream().filter(grant ->
            (INVENTORY_PARAMETER_PARENT_RESOURCES.contains(grant.resource()) || "*".equals(grant.resource()))
                && (grant.action().equals(requiredAction) || "*".equals(grant.action()))
                && "SYSTEM".equals(grant.scope())).toList();
    }

    @Override
    public boolean can(String resource, String action) { return canAny(resource, action); }

    @Override
    public void require(String resource, String action) { requireAny(resource, action); }

    @Override
    public void requireEntity(String resource, String action, Object entity) {
        if (!(entity instanceof CoreEntity coreEntity))
            throw new IllegalArgumentException("COMANDOS scoped access requires a CoreEntity during the migration period.");
        super.requireEntity(resource, action, coreEntity);
    }

    @Override
    protected void denied() {
        throw new AccessDeniedException("You do not have permission for this operation or scope.");
    }
    @Override
    public void requireStockSelectionScope(String resource, String action, long organizationId, Long unitId,
            long stockOrganizationId, Long stockUnitId) {
        requireScope(resource, action, organizationId, unitId);
        if (organizationId != stockOrganizationId)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "All stock must belong to the selected organization.");
        if (unitId != null && !java.util.Objects.equals(unitId, stockUnitId))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "All stock must belong to the selected unit.");
        requireScope(resource, action, stockOrganizationId, stockUnitId);
    }}
