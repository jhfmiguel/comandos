package com.comandos.organization.api;

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
