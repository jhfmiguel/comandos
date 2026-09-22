package com.comandos.security.service;

import com.comandos.core.model.SystemUser;
import jakarta.persistence.EntityManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService implements UserDetailsService {
    private final EntityManager em;
    public AccountService(EntityManager em) { this.em = em; }
    public record AccountView(long id, String login, String name) {}

    @Override
    public UserDetails loadUserByUsername(String username) {
        var account = em.createQuery("select u from SystemUser u where u.login = :login", SystemUser.class)
            .setParameter("login", username).getResultStream().findFirst().orElse(null);
        if (!available(account)) throw new UsernameNotFoundException("Invalid login or password.");
        return new AccountPrincipal(account.id, account.version, account.login, account.passwordHash);
    }

    public AccountView current(AccountPrincipal principal) {
        var account = em.find(SystemUser.class, principal.accountId);
        if (!available(account) || account.version != principal.accountVersion) return null;
        return new AccountView(account.id, account.login, account.person.fullName);
    }

    private boolean available(SystemUser account) {
        // MFA-enabled accounts must not fall back to password-only authentication.
        return account != null && !Boolean.TRUE.equals(account.blocked) && !account.mfaEnabled
            && Boolean.TRUE.equals(account.person.active);
    }
}
