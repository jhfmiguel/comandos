"use client"

import { RecordWorkspace } from "components/erp/shared/record-workspace"

interface CoreWorkspaceProps {
    initialResource?: string
}

export function CoreWorkspace({
    initialResource = "organizations"
}: CoreWorkspaceProps) {

    return (
        <RecordWorkspace
            module="core"
            title="Institutional core"
            initialResource={initialResource}
            description="Manage organizations, people, their roles, and system account assignments."
            showNavigation={false}
        />
    )

}