"use client"

import { RecordWorkspace } from "components/erp/shared/record-workspace"
import type { ErpResource } from "api/models/erp"

interface CoreWorkspaceProps {
    initialResource?: string
    section?: string
}

const organizationNatureResource: ErpResource = {
    key: "organization-natures",
    label: "Organization natures",
    group: "Institutional",
    fields: [
        { name: "code", label: "Code", type: "text", required: true, reference: null, choices: [] },
        { name: "name", label: "Name", type: "text", required: true, reference: null, choices: [] },
        { name: "description", label: "Description", type: "text", required: false, reference: null, choices: [] },
        { name: "active", label: "Active", type: "boolean", required: true, reference: null, choices: [] }
    ]
}

const economicActivityResource: ErpResource = {
    key: "economic-activities",
    label: "Economic activities",
    group: "Institutional",
    fields: [
        { name: "code", label: "Code", type: "text", required: true, reference: null, choices: [] },
        { name: "description", label: "Economic activity description", type: "text", required: true, reference: null, choices: [] },
        { name: "active", label: "Active", type: "boolean", required: true, reference: null, choices: [] }
    ]
}


const organizationalUnitTypeResource: ErpResource = {
    key: "unit-types",
    label: "Organizational unit types",
    group: "Institutional",
    fields: [
        { name: "code", label: "Code", type: "text", required: true, reference: null, choices: [] },
        { name: "name", label: "Name", type: "text", required: true, reference: null, choices: [] },
        { name: "description", label: "Description", type: "text", required: false, reference: null, choices: [] },
        { name: "active", label: "Active", type: "boolean", required: true, reference: null, choices: [] }
    ]
}


const personTypeResource: ErpResource = {
    key: "person-types",
    label: "Person types",
    group: "People",
    fields: [
        { name: "code", label: "Code", type: "text", required: true, reference: null, choices: [] },
        { name: "name", label: "Name", type: "text", required: true, reference: null, choices: [] },
        { name: "description", label: "Description", type: "text", required: false, reference: null, choices: [] },
        { name: "active", label: "Active", type: "boolean", required: true, reference: null, choices: [] }
    ]
}

const contactTypeResource: ErpResource = {
    key: "contact-types",
    label: "Contact types",
    group: "People",
    fields: [
        { name: "code", label: "Code", type: "text", required: true, reference: null, choices: [] },
        { name: "name", label: "Name", type: "text", required: true, reference: null, choices: [] },
        { name: "description", label: "Description", type: "text", required: false, reference: null, choices: [] },
        { name: "addressEnabled", label: "Use for addresses", type: "boolean", required: true, reference: null, choices: [] },
        { name: "phoneEnabled", label: "Use for phones", type: "boolean", required: true, reference: null, choices: [] },
        { name: "emailEnabled", label: "Use for e-mails", type: "boolean", required: true, reference: null, choices: [] },
        { name: "active", label: "Active", type: "boolean", required: true, reference: null, choices: [] }
    ]
}


const accessProfileLevelResource: ErpResource = {
    key: "profile-levels",
    label: "Access profile levels",
    group: "Access",
    fields: [
        { name: "code", label: "Code", type: "text", required: true, reference: null, choices: [] },
        { name: "name", label: "Name", type: "text", required: true, reference: null, choices: [] },
        { name: "description", label: "Description", type: "text", required: false, reference: null, choices: [] },
        { name: "active", label: "Active", type: "boolean", required: true, reference: null, choices: [] }
    ]
}

const permissionResourceResource: ErpResource = {
    key: "permission-resources",
    label: "Permission resources",
    group: "Access",
    fields: [
        { name: "code", label: "Code", type: "text", required: true, reference: null, choices: [] },
        { name: "name", label: "Name", type: "text", required: true, reference: null, choices: [] },
        { name: "description", label: "Description", type: "text", required: false, reference: null, choices: [] },
        { name: "active", label: "Active", type: "boolean", required: true, reference: null, choices: [] }
    ]
}

const permissionActionResource: ErpResource = {
    key: "permission-actions",
    label: "Permission actions",
    group: "Access",
    fields: [
        { name: "code", label: "Code", type: "text", required: true, reference: null, choices: [] },
        { name: "name", label: "Name", type: "text", required: true, reference: null, choices: [] },
        { name: "description", label: "Description", type: "text", required: false, reference: null, choices: [] },
        { name: "active", label: "Active", type: "boolean", required: true, reference: null, choices: [] }
    ]
}

const sectionTabs: Record<
    string,
    Array<{
        resource: string
        label: string
        definition?: ErpResource
    }>
> = {
    institutional: [
        {
            resource: "organization-natures",
            label: "Organization natures",
            definition: organizationNatureResource
        },
        {
            resource: "economic-activities",
            label: "Economic activities",
            definition: economicActivityResource
        },
        {
            resource: "organizations",
            label: "Organizations"
        },
        {
            resource: "unit-types",
            label: "Organizational unit types",
            definition: organizationalUnitTypeResource
        },
        {
            resource: "units",
            label: "Organizational units"
        }
    ],
    people: [
        {
            resource: "people",
            label: "People"
        },
        {
            resource: "person-types",
            label: "Person types",
            definition: personTypeResource
        },
        {
            resource: "contact-types",
            label: "Contact types",
            definition: contactTypeResource
        },
        {
            resource: "roles",
            label: "Person roles"
        },
        {
            resource: "person-roles",
            label: "Person role assignments"
        },
        {
            resource: "credentials",
            label: "Credentials"
        },
        {
            resource: "qualifications",
            label: "Qualifications"
        }
    ],
    access: [
        {
            resource: "users",
            label: "System users"
        },
        {
            resource: "profile-levels",
            label: "Access profile levels",
            definition: accessProfileLevelResource
        },
        {
            resource: "profiles",
            label: "Access profiles"
        },
        {
            resource: "permission-resources",
            label: "Permission resources",
            definition: permissionResourceResource
        },
        {
            resource: "permission-actions",
            label: "Permission actions",
            definition: permissionActionResource
        },
        {
            resource: "permissions",
            label: "Permissions"
        },
        {
            resource: "user-profiles",
            label: "User profiles"
        },
        {
            resource: "profile-permissions",
            label: "Profile permissions"
        }
    ]
}

export function CoreWorkspace({
    initialResource,
    section
}: CoreWorkspaceProps) {

    const normalizedResource = initialResource === "role-data"
        ? "person-roles"
        : initialResource

    const tabs = section
        ? sectionTabs[section]
        : undefined

    const firstTabResource = tabs?.[0]?.resource
    const requestedResource =
        normalizedResource && tabs?.some(tab => tab.resource === normalizedResource)
            ? normalizedResource
            : firstTabResource ?? normalizedResource ?? "organizations"

    return (
        <RecordWorkspace
            key={`${section ?? "all"}:${requestedResource}`}
            module="core"
            title="Institutional core"
            initialResource={requestedResource}
            description="Manage organizations, people, their roles, and system account assignments."
            showNavigation={false}
            tabs={tabs}
        />
    )

}
