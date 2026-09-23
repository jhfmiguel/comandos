"use client"

import { PlatformErrorFallback } from "platform/error-boundary"

export default function GlobalError({
    error,
    reset
}: {
    error: Error & { digest?: string }
    reset: () => void
}) {
    return (
        <html lang="pt-BR">
            <body>
                <PlatformErrorFallback error={error} reset={reset} />
            </body>
        </html>
    )
}
