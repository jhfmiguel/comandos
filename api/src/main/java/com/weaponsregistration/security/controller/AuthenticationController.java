package com.weaponsregistration.security.controller;

import com.weaponsregistration.security.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AccountService accounts;
    private final boolean requireLogin;
    private final AccessPolicy access;
    public AuthenticationController(AccountService accounts, AccessPolicy access, @Value("${erp.security.require-login:false}") boolean requireLogin) {
        this.accounts = accounts;
        this.access = access;
        this.requireLogin = requireLogin;
    }
    public record SessionView(boolean requireLogin, AccountService.AccountView user, AccessPolicy.AccessView access) {}
    public record TokenView(String headerName, String token) {}

    @GetMapping("/session")
    public SessionView session(Authentication authentication) {
        var principal = authentication != null && authentication.getPrincipal() instanceof AccountPrincipal p ? p : null;
        return new SessionView(requireLogin, principal == null ? null : accounts.current(principal), access.current());
    }

    @GetMapping("/csrf")
    public TokenView csrf(CsrfToken token) { return new TokenView(token.getHeaderName(), token.getToken()); }
}
