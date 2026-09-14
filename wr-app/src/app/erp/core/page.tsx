import { CoreWorkspace } from "components/erp/core"

interface CorePageProps {
    searchParams: Promise<{
        resource?: string
    }>
}

export default async function CorePage({
    searchParams
}: CorePageProps) {

    const params = await searchParams

    return (
        <CoreWorkspace
            initialResource={params.resource}
        />
    )

}