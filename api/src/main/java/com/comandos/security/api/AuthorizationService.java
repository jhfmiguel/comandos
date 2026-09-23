package com.comandos.security.api;

public interface AuthorizationService {

    boolean can(
        String resource,
        String action,
        Long organizationId,
        Long unitId
    );

    default void require(
        String resource,
        String action,
        Long organizationId,
        Long unitId
    ) {
        if (!can(resource, action, organizationId, unitId)) {
            throw new AccessDeniedException(
                "You do not have permission for this operation or scope."
            );
        }
    }
}
