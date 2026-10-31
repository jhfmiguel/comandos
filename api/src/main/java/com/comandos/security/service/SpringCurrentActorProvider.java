package com.comandos.security.service;

import com.fariamiguel.security.api.CurrentActor;
import com.fariamiguel.security.api.CurrentActorProvider;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * COMANDOS adapter that exposes its authenticated account through the canonical
 * Faria Miguel security contract.
 */
@Component
public class SpringCurrentActorProvider implements CurrentActorProvider {

    private final AccessPolicy access;

    public SpringCurrentActorProvider(AccessPolicy access) {
        this.access = access;
    }

    @Override
    public CurrentActor currentActor() {
        var authentication =
            SecurityContextHolder.getContext().getAuthentication();

        if (
            authentication == null
            || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof AccountPrincipal principal)
        ) {
            return CurrentActor.anonymous();
        }

        return new CurrentActor(
            principal.subject(),
            principal.displayName(),
            principal.roles(),
            principal.authorities(),
            access.currentScopeGrants(),
            principal.sessionId(),
            true
        );
    }
}
