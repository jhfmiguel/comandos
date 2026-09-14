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

type Snapshot = {
    locale: ComandosLocale
    theme: ComandosTheme
    palette: ComandosPalette
}

type PreferencesContextValue = Snapshot & {
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

const ui: Record<ComandosLocale, Record<TranslationKey, string>> = {
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

const pairs: Array<[string, string]> = [
    ["Navigation", "Navegação"],
    ["Dashboard", "Painel"],
    ["Institutional core", "Núcleo institucional"],
    ["Institutional", "Institucional"],
    ["Organizations", "Organizações"],
    ["Organizational units", "Unidades organizacionais"],
    ["People", "Pessoas"],
    ["Person roles", "Papéis de pessoa"],
    ["Person role assignments", "Atribuições de papéis"],
    ["Role details", "Detalhes do papel"],
    ["Credentials", "Credenciais"],
    ["Qualifications", "Qualificações"],
    ["Access", "Acesso"],
    ["System users", "Usuários do sistema"],
    ["Access profiles", "Perfis de acesso"],
    ["Permissions", "Permissões"],
    ["User profiles", "Perfis de usuário"],
    ["Profile permissions", "Permissões de perfil"],
    ["Assets and inventory", "Bens e inventário"],
    ["Reference data", "Dados de referência"],
    ["Catalog", "Catálogo"],
    ["Specifications", "Especificações"],
    ["Controlled equipment", "Equipamentos controlados"],
    ["Compliance", "Conformidade"],
    ["Inventory", "Inventário"],
    ["Stock history", "Histórico de estoque"],
    ["Equipment Movement", "Movimentação de equipamentos"],
    ["Sale", "Venda"],
    ["Custody", "Cautela"],
    ["Ammunition consumption", "Consumo de munição"],
    ["Donation", "Doação"],
    ["Transfer", "Transferência"],
    ["Disposal", "Baixa"],
    ["Reservation", "Reserva"],
    ["Maintenance and inspection", "Manutenção e inspeção"],
    ["Physical inventory", "Inventário físico"],
    ["Audit history", "Histórico de auditoria"],
    ["Settings", "Configurações"],
    ["Notifications", "Notificações"],
    ["Sign out", "Sair"],
    ["Sign in", "Entrar"],
    ["Signing out…", "Saindo…"],
    ["Open user menu", "Abrir menu do usuário"],
    ["Setup mode", "Modo de configuração"],
    ["Guest", "Visitante"],

    ["New record", "Novo registro"],
    ["New user", "Novo usuário"],
    ["New weapon", "Nova arma"],
    ["Register single asset", "Cadastrar bem individual"],
    ["Receive ammunition boxes", "Receber caixas de munição"],
    ["Actions", "Ações"],
    ["Edit", "Editar"],
    ["Delete", "Excluir"],
    ["Save", "Salvar"],
    ["Update", "Atualizar"],
    ["Cancel", "Cancelar"],
    ["Back", "Voltar"],
    ["Close", "Fechar"],
    ["Create", "Criar"],
    ["Add", "Adicionar"],
    ["Search", "Pesquisar"],
    ["Search records", "Pesquisar registros"],
    ["Retry", "Tentar novamente"],
    ["Loading records…", "Carregando registros…"],
    ["No records found.", "Nenhum registro encontrado."],
    [
        "No resources are available for your access profile.",
        "Nenhum recurso está disponível para o seu perfil de acesso."
    ],
    ["Yes", "Sim"],
    ["No", "Não"],
    ["Name", "Nome"],
    ["Code", "Código"],
    ["Description", "Descrição"],
    ["Status", "Situação"],
    ["Type", "Tipo"],
    ["Category", "Categoria"],
    ["Model", "Modelo"],
    ["Brand", "Marca"],
    ["Quantity", "Quantidade"],
    ["Available quantity", "Quantidade disponível"],
    ["Serial number", "Número de série"],
    ["Asset code", "Código patrimonial"],
    ["Creation date", "Data de criação"],
    ["Created at", "Criado em"],
    ["Updated at", "Atualizado em"],
    ["Active", "Ativo"],
    ["Notes", "Observações"],
    ["Reason", "Motivo"],
    ["Date", "Data"],
    ["Origin", "Origem"],
    ["Destination", "Destino"],
    ["Location", "Localização"],
    ["Unit", "Unidade"],
    ["Organization", "Organização"],
    ["Person", "Pessoa"],
    ["User", "Usuário"],
    ["Role", "Papel"],
    ["Profile", "Perfil"],
    ["Permission", "Permissão"],
    ["Price", "Preço"],
    ["Value", "Valor"],
    ["Condition", "Condição"],
    ["Acquisition state", "Estado de aquisição"],
    ["Acquisition origin", "Origem de aquisição"],
    ["Manufacturer", "Fabricante"],
    ["Caliber", "Calibre"],
    ["Lot", "Lote"],
    ["Lots", "Lotes"],

    ["Individual assets", "Bens individuais"],
    ["Stock locations", "Locais de estoque"],
    ["Stock balances", "Saldos de estoque"],
    ["Stock movements", "Movimentações de estoque"],
    ["Equipment sets", "Conjuntos de equipamentos"],
    ["Equipment set components", "Componentes do conjunto"],
    ["Item categories", "Categorias de item"],
    ["Armament types", "Tipos de armamento"],
    ["Armament classifications", "Classificações de armamento"],
    ["Brands", "Marcas"],
    ["Item models", "Modelos de item"],
    ["Technical characteristics", "Características técnicas"],
    ["Category characteristics", "Características da categoria"],
    ["Model characteristics", "Características do modelo"],
    ["Asset characteristics", "Características do bem"],
    ["Firearm specifications", "Especificações de arma de fogo"],
    ["Ammunition specifications", "Especificações de munição"],
    ["Grenade specifications", "Especificações de granada"],
    ["Spray specifications", "Especificações de espargidor"],
    [
        "Ballistic protection specifications",
        "Especificações de proteção balística"
    ],
    [
        "Electrical device specifications",
        "Especificações de dispositivo elétrico"
    ],
    ["Optical specifications", "Especificações ópticas"],
    ["Regulatory controls", "Controles regulatórios"],
    ["Expiration controls", "Controles de validade"],
    ["Certifications", "Certificações"],
    ["Recalls", "Recalls"],
    ["Recall items", "Itens de recall"],

    [
        "Manage organizations, people, their roles, and system account assignments.",
        "Gerencie organizações, pessoas, seus papéis e vínculos com contas do sistema."
    ],

    ["STATUS DO BOT", "BOT STATUS"],
    ["REQUISITO ATUAL", "CURRENT REQUIREMENT"],
    ["REQUISITO EM EXECUÇÃO", "RUNNING REQUIREMENT"],
    ["DETALHE", "DETAIL"],
    ["CONTROLE DO WORKER", "WORKER CONTROL"],
    ["Nenhum requisito em execução", "No requirement running"],
    ["SPRINTS", "SPRINTS"],
    ["Planejamento do ERP", "ERP planning"],
    ["SPRINT ATUAL", "CURRENT SPRINT"],
    ["Buscar requisito", "Search requirement"],
    ["Novo requisito", "New requirement"],
    ["itens no módulo", "items in module"],
    ["em execução", "running"],
    ["concluídos", "completed"],
    [
        "Planejamento local; execução real na fila abaixo",
        "Local planning; actual execution in the queue below"
    ],
    ["Nenhum item nesta etapa.", "No items in this stage."],
    ["Fila do bot", "Bot queue"],
    ["Situação", "Status"],
    ["Tarefa", "Task"],
    ["Arquivo", "File"],
    ["Ações", "Actions"],
    ["Nenhuma tarefa nesta fila.", "No tasks in this queue."],
    ["Trabalhe até o requisito", "Work until requirement"],
    ["Pausar BOT", "Pause BOT"],
    ["Parar BOT", "Stop BOT"],
    ["Iniciar BOT", "Start BOT"],
    ["Continuar BOT", "Resume BOT"],
    ["Pausar", "Pause"],
    ["Parar", "Stop"],
    ["Continuar", "Resume"],

    ["Configurações do ERP", "ERP settings"],
    ["Idioma", "Language"],
    ["Português", "Portuguese"],
    ["Inglês", "English"],
    ["Aparência", "Appearance"],
    ["Claro", "Light"],
    ["Escuro", "Dark"],
    ["Paleta de cores", "Color palette"],
    ["Verde", "Green"],
    ["Azul", "Blue"],
    ["Lilás", "Lilac"],
    ["Vermelho", "Red"],
    ["Amarelo", "Yellow"],
    ["Laranja", "Orange"]
]

const enToPt = new Map(pairs.map(([en, pt]) => [en, pt]))
const ptToEn = new Map(pairs.map(([en, pt]) => [pt, en]))

const defaults: Snapshot = {
    locale: "pt-BR",
    theme: "dark",
    palette: "orange"
}

const listeners = new Set<() => void>()
let cache: Snapshot | null = null
let cacheKey = ""

const readClientSnapshot = (): Snapshot => {
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

const getSnapshot = (): Snapshot => {
    const next = readClientSnapshot()
    const nextKey = JSON.stringify(next)

    if (!cache || cacheKey !== nextKey) {
        cache = next
        cacheKey = nextKey
    }

    return cache
}

const getServerSnapshot = (): Snapshot => defaults

const subscribe = (listener: () => void) => {
    listeners.add(listener)

    const onStorage = () => {
        cache = null
        cacheKey = ""
        listener()
    }

    window.addEventListener("storage", onStorage)

    return () => {
        listeners.delete(listener)
        window.removeEventListener("storage", onStorage)
    }
}

const notify = () => {
    cache = null
    cacheKey = ""
    listeners.forEach((listener) => listener())
}

const write = (key: string, value: string) => {
    window.localStorage.setItem(key, value)
    notify()
}

const translatePattern = (
    text: string,
    locale: ComandosLocale
): string => {
    const map = locale === "pt-BR" ? enToPt : ptToEn

    const exact = map.get(text)

    if (exact) {
        return exact
    }

    if (locale === "pt-BR") {
        const search = text.match(/^Search (.+)\.\.\.$/i)
        if (search) {
            const field = enToPt.get(search[1]) ?? search[1]
            return `Pesquisar ${field.toLowerCase()}...`
        }

        const edit = text.match(/^Edit (.+)$/)
        if (edit) return `Editar ${edit[1]}`

        const remove = text.match(/^Delete (.+)$/)
        if (remove) return `Excluir ${remove[1]}`
    } else {
        const search = text.match(/^Pesquisar (.+)\.\.\.$/i)
        if (search) {
            const field = ptToEn.get(search[1]) ?? search[1]
            return `Search ${field.toLowerCase()}...`
        }

        const edit = text.match(/^Editar (.+)$/)
        if (edit) return `Edit ${edit[1]}`

        const remove = text.match(/^Excluir (.+)$/)
        if (remove) return `Delete ${remove[1]}`
    }

    return text
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
        (text: string) => translatePattern(text, locale),
        [locale]
    )

    React.useEffect(() => {
        document.documentElement.lang = locale
        document.documentElement.dataset.theme = theme
        document.documentElement.dataset.palette = palette
    }, [locale, theme, palette])

    React.useEffect(() => {
        const translateElement = (element: Element) => {
            for (const attribute of [
                "title",
                "aria-label",
                "placeholder"
            ]) {
                const value = element.getAttribute(attribute)

                if (!value) continue

                const translated = tr(value)

                if (translated !== value) {
                    element.setAttribute(attribute, translated)
                }
            }

            element.childNodes.forEach((node) => {
                if (node.nodeType !== Node.TEXT_NODE) {
                    return
                }

                const value = node.textContent ?? ""
                const trimmed = value.trim()

                if (!trimmed) {
                    return
                }

                const translated = tr(trimmed)

                if (translated === trimmed) {
                    return
                }

                node.textContent = value.replace(trimmed, translated)
            })
        }

        const translateTree = (root: ParentNode) => {
            if (root instanceof Element) {
                translateElement(root)
            }

            root.querySelectorAll(
                [
                    "button",
                    "label",
                    "th",
                    "h1",
                    "h2",
                    "h3",
                    "h4",
                    "p",
                    "span",
                    "option",
                    "a",
                    "[role='menuitem']",
                    "[role='tab']"
                ].join(",")
            ).forEach(translateElement)
        }

        const timer = window.setTimeout(
            () => translateTree(document.body),
            0
        )

        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node instanceof HTMLElement) {
                        translateTree(node)
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
        write("comandos-locale", value)
    }, [])

    const setTheme = React.useCallback((value: ComandosTheme) => {
        write("comandos-theme", value)
    }, [])

    const setPalette = React.useCallback((value: ComandosPalette) => {
        write("comandos-palette", value)
    }, [])

    const toggleTheme = React.useCallback(() => {
        write(
            "comandos-theme",
            getSnapshot().theme === "dark" ? "light" : "dark"
        )
    }, [])

    const t = React.useCallback(
        (key: TranslationKey) => ui[locale][key],
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
