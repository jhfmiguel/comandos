import { CoreWorkspace } from "components/erp/core"

interface CorePageProps {
    searchParams: Promise<{
        resource?: string
        section?: string
    }>
}

export default async function CorePage({
    searchParams
}: CorePageProps) {

    const params = await searchParams

    return (
        <CoreWorkspace
            initialResource={params.resource}
            section={params.section}
        />
    )

}