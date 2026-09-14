"use client"

import * as React from "react"

export type ComandosLocale = "pt-BR" | "en-US"
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

type PreferencesContextValue = PreferenceSnapshot & {
    setLocale: (locale: ComandosLocale) => void
    setTheme: (theme: ComandosTheme) => void
    setPalette: (palette: ComandosPalette) => void
    toggleTheme: () => void
    t: (key: TranslationKey) => string
    tr: (label: string) => string
}

export type TranslationKey =
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

const translations: Record<
    ComandosLocale,
    Record<TranslationKey, string>
> = {
    "pt-BR": {
        institutionalCore: "Núcleo institucional",
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

const literalTranslations: Record<string, string> = {
    "Navigation": "Navegação",
    "Dashboard": "Painel",
    "Institutional core": "Núcleo institucional",
    "Institutional": "Institucional",
    "Organizations": "Organizações",
    "Organizational units": "Unidades organizacionais",
    "People": "Pessoas",
    "Person roles": "Papéis de pessoa",
    "Person role assignments": "Atribuições de papéis",
    "Role details": "Detalhes do papel",
    "Credentials": "Credenciais",
    "Qualifications": "Qualificações",
    "Access": "Acesso",
    "System users": "Usuários do sistema",
    "Access profiles": "Perfis de acesso",
    "Permissions": "Permissões",
    "User profiles": "Perfis de usuário",
    "Profile permissions": "Permissões de perfil",
    "Assets and inventory": "Bens e inventário",
    "Reference data": "Dados de referência",
    "Custody return condition types": "Tipos de condição de devolução de cautela",
    "Sale return reason types": "Tipos de motivo de devolução de venda",
    "Inventory count status types": "Tipos de situação de contagem de inventário",
    "Inventory count result types": "Tipos de resultado de contagem de inventário",
    "Reservation status types": "Tipos de situação de reserva",
    "Catalog": "Catálogo",
    "Item categories": "Categorias de item",
    "Armament types": "Tipos de armamento",
    "Armament classifications": "Classificações de armamento",
    "Brands": "Marcas",
    "Item models": "Modelos de item",
    "Specifications": "Especificações",
    "Technical characteristics": "Características técnicas",
    "Category characteristics": "Características da categoria",
    "Model characteristics": "Características do modelo",
    "Asset characteristics": "Características do bem",
    "Controlled equipment": "Equipamentos controlados",
    "Firearm specifications": "Especificações de arma de fogo",
    "Ammunition specifications": "Especificações de munição",
    "Grenade specifications": "Especificações de granada",
    "Spray specifications": "Especificações de espargidor",
    "Ballistic protection specifications": "Especificações de proteção balística",
    "Electrical device specifications": "Especificações de dispositivo elétrico",
    "Optical specifications": "Especificações ópticas",
    "Regulatory controls": "Controles regulatórios",
    "Compliance": "Conformidade",
    "Expiration controls": "Controles de validade",
    "Certifications": "Certificações",
    "Recalls": "Recalls",
    "Recall items": "Itens de recall",
    "Inventory": "Inventário",
    "Stock locations": "Locais de estoque",
    "Equipment sets": "Conjuntos de equipamentos",
    "Equipment set components": "Componentes do conjunto",
    "Individual assets": "Bens individuais",
    "Stock lots": "Lotes de estoque",
    "Stock history": "Histórico de estoque",
    "Stock balances": "Saldos de estoque",
    "Stock movements": "Movimentações de estoque",
    "Equipment Movement": "Movimentação de equipamentos",
    "Sale": "Venda",
    "Custody": "Cautela",
    "Ammunition consumption": "Consumo de munição",
    "Donation": "Doação",
    "Transfer": "Transferência",
    "Disposal": "Baixa",
    "Reservation": "Reserva",
    "Maintenance and inspection": "Manutenção e inspeção",
    "Physical inventory": "Inventário físico",
    "Audit history": "Histórico de auditoria",
    "Settings": "Configurações",
    "Notifications": "Notificações",
    "Sign out": "Sair",
    "Sign in": "Entrar",
    "Signing out…": "Saindo…",
    "Open user menu": "Abrir menu do usuário",
    "Setup mode": "Modo de configuração",
    "Guest": "Visitante"
}

const defaults: PreferenceSnapshot = {
    locale: "pt-BR",
    theme: "dark",
    palette: "orange"
}

const listeners = new Set<() => void>()

const readClientSnapshot = (): PreferenceSnapshot => {
    if (typeof window === "undefined") {
        return defaults
    }

    const storedLocale = window.localStorage.getItem("comandos-locale")
    const storedTheme = window.localStorage.getItem("comandos-theme")
    const storedPalette = window.localStorage.getItem("comandos-palette")

    return {
        locale: storedLocale === "en-US" ? "en-US" : "pt-BR",
        theme:
            storedTheme === "light" || storedTheme === "dark"
                ? storedTheme
                : defaults.theme,
        palette:
            storedPalette === "green" ||
            storedPalette === "blue" ||
            storedPalette === "lilac" ||
            storedPalette === "red" ||
            storedPalette === "yellow" ||
            storedPalette === "orange"
                ? storedPalette
                : defaults.palette
    }
}

let cachedSnapshot: PreferenceSnapshot | null = null
let cachedSerialized = ""

const getSnapshot = (): PreferenceSnapshot => {
    const next = readClientSnapshot()
    const serialized = JSON.stringify(next)

    if (!cachedSnapshot || cachedSerialized !== serialized) {
        cachedSnapshot = next
        cachedSerialized = serialized
    }

    return cachedSnapshot
}

const getServerSnapshot = (): PreferenceSnapshot => defaults

const subscribe = (listener: () => void) => {
    listeners.add(listener)

    const onStorage = (event: StorageEvent) => {
        if (
            event.key === "comandos-locale" ||
            event.key === "comandos-theme" ||
            event.key === "comandos-palette"
        ) {
            cachedSnapshot = null
            cachedSerialized = ""
            listener()
        }
    }

    window.addEventListener("storage", onStorage)

    return () => {
        listeners.delete(listener)
        window.removeEventListener("storage", onStorage)
    }
}

const notify = () => {
    cachedSnapshot = null
    cachedSerialized = ""
    listeners.forEach((listener) => listener())
}

const writePreference = (
    key: "comandos-locale" | "comandos-theme" | "comandos-palette",
    value: string
) => {
    window.localStorage.setItem(key, value)
    notify()
}

const PreferencesContext =
    React.createContext<PreferencesContextValue | null>(null)

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

    React.useEffect(() => {
        document.documentElement.lang = locale
        document.documentElement.dataset.theme = theme
        document.documentElement.dataset.palette = palette
    }, [locale, theme, palette])

    const setLocale = React.useCallback((value: ComandosLocale) => {
        writePreference("comandos-locale", value)
    }, [])

    const setTheme = React.useCallback((value: ComandosTheme) => {
        writePreference("comandos-theme", value)
    }, [])

    const setPalette = React.useCallback((value: ComandosPalette) => {
        writePreference("comandos-palette", value)
    }, [])

    const toggleTheme = React.useCallback(() => {
        writePreference(
            "comandos-theme",
            getSnapshot().theme === "dark" ? "light" : "dark"
        )
    }, [])

    const t = React.useCallback(
        (key: TranslationKey) => translations[locale][key],
        [locale]
    )

    const tr = React.useCallback(
        (label: string) => {
            if (locale === "en-US") {
                return label
            }

            return literalTranslations[label] ?? label
        },
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
