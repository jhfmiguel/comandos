"use client"

import * as React from "react"
import { usePathname, useRouter } from "next/navigation"
import {
    AuthSessionProvider,
    useAuthSession
} from "@faria-miguel/platform/auth-session"
import type { PlatformSession } from "@faria-miguel/platform/auth"

import { Button } from "components/common/button"
import { Message } from "components/common/message"
import { httpClient } from "api/http"
import { productDefinition } from "platform/product"

interface SessionContextValue {
    session: PlatformSession | null
    refresh: () => Promise<void>
    signOut: () => Promise<void>
    can: (
        resource: string,
        action: string,
        organizationId?: number,
        unitId?: number
    ) => boolean
}

const SessionContext = React.createContext<SessionContextValue | null>(null)

export function useSession() {
    const context = React.useContext(SessionContext)
    if (!context) throw new Error("SessionProvider is required.")
    return context
}

function ComandosSessionOverlay({ children }: { children: React.ReactNode }) {
    const {
        session,
        loading,
        error,
        refresh,
        signOut: sharedSignOut,
        can: sharedCan
    } = useAuthSession()

    const pathname = usePathname()
    const router = useRouter()

    React.useEffect(() => {
        const refreshSession = () => {
            void refresh()
        }

        window.addEventListener("session-expired", refreshSession)
        window.addEventListener("access-changed", refreshSession)
        window.addEventListener("focus", refreshSession)

        return () => {
            window.removeEventListener("session-expired", refreshSession)
            window.removeEventListener("access-changed", refreshSession)
            window.removeEventListener("focus", refreshSession)
        }
    }, [refresh])

    React.useEffect(() => {
        if (
            !loading
            && session?.requireLogin
            && !session.user
            && pathname !== productDefinition.loginPath
        ) {
            router.replace(
                `${productDefinition.loginPath}?returnTo=${encodeURIComponent(pathname)}`
            )
        }
    }, [loading, session, pathname, router])

    const signOut = React.useCallback(async () => {
        await sharedSignOut()
        router.replace(productDefinition.loginPath)
    }, [sharedSignOut, router])

    const can = React.useCallback((
        resource: string,
        action: string,
        organizationId?: number,
        unitId?: number
    ) => sharedCan({
        resource,
        action,
        organizationId,
        unitId
    }), [sharedCan])

    const canDisplay =
        pathname === productDefinition.loginPath
        || Boolean(session && (!session.requireLogin || session.user))

    return (
        <SessionContext.Provider value={{ session, refresh, signOut, can }}>
            {canDisplay ? (
                <>
                    {pathname !== productDefinition.loginPath && !session?.requireLogin && (
                        <Message
                            type="info"
                            text="Setup mode: sign-in is optional. Create an access account before enabling protected access."
                        />
                    )}
                    {children}
                </>
            ) : (
                <main className="max-w-lg mx-auto p-6">
                    {error ? (
                        <>
                            <Message type="error" text={error} />
                            <Button onClick={() => void refresh()}>Retry</Button>
                        </>
                    ) : (
                        <p role="status">
                            {loading ? "Checking session…" : "Redirecting to sign-in…"}
                        </p>
                    )}
                </main>
            )}
        </SessionContext.Provider>
    )
}

export function SessionProvider({ children }: { children: React.ReactNode }) {
    return (
        <AuthSessionProvider
            client={httpClient}
            sessionEndpoint="/api/auth/session"
            logoutEndpoint="/api/auth/logout"
        >
            <ComandosSessionOverlay>
                {children}
            </ComandosSessionOverlay>
        </AuthSessionProvider>
    )
}
