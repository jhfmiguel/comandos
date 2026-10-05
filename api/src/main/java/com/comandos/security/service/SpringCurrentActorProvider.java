package com.comandos.security.service;

import com.fariamiguel.security.api.CurrentActor;
import com.fariamiguel.security.api.CurrentActorProvider;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * COMANDOS adapter that exposes its authenticated account through the canonical
 * Faria Miguel security contract.
 */
@Component
public class SpringCurrentActorProvider implements CurrentActorProvider {

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

        Set<String> authorities = principal.getAuthorities().stream()
            .map(granted -> granted.getAuthority())
            .collect(Collectors.toUnmodifiableSet());

        Set<String> roles = authorities.stream()
            .filter(value -> value.startsWith("ROLE_"))
            .map(value -> value.substring(5))
            .collect(Collectors.toUnmodifiableSet());

        return new CurrentActor(
            String.valueOf(principal.accountId),
            principal.getUsername(),
            roles,
            authorities,
            Set.of(),
            null,
            true
        );
    }
}
