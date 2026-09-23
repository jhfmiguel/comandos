"use client"

import * as React from "react"

export type PlatformErrorFallbackProps = {
    error: Error & { digest?: string }
    reset: () => void
}

export function PlatformErrorFallback({
    error,
    reset
}: PlatformErrorFallbackProps) {
    React.useEffect(() => {
        console.error("Unhandled application error", {
            name: error.name,
            digest: error.digest
        })
    }, [error])

    return (
        <main className="flex min-h-[60vh] items-center justify-center px-6 py-12">
            <section
                className="w-full max-w-xl rounded-2xl border border-white/10 bg-black/20 p-8 text-center shadow-2xl"
                role="alert"
                aria-live="assertive"
            >
                <p className="text-sm font-semibold uppercase tracking-[0.18em] opacity-70">
                    Erro inesperado
                </p>
                <h1 className="mt-3 text-2xl font-semibold">
                    Não foi possível concluir esta operação.
                </h1>
                <p className="mt-3 text-sm opacity-75">
                    Tente novamente. Se o problema continuar, informe o código da ocorrência ao suporte.
                </p>

                {error.digest ? (
                    <p className="mt-4 font-mono text-xs opacity-60">
                        Código: {error.digest}
                    </p>
                ) : null}

                <button
                    type="button"
                    onClick={reset}
                    className="mt-6 rounded-lg border border-current px-5 py-2 text-sm font-semibold transition-opacity hover:opacity-80"
                >
                    Tentar novamente
                </button>
            </section>
        </main>
    )
}
