"use client"

import { PlatformErrorFallback } from "@faria-miguel/platform/error-boundary"

export default function Error({
    error,
    reset
}: {
    error: Error & { digest?: string }
    reset: () => void
}) {
    return <PlatformErrorFallback error={error} reset={reset} />
}
