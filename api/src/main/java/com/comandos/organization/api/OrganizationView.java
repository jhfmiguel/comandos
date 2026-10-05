package com.comandos.organization.api;

@Deprecated(forRemoval = true)
public record OrganizationView(
    long id,
    String nature,
    String economicActivity,
    String name,
    String acronym,
    String taxId,
    boolean publicOrganization,
    boolean active
) {
}
