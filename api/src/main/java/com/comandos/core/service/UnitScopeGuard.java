package com.comandos.core.service;

/** Modules implement this hook to protect their unit-scoped relationships. */
public interface UnitScopeGuard {
    void validateOrganizationChange(long unitId, long organizationId);
}
