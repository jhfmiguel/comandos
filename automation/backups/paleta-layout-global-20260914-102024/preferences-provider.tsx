"use client"

import * as React from "react"

import {
    type ComandosLocale,
    translateText
} from "./i18n-catalog"

export type { ComandosLocale } from "./i18n-catalog"

export type ComandosTheme = "dark" | "light"
export type ComandosPalette =
    | "green"
    | "blue"
    | "lilac"
    | "red"
    | "yellow"
    | "orange"

type PreferenceSnapshot = {
    locale: ComandosLocale
    theme: ComandosTheme
    palette: ComandosPalette
}

type TranslationKey =
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
    | "palette"
    | "green"
    | "blue"
    | "lilac"
    | "red"
    | "yellow"
    | "orange"
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
    | "activateLight"
    | "activateDark"
    | "quickNavigation"

type PreferencesContextValue = PreferenceSnapshot & {
    setLocale: (locale: ComandosLocale) => void
    setTheme: (theme: ComandosTheme) => void
    setPalette: (palette: ComandosPalette) => void
    toggleTheme: () => void
    t: (key: TranslationKey) => string
    tr: (text: string) => string
}

const uiTranslations: Record<
    ComandosLocale,
    Record<TranslationKey, string>
> = {
    "pt-BR": {
        commandCenter: "Command Center",
        bot: "Bot",
        settings: "Configurações",
        settingsTitle: "Configurações do ERP",
        language: "Idioma",
        portuguese: "Português",
        english: "Inglês",
        appearance: "Aparência",
        light: "Claro",
        dark: "Escuro",
        palette: "Paleta de cores",
        green: "Verde",
        blue: "Azul",
        lilac: "Lilás",
        red: "Vermelho",
        yellow: "Amarelo",
        orange: "Laranja",
        savedAutomatically:
            "As preferências são salvas automaticamente neste navegador.",
        botWorking: "trabalhando",
        botWaiting: "aguardando",
        botPaused: "pausado",
        botOffline: "Codex indisponível",
        botStopped: "parado",
        botFailed: "falhou",
        botCompleted: "concluído",
        botValidating: "validando",
        botStarting: "iniciando",
        activateLight: "Ativar modo claro",
        activateDark: "Ativar modo escuro",
        quickNavigation: "Navegação rápida do Comandos"
    },
    "en-US": {
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
        palette: "Color palette",
        green: "Green",
        blue: "Blue",
        lilac: "Lilac",
        red: "Red",
        yellow: "Yellow",
        orange: "Orange",
        savedAutomatically:
            "Preferences are saved automatically in this browser.",
        botWorking: "working",
        botWaiting: "waiting",
        botPaused: "paused",
        botOffline: "Codex unavailable",
        botStopped: "stopped",
        botFailed: "failed",
        botCompleted: "completed",
        botValidating: "validating",
        botStarting: "starting",
        activateLight: "Activate light mode",
        activateDark: "Activate dark mode",
        quickNavigation: "Comandos quick navigation"
    }
}

const defaults: PreferenceSnapshot = {
    locale: "pt-BR",
    theme: "dark",
    palette: "orange"
}

const listeners = new Set<() => void>()
let cachedSnapshot: PreferenceSnapshot | null = null
let cachedKey = ""

const readClientSnapshot = (): PreferenceSnapshot => {
    if (typeof window === "undefined") return defaults

    const locale = window.localStorage.getItem("comandos-locale")
    const theme = window.localStorage.getItem("comandos-theme")
    const palette = window.localStorage.getItem("comandos-palette")

    return {
        locale: locale === "en-US" ? "en-US" : "pt-BR",
        theme:
            theme === "light" || theme === "dark"
                ? theme
                : defaults.theme,
        palette:
            palette === "green" ||
            palette === "blue" ||
            palette === "lilac" ||
            palette === "red" ||
            palette === "yellow" ||
            palette === "orange"
                ? palette
                : defaults.palette
    }
}

const getSnapshot = (): PreferenceSnapshot => {
    const next = readClientSnapshot()
    const key = JSON.stringify(next)

    if (!cachedSnapshot || cachedKey !== key) {
        cachedSnapshot = next
        cachedKey = key
    }

    return cachedSnapshot
}

