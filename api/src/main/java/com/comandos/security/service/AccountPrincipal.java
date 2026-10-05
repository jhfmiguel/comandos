package com.comandos.security.service;

import com.fariamiguel.security.api.PlatformPrincipal;
import com.fariamiguel.security.api.ScopeGrant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Local COMANDOS account principal exposed through the canonical Faria Miguel
 * PlatformPrincipal contract.
 *
 * <p>Scope grants are resolved live by the CurrentActor provider so revocation
 * never depends on issuing a new login session.</p>
 */
public class AccountPrincipal extends User implements PlatformPrincipal {
    public final long accountId;
    public final long accountVersion;
    private final String displayName;

    public AccountPrincipal(
            long id,
            long version,
            String login,
            String passwordHash) {
        this(id, version, login, login, passwordHash);
    }

    public AccountPrincipal(
            long id,
            long version,
            String login,
            String displayName,
            String passwordHash) {
        super(login, passwordHash, List.of(new SimpleGrantedAuthority("AUTHENTICATED")));
        this.accountId = id;
        this.accountVersion = version;
        this.displayName =
            displayName == null || displayName.isBlank()
                ? login
                : displayName.trim();
    }

    @Override
    public String subject() {
        return String.valueOf(accountId);
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Set<String> roles() {
        return getAuthorities().stream()
            .map(granted -> granted.getAuthority())
            .filter(value -> value.startsWith("ROLE_"))
            .map(value -> value.substring(5))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> authorities() {
        return getAuthorities().stream()
            .map(granted -> granted.getAuthority())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<ScopeGrant> scopeGrants() {
        // Live scopes are resolved by SpringCurrentActorProvider/AccessPolicy.
        return Set.of();
    }

    @Override
    public String sessionId() {
        return null;
    }
}
