"use client"

import * as React from "react"
import {
    PreferencesProvider as SharedPreferencesProvider,
    platformPreferencesStorageKey,
    usePlatformPreferences
} from "@faria-miguel/platform/preferences"
import { ThemeProvider as PlatformThemeProvider } from "@faria-miguel/platform/theme"

import {
    type ComandosLocale,
    translateText
} from "./i18n-catalog"
import {
    productDefinition,
    productStorageKey
} from "platform/product"

export type { ComandosLocale } from "./i18n-catalog"

export type ComandosTheme = "dark" | "light" | "mixed"
export type ComandosPalette =
    | "green"
    | "blue"
    | "lilac"
    | "red"
    | "yellow"
    | "orange"
    | "black"
    | "gray"
    | "white"
    | "pink"
    | "custom"

type PreferenceSnapshot = {
    locale: ComandosLocale
    theme: ComandosTheme
    palette: ComandosPalette
    customColor: string
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
    | "black"
    | "gray"
    | "white"
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
    | "pink"
    | "custom"

type PreferencesContextValue = PreferenceSnapshot & {
    setLocale: (locale: ComandosLocale) => void
    setTheme: (theme: ComandosTheme) => void
    setPalette: (palette: ComandosPalette) => void
    setCustomColor: (color: string) => void
    toggleTheme: () => void
    t: (key: TranslationKey) => string
    tr: (text: string) => string
}

const uiTranslations: Record<ComandosLocale, Record<TranslationKey, string>> = {
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
        pink: "Rosa",
        custom: "Personalizada",
        black: "Preta",
        gray: "Cinza",
        white: "Branco",
        savedAutomatically: "As preferências são salvas automaticamente neste navegador.",
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
        pink: "Pink",
        custom: "Custom",
        black: "Black",
        gray: "Gray",
        white: "White",
        savedAutomatically: "Preferences are saved automatically in this browser.",
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

const normalizeHexColor = (value?: string | null): string => {
    const normalized = (value ?? "#ff9900").trim().toLowerCase()
    return /^#[0-9a-f]{6}$/.test(normalized) ? normalized : "#ff9900"
}

const hexToRgb = (hex: string): [number, number, number] => {
    const value = normalizeHexColor(hex).slice(1)
    return [
        Number.parseInt(value.slice(0, 2), 16),
        Number.parseInt(value.slice(2, 4), 16),
        Number.parseInt(value.slice(4, 6), 16)
    ]
}

const mixWithBlack = (hex: string, amount: number): string => {
    const [red, green, blue] = hexToRgb(hex)
    const factor = Math.max(0, Math.min(1, 1 - amount))
    const toHex = (value: number) =>
        Math.round(value * factor).toString(16).padStart(2, "0")

    return `#${toHex(red)}${toHex(green)}${toHex(blue)}`
}

const contrastFor = (hex: string): string => {
    const [red, green, blue] = hexToRgb(hex)
    const luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
    return luminance > 0.62 ? "#1f1f1f" : "#ffffff"
}

const applyCustomPaletteVariables = (hex: string) => {
    const root = document.documentElement
    const normalized = normalizeHexColor(hex)
    const [red, green, blue] = hexToRgb(normalized)

    root.style.setProperty("--comandos-custom-accent", normalized)
    root.style.setProperty("--comandos-custom-accent-hover", mixWithBlack(normalized, 0.10))
    root.style.setProperty("--comandos-custom-accent-strong", mixWithBlack(normalized, 0.20))
    root.style.setProperty("--comandos-custom-accent-rgb", `${red}, ${green}, ${blue}`)
    root.style.setProperty("--comandos-custom-accent-contrast", contrastFor(normalized))
}

const defaults: PreferenceSnapshot = {
    locale: productDefinition.defaultLocale,
    theme: productDefinition.defaultTheme,
    palette: "custom",
    customColor: productDefinition.defaultAccent
}

const PreferencesContext = React.createContext<PreferencesContextValue | null>(null)

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

function normalizeLocale(value: unknown): ComandosLocale {
    return value === "en-US" ? "en-US" : "pt-BR"
}

function normalizeTheme(value: unknown): ComandosTheme {
    return value === "light" || value === "dark" || value === "mixed"
        ? value
        : defaults.theme
}

function normalizePalette(value: unknown): ComandosPalette {
    const allowed: ComandosPalette[] = [
        "green", "blue", "lilac", "red", "yellow", "orange",
        "black", "gray", "white", "pink", "custom"
    ]

    return allowed.includes(value as ComandosPalette)
        ? value as ComandosPalette
        : defaults.palette
}

function ComandosPreferencesBridge({ children }: { children: React.ReactNode }) {
    const {
        preferences,
        setPreference,
        patchPreferences
    } = usePlatformPreferences()

    const migratedRef = React.useRef(false)

    React.useEffect(() => {
        if (migratedRef.current) return
        migratedRef.current = true

        const sharedKey = platformPreferencesStorageKey(
            productDefinition.storageNamespace
        )

        if (window.localStorage.getItem(sharedKey)) return

        const legacyLocale = window.localStorage.getItem(productStorageKey("locale"))
        const legacyTheme = window.localStorage.getItem(productStorageKey("theme"))
        const legacyPalette = window.localStorage.getItem(productStorageKey("palette"))
        const legacyColor = window.localStorage.getItem(productStorageKey("custom-color"))

        patchPreferences({
            locale: normalizeLocale(legacyLocale ?? defaults.locale),
            theme: normalizeTheme(legacyTheme ?? defaults.theme),
            palette: normalizePalette(legacyPalette ?? defaults.palette),
            accent: normalizeHexColor(legacyColor ?? defaults.customColor)
        })
    }, [patchPreferences])

    const locale = normalizeLocale(preferences.locale)
    const theme = normalizeTheme(preferences.theme)
    const palette = normalizePalette(preferences.palette)
    const customColor = normalizeHexColor(
        typeof preferences.accent === "string"
            ? preferences.accent
            : defaults.customColor
    )

    const tr = React.useCallback(
        (text: string) => translateText(text, locale),
        [locale]
    )

    React.useEffect(() => {
        document.documentElement.lang = locale
        document.documentElement.dataset.palette = palette
        applyCustomPaletteVariables(customColor)
    }, [locale, palette, customColor])

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
            if (root instanceof Element) translateElement(root)
            root.querySelectorAll("*").forEach(translateElement)
        }

        const timer = window.setTimeout(() => translateTree(document.body), 0)

        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (
                    mutation.type === "characterData"
                    && mutation.target.parentElement
                ) {
                    translateElement(mutation.target.parentElement)
                    return
                }

                mutation.addedNodes.forEach((node) => {
                    if (node instanceof HTMLElement) {
                        translateTree(node)
                    } else if (
                        node.nodeType === Node.TEXT_NODE
                        && node.parentElement
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

    const setLocale = React.useCallback(
        (value: ComandosLocale) => setPreference("locale", value),
        [setPreference]
    )

    const setTheme = React.useCallback(
        (value: ComandosTheme) => setPreference("theme", value),
        [setPreference]
    )

    const setPalette = React.useCallback(
        (value: ComandosPalette) => setPreference("palette", value),
        [setPreference]
    )

    const setCustomColor = React.useCallback((value: string) => {
        patchPreferences({
            accent: normalizeHexColor(value),
            palette: "custom"
        })
    }, [patchPreferences])

    const toggleTheme = React.useCallback(() => {
        setPreference("theme", theme === "dark" ? "light" : "dark")
    }, [setPreference, theme])

    const t = React.useCallback(
        (key: TranslationKey) => {
            if (key === "settingsTitle") {
                return locale === "pt-BR"
                    ? `Configurações do ${productDefinition.shortName}`
                    : `${productDefinition.shortName} settings`
            }

            if (key === "quickNavigation") {
                return locale === "pt-BR"
                    ? `Navegação rápida do ${productDefinition.shortName}`
                    : `${productDefinition.shortName} quick navigation`
            }

            return uiTranslations[locale][key]
        },
        [locale]
    )

    const value = React.useMemo(
        () => ({
            locale,
            theme,
            palette,
            customColor,
            setLocale,
            setTheme,
            setPalette,
            setCustomColor,
            toggleTheme,
            t,
            tr
        }),
        [
            locale,
            theme,
            palette,
            customColor,
            setLocale,
            setTheme,
            setPalette,
            setCustomColor,
            toggleTheme,
            t,
            tr
        ]
    )

    return (
        <PreferencesContext.Provider value={value}>
            <PlatformThemeProvider theme={theme}>
                {children}
            </PlatformThemeProvider>
        </PreferencesContext.Provider>
    )
}

export function PreferencesProvider({
    children
}: {
    children: React.ReactNode
}) {
    return (
        <SharedPreferencesProvider
            namespace={productDefinition.storageNamespace}
            defaults={{
                locale: defaults.locale,
                theme: defaults.theme,
                palette: defaults.palette,
                accent: defaults.customColor
            }}
        >
            <ComandosPreferencesBridge>
                {children}
            </ComandosPreferencesBridge>
        </SharedPreferencesProvider>
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
