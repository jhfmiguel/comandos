"use client"

import { ConfigurationConsole } from "@faria-miguel/configuration-console"
import "@faria-miguel/configuration-console/styles.css"
import { Layout } from "components/layout"

export default function PlatformConfigurationPage() {
    return (
        <Layout title="Configurações da plataforma">
            <ConfigurationConsole
                apiUrl={process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}
                showLogin={false}
            />
        </Layout>
    )
}
