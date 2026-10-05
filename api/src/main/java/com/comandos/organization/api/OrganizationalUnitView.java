package com.comandos.organization.api;

@Deprecated(forRemoval = true)
public record OrganizationalUnitView(
    long id,
    long organizationId,
    Long parentUnitId,
    String code,
    String name,
    String type,
    boolean active
) {
}
