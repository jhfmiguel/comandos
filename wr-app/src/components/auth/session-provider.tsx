"use client";

import * as React from "react";
import { usePathname, useRouter } from "next/navigation";
import { Button } from "@primereact/ui/button";
import { Message } from "components/common/message";
import { httpClient } from "api/http";

interface Account { id: number; login: string; name: string }
interface Grant { resource: string; action: string; scope: string; organizationId: number; unitId: number | null }
interface Session { requireLogin: boolean; user: Account | null; access?: { enforced: boolean; grants: Grant[] } }
interface SessionContextValue {
    session: Session | null; refresh: () => Promise<void>; signOut: () => Promise<void>;
    can: (resource: string, action: string, organizationId?: number, unitId?: number) => boolean;
}
const SessionContext = React.createContext<SessionContextValue | null>(null);

export function useSession() {
    const context = React.useContext(SessionContext);
    if (!context) throw new Error("SessionProvider is required.");
    return context;
}

export function SessionProvider({ children }: { children: React.ReactNode }) {
    const [session, setSession] = React.useState<Session | null>(null);
    const [error, setError] = React.useState("");
    const pathname = usePathname();
    const isBot = pathname === "/bot";
    const router = useRouter();
    const refresh = React.useCallback(async () => {
        try {
            const response = await httpClient.get<Session>("/api/auth/session");
            setSession(response.data); setError("");
        } catch {
            setSession(null);
            setError("Unable to reach the API. Make sure the backend is running and retry.");
        }
    }, []);
    React.useEffect(() => {
        if (isBot) return;
        const controller = new AbortController();
        httpClient.get<Session>("/api/auth/session", { signal: controller.signal }).then(response => {
            if (!controller.signal.aborted) { setSession(response.data); setError(""); }
        }).catch(() => {
            if (!controller.signal.aborted) setError("Unable to reach the API. Make sure the backend is running and retry.");
        });
        const expired = () => { setSession(null); void refresh(); };
        const accessChanged = () => { void refresh(); };
        window.addEventListener("session-expired", expired);
        window.addEventListener("access-changed", accessChanged);
        window.addEventListener("focus", accessChanged);
        return () => {
            controller.abort();
            window.removeEventListener("session-expired", expired);
            window.removeEventListener("access-changed", accessChanged);
            window.removeEventListener("focus", accessChanged);
        };
    }, [refresh, isBot]);
    React.useEffect(() => {
        if (!isBot && session?.requireLogin && !session.user && pathname !== "/login") {
            router.replace(`/login?returnTo=${encodeURIComponent(pathname)}`);
        }
    }, [session, pathname, router, isBot]);
    async function signOut() {
        await httpClient.post("/api/auth/logout");
        await refresh();
        router.replace("/login");
    }
    const canDisplay = isBot || pathname === "/login" || session && (!session.requireLogin || session.user);
    const can = (resource: string, action: string, organizationId?: number, unitId?: number) => !session?.access?.enforced || session.access.grants.some(grant =>
        (grant.resource === resource || grant.resource === "*") && (grant.action === action || grant.action === "*") &&
        (grant.scope === "SYSTEM" || grant.scope === "ORGANIZATION" && (organizationId === undefined || grant.organizationId === organizationId)
            || grant.scope === "UNIT" && organizationId !== undefined && unitId !== undefined
                && grant.organizationId === organizationId && grant.unitId === unitId));
    return <SessionContext.Provider value={{ session, refresh, signOut, can }}>
        {canDisplay ? <>
            {!isBot && pathname !== "/login" && !session?.requireLogin && <div className="px-4 pt-3"><Message type="info" text="Setup mode: sign-in is optional. Create an access account before enabling protected access." /></div>}
            {children}
        </> : <main className="max-w-lg mx-auto p-6">
            {error ? <><Message type="error" text={error} /><Button onClick={() => { setError(""); void refresh(); }}>Retry</Button></> : <p role="status">Checking session…</p>}
        </main>}
    </SessionContext.Provider>;
}
