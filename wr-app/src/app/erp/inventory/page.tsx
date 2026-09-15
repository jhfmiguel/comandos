import { InventoryWorkspace } from "components/erp/inventory"

interface InventoryPageProps {
    searchParams: Promise<{
        resource?: string
        section?: string
    }>
}

export default async function InventoryPage({ searchParams }: InventoryPageProps) {
    const params = await searchParams
    return <InventoryWorkspace initialResource={params.resource} section={params.section} />
}