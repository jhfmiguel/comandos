"use client";

import { RecordWorkspace } from "components/erp/shared/record-workspace";

export function CoreWorkspace() {
    return <RecordWorkspace 
                module="core" 
                title="Institutional core" 
                initialResource="organizations"
                description="Manage organizations, people, their roles, and system account assignments." 
            />;
}
