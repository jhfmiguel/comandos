"use client"

import { RecordWorkspace } from "components/erp/shared/record-workspace"

interface CoreWorkspaceProps {
    initialResource?: string
    section?: string
}

const sectionTabs: Record<
    string,
    Array<{
        resource: string
        label: string
    }>
> = {
    institutional: [
        {
            resource: "organization-natures",
            label: "Organization natures"
        },
        {
            resource: "economic-activities",
            label: "Economic activities"
        },
        {
            resource: "organizations",
            label: "Organizations"
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
            resource: "roles",
            label: "Person roles"
        },
        {
            resource: "person-roles",
            label: "Person role assignments"
        },
        {
            resource: "role-data",
            label: "Role details"
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
            resource: "profiles",
            label: "Access profiles"
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
    initialResource = "organizations",
    section
}: CoreWorkspaceProps) {

    const tabs = section
        ? sectionTabs[section]
        : undefined

    const firstTabResource = tabs?.[0]?.resource

    return (
        <RecordWorkspace
            module="core"
            title="Institutional core"
            initialResource={
                firstTabResource ?? initialResource
            }
            description="Manage organizations, people, their roles, and system account assignments."
            showNavigation={false}
            tabs={tabs}
        />
    )

}
