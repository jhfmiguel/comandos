"use client"

import Link from "next/link"
import * as React from "react"

import { Moon, Sun } from "@primeicons/react"

import { useComandosPreferences } from "components/settings/preferences-provider"

type BotStatus = {
    state?: string
    message?: string
    taskName?: string
    alive?: boolean
}

const statusClass: Record<string, string> = {
    working: "is-working",
    validating: "is-validating",
    waiting: "is-waiting",
    paused: "is-paused",
    "pause-requested": "is-paused",
    "codex-offline": "is-offline",
    offline: "is-offline",
    unresponsive: "is-offline",
    stopped: "is-stopped",
    failed: "is-failed",
    completed: "is-completed",
    starting: "is-starting"
}

export function TopToolbar() {
    const {
        theme,
        toggleTheme,
        t
    } = useComandosPreferences()

    const [botStatus, setBotStatus] = React.useState<BotStatus>({
        state: "offline",
        message: ""
    })

    React.useEffect(() => {
        let active = true

        const refresh = async () => {
            try {
                const response = await fetch(
                    "/api/bot/status",
                    {
                        cache: "no-store"
                    }
                )

                if (!response.ok) {
                    throw new Error()
                }

                const data = await response.json() as BotStatus

                if (active) {
                    setBotStatus(data)
                }
            } catch {
                if (active) {
                    setBotStatus({
                        state: "offline",
                        message: "Worker offline."
                    })
                }
            }
        }

        void refresh()

        const timer = window.setInterval(
            refresh,
            3000
        )

        return () => {
            active = false
            window.clearInterval(timer)
        }
    }, [])

    const state = botStatus.state || "offline"

    const stateLabel =
        state === "working"
            ? t("botWorking")
            : state === "validating"
                ? t("botValidating")
                : state === "waiting"
                    ? t("botWaiting")
                    : state === "paused" || state === "pause-requested"
                        ? t("botPaused")
                        : state === "codex-offline" || state === "offline" || state === "unresponsive"
                            ? t("botOffline")
                            : state === "stopped"
                                ? t("botStopped")
                                : state === "failed"
                                    ? t("botFailed")
                                    : state === "completed"
                                        ? t("botCompleted")
                                        : t("botStarting")

    return (
        <nav
            className="comandos-top-toolbar"
            aria-label="Comandos quick navigation"
        >
            <button
                type="button"
                className="comandos-top-action comandos-theme-toggle"
                onClick={toggleTheme}
                aria-label={
                    theme === "dark"
                        ? "Ativar modo claro"
                        : "Ativar modo escuro"
                }
                title={
                    theme === "dark"
                        ? t("light")
                        : t("dark")
                }
            >
                {theme === "dark"
                    ? <Sun />
                    : <Moon />
                }
            </button>

            

            

            <Link
                href="/bot"
                className={`comandos-bot-indicator ${statusClass[state] || "is-offline"} comandos-bot-status-centered`}
                title="Command Center"
                aria-label={`${t("bot")}: ${stateLabel}`}
            >
                <span
                    className="comandos-bot-light"
                    aria-hidden="true"
                />
                <span className="comandos-bot-label">
                    {t("bot")}
                </span>
                <span className="comandos-bot-state">
                    {stateLabel}
                </span>
            </Link>
        </nav>
    )
}