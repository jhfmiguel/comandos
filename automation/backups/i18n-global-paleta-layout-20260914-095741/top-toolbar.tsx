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
                    : state === "paused" ||
                        state === "pause-requested"
                        ? t("botPaused")
                        : state === "codex-offline" ||
                            state === "offline" ||
                            state === "unresponsive"
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
            aria-label={t("quickNavigation")}
        >
            <button
                type="button"
                className="comandos-top-action comandos-theme-toggle"
                onClick={toggleTheme}
                aria-label={
                    theme === "dark"
                        ? t("activateLight")
                        : t("activateDark")
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
                className="comandos-bot-indicator comandos-bot-status-centered"
                title={t("commandCenter")}
                aria-label={`${t("bot")}: ${stateLabel}`}
            >
                <span
                    className={
                        `comandos-bot-status-light comandos-bot-status-${state}`
                    }
                    aria-hidden="true"
                />
            </Link>
        </nav>
    )
}
