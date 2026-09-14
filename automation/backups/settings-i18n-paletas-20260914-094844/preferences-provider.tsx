"use client"

import * as React from "react"

export type ComandosLocale = "pt-BR" | "en-US"
export type ComandosTheme = "dark" | "light"

type PreferencesContextValue = {
    locale: ComandosLocale
    theme: ComandosTheme
    setLocale: (locale: ComandosLocale) => void
    setTheme: (theme: ComandosTheme) => void
    toggleTheme: () => void
    t: (key: TranslationKey) => string
}

type TranslationKey =
    | "institutionalCore"
    | "commandCenter"
    | "bot"
    | "settings"
    | "settingsTitle"
    | "language"
    | "portuguese"
    | "english"
    | "appearance"
    | "light"
    | "dark"
    | "savedAutomatically"
    | "botWorking"
    | "botWaiting"
    | "botPaused"
    | "botOffline"
    | "botStopped"
    | "botFailed"
    | "botCompleted"
    | "botValidating"
    | "botStarting"

const translations: Record<ComandosLocale, Record<TranslationKey, string>> = {
    "pt-BR": {
        institutionalCore: "NÃºcleo institucional",
        commandCenter: "Command Center",
        bot: "Bot",
        settings: "ConfiguraÃ§Ãµes",
        settingsTitle: "ConfiguraÃ§Ãµes do ERP",
        language: "Idioma",
        portuguese: "PortuguÃªs",
        english: "InglÃªs",
        appearance: "AparÃªncia",
        light: "Claro",
        dark: "Escuro",
        savedAutomatically: "As preferÃªncias sÃ£o salvas automaticamente neste navegador.",
        botWorking: "trabalhando",
        botWaiting: "aguardando",
        botPaused: "pausado",
        botOffline: "Codex indisponÃ­vel",
        botStopped: "parado",
        botFailed: "falhou",
        botCompleted: "concluÃ­do",
        botValidating: "validando",
        botStarting: "iniciando"
    },
    "en-US": {
        institutionalCore: "Institutional core",
        commandCenter: "Command Center",
        bot: "Bot",
        settings: "Settings",
        settingsTitle: "ERP settings",
        language: "Language",
        portuguese: "Portuguese",
        english: "English",
        appearance: "Appearance",
        light: "Light",
        dark: "Dark",
        savedAutomatically: "Preferences are saved automatically in this browser.",
        botWorking: "working",
        botWaiting: "waiting",
        botPaused: "paused",
        botOffline: "Codex unavailable",
        botStopped: "stopped",
        botFailed: "failed",
        botCompleted: "completed",
        botValidating: "validating",
        botStarting: "starting"
    }
}

const PreferencesContext = React.createContext<PreferencesContextValue | null>(null)

export function PreferencesProvider({
    children
}: {
    children: React.ReactNode
}) {
    const [locale, setLocaleState] = React.useState<ComandosLocale>(() => {
        if (typeof window === "undefined") {
            return "pt-BR"
        }

        const storedLocale =
            window.localStorage.getItem("comandos-locale")

        return storedLocale === "en-US"
            ? "en-US"
            : "pt-BR"
    })

    const [theme, setThemeState] = React.useState<ComandosTheme>(() => {
        if (typeof window === "undefined") {
            return "dark"
        }

        const storedTheme =
            window.localStorage.getItem("comandos-theme")

        if (storedTheme === "dark" || storedTheme === "light") {
            return storedTheme
        }

        return window.matchMedia("(prefers-color-scheme: light)").matches
            ? "light"
            : "dark"
    })

    React.useEffect(() => {
        document.documentElement.lang = locale
        document.documentElement.dataset.theme = theme

        window.localStorage.setItem(
            "comandos-locale",
            locale
        )

        window.localStorage.setItem(
            "comandos-theme",
            theme
        )
    }, [locale, theme])

    const setLocale = React.useCallback((value: ComandosLocale) => {
        setLocaleState(value)
    }, [])

    const setTheme = React.useCallback((value: ComandosTheme) => {
        setThemeState(value)
    }, [])

    const toggleTheme = React.useCallback(() => {
        setThemeState((current) => current === "dark" ? "light" : "dark")
    }, [])

    const t = React.useCallback(
        (key: TranslationKey) => translations[locale][key],
        [locale]
    )

    const value = React.useMemo(
        () => ({
            locale,
            theme,
            setLocale,
            setTheme,
            toggleTheme,
            t
        }),
        [locale, theme, setLocale, setTheme, toggleTheme, t]
    )

    return (
        <PreferencesContext.Provider value={value}>
            {children}
        </PreferencesContext.Provider>
    )
}

export function useComandosPreferences() {
    const context = React.useContext(PreferencesContext)

    if (!context) {
        throw new Error("useComandosPreferences must be used inside PreferencesProvider")
    }

    return context
}