"use client"

import { RecordWorkspace } from "components/erp/shared/record-workspace"

interface InventoryWorkspaceProps {
    initialResource?: string
}

export function InventoryWorkspace({
    initialResource = "categories"
}: InventoryWorkspaceProps) {

    return (
        <RecordWorkspace
            module="inventory"
            title="Assets and inventory"
            initialResource={initialResource}
            description="Manage equipment models, firearm and ammunition specifications, registrations, individual assets, stock lots, and technical characteristics."
            showNavigation={false}
        />
    )

}