const getServerSnapshot = (): PreferenceSnapshot => defaults

const subscribe = (listener: () => void) => {
    listeners.add(listener)

    const onStorage = () => {
        cachedSnapshot = null
        cachedKey = ""
        listener()
    }

    window.addEventListener("storage", onStorage)

    return () => {
        listeners.delete(listener)
        window.removeEventListener("storage", onStorage)
    }
}

const notify = () => {
    cachedSnapshot = null
    cachedKey = ""
    listeners.forEach((listener) => listener())
}

const store = (key: string, value: string) => {
    window.localStorage.setItem(key, value)
    notify()
}

const PreferencesContext =
    React.createContext<PreferencesContextValue | null>(null)

const TRANSLATABLE_ATTRIBUTES = [
    "title",
    "aria-label",
    "placeholder",
    "alt"
] as const

const shouldIgnore = (element: Element): boolean =>
    Boolean(
        element.closest(
            [
                "[data-i18n-ignore='true']",
                "script",
                "style",
                "code",
                "pre"
            ].join(",")
        )
    )

export function PreferencesProvider({
    children
}: {
    children: React.ReactNode
}) {
    const snapshot = React.useSyncExternalStore(
        subscribe,
        getSnapshot,
        getServerSnapshot
    )

    const { locale, theme, palette } = snapshot

    const tr = React.useCallback(
        (text: string) => translateText(text, locale),
        [locale]
    )

    React.useEffect(() => {
        document.documentElement.lang = locale
        document.documentElement.dataset.theme = theme
        document.documentElement.dataset.palette = palette
    }, [locale, theme, palette])

    React.useEffect(() => {
        const translateElement = (element: Element) => {
            if (shouldIgnore(element)) return

            for (const attribute of TRANSLATABLE_ATTRIBUTES) {
                const value = element.getAttribute(attribute)

                if (!value) continue

                const translated = tr(value)

                if (translated !== value) {
                    element.setAttribute(attribute, translated)
                }
            }

            element.childNodes.forEach((node) => {
                if (node.nodeType !== Node.TEXT_NODE) return

                const value = node.textContent ?? ""
                const translated = tr(value)

                if (translated !== value) {
                    node.textContent = translated
                }
            })
        }

        const translateTree = (root: ParentNode) => {
            if (root instanceof Element) {
                translateElement(root)
            }

            root.querySelectorAll("*").forEach(translateElement)
        }

        const timer = window.setTimeout(
            () => translateTree(document.body),
            0
        )

        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (
                    mutation.type === "characterData" &&
                    mutation.target.parentElement
                ) {
                    translateElement(mutation.target.parentElement)
                    return
                }

                mutation.addedNodes.forEach((node) => {
                    if (node instanceof HTMLElement) {
                        translateTree(node)
                    } else if (
                        node.nodeType === Node.TEXT_NODE &&
                        node.parentElement
                    ) {
                        translateElement(node.parentElement)
                    }
                })
            })
        })

        observer.observe(document.body, {
            childList: true,
            characterData: true,
            subtree: true
        })

        return () => {
            window.clearTimeout(timer)
            observer.disconnect()
        }
    }, [tr])

    const setLocale = React.useCallback((value: ComandosLocale) => {
        store("comandos-locale", value)
    }, [])

    const setTheme = React.useCallback((value: ComandosTheme) => {
        store("comandos-theme", value)
    }, [])

    const setPalette = React.useCallback((value: ComandosPalette) => {
        store("comandos-palette", value)
    }, [])

    const toggleTheme = React.useCallback(() => {
        store(
            "comandos-theme",
            getSnapshot().theme === "dark" ? "light" : "dark"
        )
    }, [])

    const t = React.useCallback(
        (key: TranslationKey) => uiTranslations[locale][key],
        [locale]
    )

    const value = React.useMemo(
        () => ({
            locale,
            theme,
            palette,
            setLocale,
            setTheme,
            setPalette,
            toggleTheme,
            t,
            tr
        }),
        [
            locale,
            theme,
            palette,
            setLocale,
            setTheme,
            setPalette,
            toggleTheme,
            t,
            tr
        ]
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
        throw new Error(
            "useComandosPreferences must be used inside PreferencesProvider"
        )
    }

    return context
}
