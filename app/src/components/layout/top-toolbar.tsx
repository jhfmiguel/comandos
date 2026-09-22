"use client"

import { Moon, Sun } from "components/ui/icons"

import { useComandosPreferences } from "components/settings/preferences-provider"

export function TopToolbar() {
    const {
        theme,
        toggleTheme,
        t
    } = useComandosPreferences()

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
        </nav>
    )
}
