package com.comandos.core.service;

import com.comandos.core.model.*;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Explicit allowlist shared by API validation and form metadata. */
public final class CoreCatalog {
    private CoreCatalog() {}

    public record Field(String name, String label, String type, boolean required,
                        String reference, List<String> choices) {
        public String property() {
            return reference == null ? name : name.substring(0, name.length() - 2);
        }
    }
    public record Resource(String key, String label, String group,
                           Class<? extends CoreEntity> entity, List<Field> fields) {}

    public static final List<Resource> RESOURCES = List.of(
        new Resource("organization-natures", "Organization natures", "Institutional", OrganizationNature.class, List.of(
            new Field("code", "Code", "text", true, null, List.of()),
            new Field("name", "Name", "text", true, null, List.of()),
            new Field("description", "Description", "text", false, null, List.of()),
            new Field("active", "Active", "boolean", true, null, List.of())
        )),
        new Resource("economic-activities", "Economic activities", "Institutional", EconomicActivity.class, List.of(
            new Field("code", "Code", "text", true, null, List.of()),
            new Field("description", "Economic activity description", "text", true, null, List.of()),
            new Field("active", "Active", "boolean", true, null, List.of())
        )),
        new Resource("unit-types", "Organizational unit types", "Institutional", OrganizationalUnitType.class, List.of(
            new Field("code", "Code", "text", true, null, List.of()),
            new Field("name", "Name", "text", true, null, List.of()),
            new Field("description", "Description", "text", false, null, List.of()),
            new Field("active", "Active", "boolean", true, null, List.of())
        )),
        new Resource("person-types", "Person types", "People", PersonType.class, List.of(
            new Field("code", "Code", "text", true, null, List.of()),
            new Field("name", "Name", "text", true, null, List.of()),
            new Field("description", "Description", "text", false, null, List.of()),
            new Field("active", "Active", "boolean", true, null, List.of())
        )),
        new Resource("organizations", "Organizations", "Institutional", Organization.class, List.of(
            new Field("natureId", "Nature", "reference", true, "organization-natures", List.of()),
            new Field("economicActivityId", "Economic activity description", "reference", true, "economic-activities", List.of()),
            new Field("name", "Name", "text", true, null, List.of()),
            new Field("acronym", "Acronym", "text", false, null, List.of()),
            new Field("taxId", "Tax ID (CNPJ)", "text", false, null, List.of()),
            new Field("publicOrganization", "Public organization", "boolean", true, null, List.of()),
            new Field("active", "Active", "boolean", true, null, List.of())
        )),
        new Resource("units", "Organizational units", "Institutional", OrganizationalUnit.class, List.of(
            new Field("organizationId", "Organization", "reference", true, "organizations", List.of()),
            new Field("parentUnitId", "Parent unit", "reference", false, "units", List.of()),
            new Field("code", "Code", "text", true, null, List.of()),
            new Field("name", "Name", "text", true, null, List.of()),
            new Field("unitTypeId", "Type", "reference", true, "unit-types", List.of()),
            new Field("active", "Active", "boolean", false, null, List.of())
        )),
        new Resource("people", "People", "People", Person.class, List.of(
            new Field("personTypeRefId", "Person type", "reference", true, "person-types", List.of()),
            new Field("fullName", "Full name / legal name", "text", true, null, List.of()),
            new Field("taxId", "Tax ID (CPF / CNPJ)", "text", false, null, List.of()),
            new Field("birthDate", "Birth date", "date", false, null, List.of()),
            new Field("active", "Active", "boolean", true, null, List.of())
        )),
        new Resource("person-addresses", "Endereços", "People", PersonAddress.class, List.of(
            new Field("personId", "Person", "reference", true, "people", List.of()),
            new Field("type", "Tipo de endereço", "choice", true, null, List.of("RESIDENTIAL", "BUSINESS", "MAILING", "OTHER")),
            new Field("foreignAddress", "Endereço no exterior", "boolean", false, null, List.of()),
            new Field("country", "País", "text", false, null, List.of()),
            new Field("postalCode", "CEP / código postal", "text", false, null, List.of()),
            new Field("street", "Logradouro", "text", true, null, List.of()),
            new Field("number", "Número", "text", true, null, List.of()),
            new Field("complement", "Complemento", "text", false, null, List.of()),
            new Field("district", "Bairro", "text", false, null, List.of()),
            new Field("city", "Cidade", "text", true, null, List.of()),
            new Field("state", "UF", "text", true, null, List.of()),
            new Field("primaryAddress", "Endereço principal", "boolean", true, null, List.of())
        )),
        new Resource("person-phones", "Telefones", "People", PersonPhone.class, List.of(
            new Field("personId", "Person", "reference", true, "people", List.of()),
            new Field("type", "Tipo de telefone", "choice", true, null, List.of("MOBILE", "WHATSAPP", "HOME", "WORK", "OTHER")),
            new Field("countryCode", "Código do país", "text", false, null, List.of()),
            new Field("number", "Telefone", "text", true, null, List.of()),
            new Field("whatsapp", "WhatsApp", "boolean", true, null, List.of()),
            new Field("primaryPhone", "Telefone principal", "boolean", true, null, List.of())
        )),
        new Resource("person-emails", "E-mails", "People", PersonEmail.class, List.of(
            new Field("personId", "Person", "reference", true, "people", List.of()),
            new Field("type", "Tipo de e-mail", "choice", true, null, List.of("PERSONAL", "WORK", "OTHER")),
            new Field("email", "E-mail", "email", true, null, List.of()),
            new Field("primaryEmail", "E-mail principal", "boolean", true, null, List.of())
        )),
        new Resource("roles", "Person roles", "People", PersonRole.class, List.of(
            new Field("code", "Code", "text", true, null, List.of()),
            new Field("name", "Name", "text", true, null, List.of())
        )),
        new Resource("person-roles", "Person role assignments", "People", PersonRoleAssignment.class, List.of(
            new Field("personId", "Person", "reference", true, "people", List.of()),
            new Field("roleId", "Role", "reference", true, "roles", List.of()),
            new Field("organizationId", "Organization", "reference", true, "organizations", List.of()),
            new Field("unitId", "Unit", "reference", false, "units", List.of()),
            new Field("startDate", "Start date", "date", true, null, List.of()),
            new Field("endDate", "End date", "date", false, null, List.of()),
            new Field("status", "Status", "choice", true, null, List.of("ACTIVE", "SUSPENDED", "ENDED"))
        )),
        new Resource("role-data", "Role details", "People", RoleData.class, List.of(
            new Field("personRoleId", "Person role assignment", "reference", true, "person-roles", List.of()),
            new Field("key", "Key", "text", true, null, List.of()),
            new Field("value", "Value", "text", true, null, List.of())
        )),
        new Resource("users", "System users", "Access", SystemUser.class, List.of(
            new Field("personId", "Person", "reference", true, "people", List.of()),
            new Field("login", "Login", "text", true, null, List.of()),
            new Field("password", "Password", "password", true, null, List.of()),
            new Field("blocked", "Blocked", "boolean", true, null, List.of())
        )),
        new Resource("profile-levels", "Access profile levels", "Access", AccessProfileLevel.class, List.of(
            new Field("code", "Code", "text", true, null, List.of()),
            new Field("name", "Name", "text", true, null, List.of()),
            new Field("description", "Description", "text", false, null, List.of()),
            new Field("active", "Active", "boolean", true, null, List.of())
        )),
        new Resource("profiles", "Access profiles", "Access", AccessProfile.class, List.of(
            new Field("name", "Name", "text", true, null, List.of()),
            new Field("levelTypeId", "Level", "reference", true, "profile-levels", List.of())
        )),
        new Resource("permission-resources", "Permission resources", "Access", PermissionResource.class, List.of(
            new Field("code", "Code", "text", true, null, List.of()),
            new Field("name", "Name", "text", true, null, List.of()),
            new Field("description", "Description", "text", false, null, List.of()),
            new Field("active", "Active", "boolean", true, null, List.of())
        )),
        new Resource("permission-actions", "Permission actions", "Access", PermissionAction.class, List.of(
            new Field("code", "Code", "text", true, null, List.of()),
            new Field("name", "Name", "text", true, null, List.of()),
            new Field("description", "Description", "text", false, null, List.of()),
            new Field("active", "Active", "boolean", true, null, List.of())
        )),
        new Resource("permissions", "Permissions", "Access", Permission.class, List.of(
            new Field("resourceTypeId", "Resource", "reference", true, "permission-resources", List.of()),
            new Field("actionTypeId", "Action", "reference", true, "permission-actions", List.of())
        )),
        new Resource("user-profiles", "User profiles", "Access", UserProfile.class, List.of(
            new Field("userId", "User", "reference", true, "users", List.of()),
            new Field("profileId", "Profile", "reference", true, "profiles", List.of()),
            new Field("organizationId", "Organization", "reference", true, "organizations", List.of()),
            new Field("unitId", "Unit", "reference", false, "units", List.of())
        )),
        new Resource("profile-permissions", "Profile permissions", "Access", ProfilePermission.class, List.of(
            new Field("profileId", "Profile", "reference", true, "profiles", List.of()),
            new Field("permissionId", "Permission", "reference", true, "permissions", List.of())
        )),
        new Resource("credentials", "Credentials", "People", PersonCredential.class, List.of(
            new Field("personId", "Person", "reference", true, "people", List.of()),
            new Field("type", "Type", "text", true, null, List.of()),
            new Field("number", "Number", "text", true, null, List.of()),
            new Field("validUntil", "Valid until", "date", true, null, List.of())
        )),
        new Resource("qualifications", "Qualifications", "People", PersonQualification.class, List.of(
            new Field("personId", "Person", "reference", true, "people", List.of()),
            new Field("category", "Category", "text", true, null, List.of()),
            new Field("validUntil", "Valid until", "date", true, null, List.of()),
            new Field("status", "Status", "choice", true, null, List.of("ACTIVE", "SUSPENDED", "REVOKED"))
        ))
    );

    public static Resource get(String key) {
        return RESOURCES.stream().filter(r -> r.key().equals(key)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found."));
    }
}
