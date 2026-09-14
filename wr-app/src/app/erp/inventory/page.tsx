import { InventoryWorkspace } from "components/erp/inventory"

interface InventoryPageProps {
    searchParams: Promise<{
        resource?: string
    }>
}

export default async function InventoryPage({
    searchParams
}: InventoryPageProps) {

    const params = await searchParams

    return (
        <InventoryWorkspace
            initialResource={params.resource}
        />
    )

}