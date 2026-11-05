package com.comandos.audit.service;
import com.fariamiguel.app.audit.service.AuditService;

import com.fariamiguel.app.audit.api.AuditReferenceEnricher;
import com.comandos.security.service.ComandosAccessPolicy;
import com.fariamiguel.app.security.service.AccountPrincipal;
import com.fariamiguel.security.api.CurrentActorProvider;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.function.BiConsumer;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the security product actor and product reference enrichers. */
@Service
@Primary
@Transactional(readOnly = true)
public class ComandosAuditService extends AuditService {
    private final List<AuditReferenceEnricher> enrichers;

    public ComandosAuditService(EntityManager em, ComandosAccessPolicy access, CurrentActorProvider actors,
            List<AuditReferenceEnricher> enrichers) {
        super(em, access, () -> resolveActor(actors));
        this.enrichers = List.copyOf(enrichers);
    }

    private static Actor resolveActor(CurrentActorProvider actors) {
        var current = actors.currentActor();
        if (!current.authenticated()) return new Actor(null, null, "UNAUTHENTICATED");
        String login = current.displayName();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            login = principal.getUsername();
        }
        return new Actor(numericActorId(current.id()), login, "ACCOUNT");
    }

    private static Long numericActorId(String value) {
        if (value == null || value.isBlank() || "anonymous".equalsIgnoreCase(value)) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return Integer.toUnsignedLong(value.hashCode());
        }
    }

    @Override
    protected void enrichReferences(String kind, long targetId, BiConsumer<String, Long> addReference) {
        for (var enricher : enrichers) enricher.enrich(kind, targetId, addReference);
    }
}