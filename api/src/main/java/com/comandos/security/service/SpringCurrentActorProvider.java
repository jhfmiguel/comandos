package com.comandos.security.service;

import com.comandos.security.api.CurrentActor;
import com.comandos.security.api.CurrentActorProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringCurrentActorProvider implements CurrentActorProvider {

    @Override
    public CurrentActor current() {
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
            principal.accountId,
            principal.getUsername(),
            true
        );
    }
}
