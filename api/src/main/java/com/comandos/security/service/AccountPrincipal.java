package com.comandos.security.service;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AccountPrincipal extends User {
    public final long accountId;
    public final long accountVersion;

    public AccountPrincipal(long id, long version, String login, String passwordHash) {
        super(login, passwordHash, List.of(new SimpleGrantedAuthority("AUTHENTICATED")));
        accountId = id;
        accountVersion = version;
    }
}
