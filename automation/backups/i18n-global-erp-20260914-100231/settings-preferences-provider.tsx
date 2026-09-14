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
    tr: (text: string) => string
}

export type TranslationKey =
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
    | "bot"
    | "commandCenter"
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

const uiTranslations: Record<
    ComandosLocale,
    Record<TranslationKey, string>
> = {
    "pt-BR": {
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
        bot: "Bot",
        commandCenter: "Command Center",
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
        bot: "Bot",
        commandCenter: "Command Center",
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

/*
 * Dicionário de compatibilidade do ERP legado.
 * Inglês é a chave canônica; pt-BR é a tradução exibida.
 * Novos componentes devem preferir t()/tr() diretamente.
 */
const pt: Record<string, string> = {
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
    "Catalog": "Catálogo",
    "Specifications": "Especificações",
    "Controlled equipment": "Equipamentos controlados",
    "Compliance": "Conformidade",
    "Inventory": "Inventário",
    "Stock history": "Histórico de estoque",
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
    "Guest": "Visitante",

    "New record": "Novo registro",
    "Register single asset": "Cadastrar bem individual",
    "Receive ammunition boxes": "Receber caixas de munição",
    "Actions": "Ações",
    "Edit": "Editar",
    "Delete": "Excluir",
    "Save": "Salvar",
    "Cancel": "Cancelar",
    "Back": "Voltar",
    "Close": "Fechar",
    "Create": "Criar",
    "Update": "Atualizar",
    "Search": "Pesquisar",
    "Search records": "Pesquisar registros",
    "Retry": "Tentar novamente",
    "Loading records…": "Carregando registros…",
    "No records found.": "Nenhum registro encontrado.",
    "No resources are available for your access profile.":
        "Nenhum recurso está disponível para o seu perfil de acesso.",
    "Yes": "Sim",
    "No": "Não",
    "Name": "Nome",
    "Code": "Código",
    "Description": "Descrição",
    "Status": "Situação",
    "Type": "Tipo",
    "Category": "Categoria",
    "Model": "Modelo",
    "Brand": "Marca",
    "Quantity": "Quantidade",
    "Available quantity": "Quantidade disponível",
    "Serial number": "Número de série",
    "Asset code": "Código patrimonial",
    "Patrimonial identification": "Identificação patrimonial",
    "Creation date": "Data de criação",
    "Created at": "Criado em",
    "Updated at": "Atualizado em",
    "Active": "Ativo",
    "Notes": "Observações",
    "Reason": "Motivo",
    "Date": "Data",
    "Origin": "Origem",
    "Destination": "Destino",
    "Location": "Localização",
    "Unit": "Unidade",
    "Organization": "Organização",
    "Person": "Pessoa",
    "User": "Usuário",
    "Role": "Papel",
    "Profile": "Perfil",
    "Permission": "Permissão",
    "Price": "Preço",
    "Value": "Valor",
    "Condition": "Condição",
    "Acquisition state": "Estado de aquisição",
    "Acquisition origin": "Origem de aquisição",
    "Manufacturer": "Fabricante",
    "Caliber": "Calibre",
    "Lot": "Lote",
    "Lots": "Lotes",
    "Individual assets": "Bens individuais",
    "Stock locations": "Locais de estoque",
    "Stock balances": "Saldos de estoque",
    "Stock movements": "Movimentações de estoque",
    "Equipment sets": "Conjuntos de equipamentos",
    "Equipment set components": "Componentes do conjunto",
    "Item categories": "Categorias de item",
    "Armament types": "Tipos de armamento",
    "Armament classifications": "Classificações de armamento",
    "Brands": "Marcas",
    "Item models": "Modelos de item",
    "Technical characteristics": "Características técnicas",
    "Category characteristics": "Características da categoria",
    "Model characteristics": "Características do modelo",
    "Asset characteristics": "Características do bem",
    "Firearm specifications": "Especificações de arma de fogo",
    "Ammunition specifications": "Especificações de munição",
    "Grenade specifications": "Especificações de granada",
    "Spray specifications": "Especificações de espargidor",
    "Ballistic protection specifications":
        "Especificações de proteção balística",
    "Electrical device specifications":
        "Especificações de dispositivo elétrico",
    "Optical specifications": "Especificações ópticas",
    "Regulatory controls": "Controles regulatórios",
    "Expiration controls": "Controles de validade",
    "Certifications": "Certificações",
    "Recalls": "Recalls",
    "Recall items": "Itens de recall",

    "Manage organizations, people, their roles, and system account assignments.":
        "Gerencie organizações, pessoas, seus papéis e vínculos com contas do sistema.",
    "Manage controlled equipment, catalog, inventory, stock, and traceability.":
        "Gerencie equipamentos controlados, catálogo, inventário, estoque e rastreabilidade.",

    "Command Center": "Command Center",
    "STATUS DO BOT": "STATUS DO BOT",
    "REQUISITO ATUAL": "REQUISITO ATUAL",
    "REQUISITO EM EXECUÇÃO": "REQUISITO EM EXECUÇÃO",
    "DETALHE": "DETALHE",
    "CONTROLE DO WORKER": "CONTROLE DO WORKER",
    "Nenhum requisito em execução": "Nenhum requisito em execução",
    "SPRINTS": "SPRINTS",
    "Planejamento do ERP": "Planejamento do ERP",
    "SPRINT ATUAL": "SPRINT ATUAL",
    "Buscar requisito": "Buscar requisito",
    "Novo requisito": "Novo requisito",
    "itens no módulo": "itens no módulo",
    "em execução": "em execução",
    "concluídos": "concluídos",
    "Planejamento local; execução real na fila abaixo":
        "Planejamento local; execução real na fila abaixo",
    "Nenhum item nesta etapa.": "Nenhum item nesta etapa.",
    "Fila do bot": "Fila do bot",
    "Situação": "Situação",
    "Tarefa": "Tarefa",
    "Arquivo": "Arquivo",
    "Ações": "Ações"
}

const enFromPt: Record<string, string> = Object.fromEntries(
    Object.entries(pt).map(([english, portuguese]) => [portuguese, english])
)

const defaults: PreferenceSnapshot = {
    locale: "pt-BR",
    theme: "dark",
    palette: "orange"
}

const listeners = new Set<() => void>()
let cached: PreferenceSnapshot | null = null
let cachedKey = ""

const readClientSnapshot = (): PreferenceSnapshot => {
    if (typeof window === "undefined") {
        return defaults
    }

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

    if (!cached || cachedKey !== key) {
        cached = next
        cachedKey = key
    }

    return cached
}

const getServerSnapshot = () => defaults

const subscribe = (listener: () => void) => {
    listeners.add(listener)

    const storage = () => {
        cached = null
        cachedKey = ""
        listener()
    }

    window.addEventListener("storage", storage)

    return () => {
        listeners.delete(listener)
        window.removeEventListener("storage", storage)
    }
}

const notify = () => {
    cached = null
    cachedKey = ""
    listeners.forEach((listener) => listener())
}

const setStored = (key: string, value: string) => {
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

    const tr = React.useCallback(
        (text: string) => {
            if (!text) return text

            if (locale === "pt-BR") {
                return pt[text] ?? text
            }

            return enFromPt[text] ?? text
        },
        [locale]
    )

    React.useEffect(() => {
        document.documentElement.lang = locale
        document.documentElement.dataset.theme = theme
        document.documentElement.dataset.palette = palette
    }, [locale, theme, palette])

    /*
     * Compatibilidade para componentes antigos que ainda possuem
     * labels literais. Executa somente após a hidratação.
     * Não altera inputs/td de dados de usuário.
     */
    React.useEffect(() => {
        const translateNode = (root: ParentNode) => {
            const selectors = [
                "button",
                "label",
                "th",
                "h1",
                "h2",
                "h3",
                "h4",
                "nav",
                "option",
                "[role='tab']",
                "[role='menuitem']",
                ".comandos-sidebar-label",
                ".eyebrow",
                ".column-empty",
                ".metric-note"
            ]

            root.querySelectorAll<HTMLElement>(
                selectors.join(",")
            ).forEach((element) => {
                if (
                    element.children.length === 0 &&
                    element.textContent
                ) {
                    const current = element.textContent.trim()
                    const translated = tr(current)

                    if (translated !== current) {
                        element.textContent = translated
                    }
                }

                for (const attribute of [
                    "title",
                    "aria-label",
                    "placeholder"
                ]) {
                    const current = element.getAttribute(attribute)

                    if (!current) continue

                    const translated = tr(current)

                    if (translated !== current) {
                        element.setAttribute(attribute, translated)
                    }
                }
            })
        }

        const run = () => translateNode(document)

        const timer = window.setTimeout(run, 0)

        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node instanceof HTMLElement) {
                        translateNode(node)
                    }
                })
            })
        })

        observer.observe(document.body, {
            childList: true,
            subtree: true
        })

        return () => {
            window.clearTimeout(timer)
            observer.disconnect()
        }
    }, [tr])

    const setLocale = React.useCallback((value: ComandosLocale) => {
        setStored("comandos-locale", value)
    }, [])

    const setTheme = React.useCallback((value: ComandosTheme) => {
        setStored("comandos-theme", value)
    }, [])

    const setPalette = React.useCallback((value: ComandosPalette) => {
        setStored("comandos-palette", value)
    }, [])

    const toggleTheme = React.useCallback(() => {
        setStored(
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
