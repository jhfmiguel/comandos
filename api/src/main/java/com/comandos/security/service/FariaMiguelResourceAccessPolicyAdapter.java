package com.comandos.security.service;

import com.comandos.core.model.CoreEntity;
import com.fariamiguel.security.api.ResourceAccessPolicy;
import org.springframework.stereotype.Component;

/** Transitional adapter while COMANDOS identity persistence is migrated to the shared foundation. */
@Component
public final class FariaMiguelResourceAccessPolicyAdapter implements ResourceAccessPolicy {
    private final AccessPolicy delegate;

    public FariaMiguelResourceAccessPolicyAdapter(AccessPolicy delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean can(String resource, String action) {
        return delegate.canAny(resource, action);
    }

    @Override
    public void require(String resource, String action) {
        delegate.requireAny(resource, action);
    }

    @Override
    public String predicate(String resource, String action, String alias) {
        return delegate.predicate(resource, action, alias);
    }

    @Override
    public void requireEntity(String resource, String action, Object entity) {
        if (!(entity instanceof CoreEntity coreEntity)) {
            throw new IllegalArgumentException("COMANDOS scoped access requires a CoreEntity during the migration period.");
        }
        delegate.requireEntity(resource, action, coreEntity);
    }
}
