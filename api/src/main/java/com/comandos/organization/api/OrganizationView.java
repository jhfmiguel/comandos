package com.comandos.organization.api;

public record OrganizationView(
    long id,
    String nature,
    String name,
    String acronym,
    String taxId,
    boolean publicOrganization,
    boolean active
) {
}
