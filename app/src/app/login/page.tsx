"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import axios from "axios";
import { Button } from "@primereact/ui/button";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import { httpClient } from "api/http";
import styles from "components/erp/shared/workspace.module.css";

export default function LoginPage() {
    const [login, setLogin] = React.useState("");
    const [password, setPassword] = React.useState("");
    const [busy, setBusy] = React.useState(false);
    const [error, setError] = React.useState("");
    const { session, refresh } = useSession();
    const router = useRouter();
    async function submit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (busy) return;
        setBusy(true); setError("");
        try {
            await httpClient.post("/api/auth/login", new URLSearchParams({ username: login, password }),
                { headers: { "Content-Type": "application/x-www-form-urlencoded" } });
            setPassword("");
            await refresh();
            const returnTo = new URLSearchParams(window.location.search).get("returnTo") || "/";
            router.replace(returnTo.startsWith("/") && !returnTo.startsWith("//") && !returnTo.includes("\\") ? returnTo : "/");
        } catch (error) {
            setError(axios.isAxiosError(error) && typeof error.response?.data?.detail === "string"
                ? error.response.data.detail : "Unable to sign in. Check the API connection and try again.");
        } finally { setBusy(false); }
    }
    return <main className={`w-full max-w-md mx-auto p-6 mt-12 ${styles.workspace}`}>
        <h1 className="text-2xl font-semibold mb-2">Sign in</h1>
        <p className={styles.intro}>Use your system access account to continue.</p>
        {error && <Message type="error" text={error} onClose={() => setError("")} />}
        <form onSubmit={submit}>
            <fieldset disabled={busy} className={`${styles.field} gap-4!`}>
                <div className={styles.field}><label htmlFor="login">Login</label>
                    <input id="login" name="username" autoComplete="username" required maxLength={255} value={login} onChange={event => setLogin(event.target.value)} /></div>
                <div className={styles.field}><label htmlFor="password">Password</label>
                    <input id="password" name="password" type="password" autoComplete="current-password" required value={password} onChange={event => setPassword(event.target.value)} /></div>
                <Button type="submit" className="registration-yellow-button">{busy ? "Signing in…" : "Sign in"}</Button>
            </fieldset>
        </form>
        {session && !session.requireLogin && <p className="mt-4"><Link href="/erp/core">Continue setup</Link></p>}
    </main>;
}
