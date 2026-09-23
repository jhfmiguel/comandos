package com.comandos.security.service;

import com.comandos.security.api.AuthorizationService;
import org.springframework.stereotype.Component;

@Component
public class AccessPolicyAuthorizationService
    implements AuthorizationService {

    private final AccessPolicy accessPolicy;

    public AccessPolicyAuthorizationService(AccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    @Override
    public boolean can(
        String resource,
        String action,
        Long organizationId,
        Long unitId
    ) {
        return accessPolicy.canScope(
            resource,
            action,
            organizationId,
            unitId
        );
    }
}
