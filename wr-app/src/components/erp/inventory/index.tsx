"use client";

import { RecordWorkspace } from "components/erp/shared/record-workspace";

export function InventoryWorkspace() {
    return <RecordWorkspace 
                module="inventory" 
                title="Assets and inventory" 
                initialResource="categories"
                description="Manage equipment models, firearm and ammunition specifications, registrations, individual assets, stock lots, and technical characteristics." 
            />;
}
