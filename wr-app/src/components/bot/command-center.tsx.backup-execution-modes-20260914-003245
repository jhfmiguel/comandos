"use client"

import { useEffect, useMemo, useState } from "react"
import { Plus, Search, Undo2, X } from "lucide-react"


import { Paginator } from "@primereact/ui/paginator"
import type {
    PaginatorPagesInstance,
    PaginatorRootChangeEvent
} from "@primereact/ui/paginator"

import { AngleDoubleLeft } from "@primeicons/react/angle-double-left"
import { AngleDoubleRight } from "@primeicons/react/angle-double-right"
import { AngleLeft } from "@primeicons/react/angle-left"
import { AngleRight } from "@primeicons/react/angle-right"
import { EllipsisH } from "@primeicons/react/ellipsis-h"
import { Layout } from "components"
import { ArrowUTurnUpLeft, Pencil, Send, Trash } from "@primeicons/react"

type ModuleKey = string
type RequirementType = "Requisito" | "Correção"
type RequirementStatus = "Backlog" | "Fazendo" | "Concluído"

type Sprint = {
    key: ModuleKey
    name: string
    detail: string
    accent: string
    steps: number
}

type Requirement = {
    id: string
    title: string
    description: string
    module: ModuleKey
    type: RequirementType
    status: RequirementStatus
    priority: "Alta" | "Média" | "Baixa"
    acceptance: string
    createdAt: string
}

type QueuedTask = {
    name: string
    title: string
    workstream: string
    state: string
}

type BotStatus = {
    state: string
    message: string
    taskName: string
    workstream?: string
    processedTasks: number
    updatedAt: string | null
    rateLimitResetAt?: string | null
    alive?: boolean
    tasks?: QueuedTask[]
}

const defaultSprints: Sprint[] = [
    {
        key: "armamento",
        name: "Armamento",
        detail: "Cadastro, ciclo de vida e movimentações",
        accent: "#e85d04",
        steps: 4
    },
    {
        key: "municao",
        name: "Munição",
        detail: "Consumo, lotes e rastreabilidade",
        accent: "#f48c06",
        steps: 3
    },
    {
        key: "transporte",
        name: "Transporte",
        detail: "Transferências, custódia e logística",
        accent: "#2a9d8f",
        steps: 4
    },
    {
        key: "inventario",
        name: "Inventário",
        detail: "Estoque, contagem e reconciliação",
        accent: "#457b9d",
        steps: 5
    },
    {
        key: "custodia",
        name: "Custódia",
        detail: "Responsáveis, unidades e termos",
        accent: "#6d597a",
        steps: 3
    },
    {
        key: "auditoria",
        name: "Auditoria",
        detail: "Histórico, conformidade e evidências",
        accent: "#bc6c25",
        steps: 3
    }
]

const statusColumns: RequirementStatus[] = ["Backlog", "Fazendo", "Concluído"]

const stateLabels: Record<string, string> = {
    offline: "offline",
    waiting: "aguardando",
    working: "trabalhando",
    validating: "validando",
    paused: "pausado",
    "pause-requested": "pausa solicitada",
    completed: "concluído",
    stopped: "parado",
    failed: "falhou",
    unresponsive: "sem resposta",
    starting: "iniciando"
}

const queueLabels: Record<string, string> = {
    pending: "Pendente",
    working: "Em execução",
    completed: "Concluída",
    failed: "Falhou"
}

const slugify = (value: string) => value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 40)

const getPrefix = (sprint: Sprint) => sprint.key.slice(0, 3).toUpperCase()

const formatRateLimitReset = (botStatus: BotStatus) => {
    const rawReset = botStatus.rateLimitResetAt

    if (rawReset) {
        const parsedReset = new Date(rawReset)

        if (!Number.isNaN(parsedReset.getTime())) {
            return parsedReset.toLocaleTimeString("pt-BR", {
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit"
            })
        }

        return rawReset
    }

    const message = botStatus.message || ""

    const resetMatch = message.match(
        /(?:rate limit\s+resets?\s+(?:on|at)|resets?\s+(?:on|at)|try again at|liberad[oa]\s+(?:Ã s|as)|dispon[iÃ­]vel\s+(?:Ã s|as))\s+([^.;\r\n]+)/i
    )

    if (!resetMatch?.[1]) {
        return null
    }

    const messageReset = resetMatch[1].trim()
    const parsedMessageReset = new Date(messageReset)

    if (!Number.isNaN(parsedMessageReset.getTime())) {
        return parsedMessageReset.toLocaleTimeString("pt-BR", {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
        })
    }

    return messageReset
}
export function CommandCenter() {
    const [sprints, setSprints] = useState<Sprint[]>(defaultSprints)
    const [requirements, setRequirements] = useState<Requirement[]>([])
    const [loaded, setLoaded] = useState(false)
    const [busy, setBusy] = useState(false)
    const [queuePage, setQueuePage] = useState(1)
    const [selectedModule, setSelectedModule] = useState<ModuleKey>("armamento")
    const [search, setSearch] = useState("")
    const [showRequirementForm, setShowRequirementForm] = useState(false)
    const [editingRequirementId, setEditingRequirementId] = useState<string | null>(null)
    const [showSprintForm, setShowSprintForm] = useState(false)
    const [editingSprintKey, setEditingSprintKey] = useState<string | null>(null)
    const [notice, setNotice] = useState("")
    const [botStatus, setBotStatus] = useState<BotStatus>({
        state: "offline",
        message: "Worker ainda não iniciou.",
        taskName: "",
        processedTasks: 0,
        updatedAt: null
    })

    const [form, setForm] = useState({
        title: "",
        description: "",
        type: "Requisito" as RequirementType,
        priority: "Média" as Requirement["priority"],
        acceptance: ""
    })

    const [sprintForm, setSprintForm] = useState({
        name: "",
        detail: "",
        accent: "#e85d04",
        steps: 1
    })

    const selectedSprint = useMemo(
        () => sprints.find((sprint) => sprint.key === selectedModule) ?? sprints[0],
        [sprints, selectedModule]
    )

    useEffect(() => {
        Promise.resolve().then(() => {
            try {
                const storedSprints = JSON.parse(
                    window.localStorage.getItem("comandos-sprints") || "null"
                )

                if (Array.isArray(storedSprints) && storedSprints.length > 0) {
                    setSprints(storedSprints)
                    setSelectedModule(storedSprints[0].key)
                }
            } catch {
                setNotice("Não foi possível carregar as sprints salvas.")
            }

            try {
                const storedRequirements = JSON.parse(
                    window.localStorage.getItem("comandos-requirements") || "[]"
                )

                if (Array.isArray(storedRequirements)) {
                    setRequirements(storedRequirements)
                }
            } catch {
                setNotice("Não foi possível carregar o backlog salvo no navegador.")
            }

            setLoaded(true)
        })
    }, [])

    useEffect(() => {
        if (!loaded) return

        try {
            window.localStorage.setItem(
                "comandos-sprints",
                JSON.stringify(sprints)
            )
        } catch (error) {
            console.error(
                "Não foi possível salvar as sprints no navegador.",
                error
            )
        }
    }, [sprints, loaded])

    useEffect(() => {
        if (!loaded) return

        try {
            window.localStorage.setItem(
                "comandos-requirements",
                JSON.stringify(requirements)
            )
        } catch (error) {
            console.error(
                "Não foi possível salvar o backlog no navegador.",
                error
            )
        }
    }, [requirements, loaded])

    const refreshStatus = async () => {
        try {
            const response = await fetch("/api/bot/status", { cache: "no-store" })

            if (!response.ok) {
                throw new Error()
            }

            const nextBotStatus = await response.json() as BotStatus
                setBotStatus(nextBotStatus)

                setRequirements((current) =>
                    current.map((requirement) => {
                        const requirementNumber = requirement.id.match(/(\d{3})$/)?.[1]

                        if (!requirementNumber) {
                            return requirement
                        }

                        const task = (nextBotStatus.tasks || []).find((candidate) =>
                            candidate.workstream === requirement.module &&
                            (
                                candidate.name.includes(`-${requirementNumber}-`) ||
                                candidate.title.includes(` ${requirementNumber} -`)
                            )
                        )

                        if (!task) {
                            return requirement
                        }

                        if (
                            ["working", "validating"].includes(task.state) &&
                            requirement.status !== "Fazendo"
                        ) {
                            return {
                                ...requirement,
                                status: "Fazendo" as RequirementStatus
                            }
                        }

                        if (
                            task.state === "completed" &&
                            requirement.status !== "Concluído"
                        ) {
                            return {
                                ...requirement,
                                status: "Concluído" as RequirementStatus
                            }
                        }

                        return requirement
                    })
                )
        } catch {
            setBotStatus((current) => ({
                ...current,
                state: "offline",
                message: "Não foi possível consultar o worker."
            }))
        }
    }

    useEffect(() => {
        const initialRefresh = window.setTimeout(() => {
            void refreshStatus()
        }, 0)

        const timer = window.setInterval(() => {
            void refreshStatus()
        }, 2000)

        botStatus.state === "working" ||
        botStatus.state === "validating"
    return () => {
            window.clearTimeout(initialRefresh)
            window.clearInterval(timer)
        }
    }, [])

    const filteredRequirements = useMemo(
        () => requirements.filter((requirement) => {
            const matchesModule = requirement.module === selectedModule
            const query = search.toLowerCase()

            return matchesModule && (
                !query ||
                `${requirement.id} ${requirement.title} ${requirement.description}`
                    .toLowerCase()
                    .includes(query)
            )
        }),
        [requirements, search, selectedModule]
    )

    const metrics = useMemo(() => ({
        total: filteredRequirements.length,
        doing: filteredRequirements.filter(
            (requirement) => requirement.status === "Fazendo"
        ).length,
        done: filteredRequirements.filter(
            (requirement) => requirement.status === "Concluído"
        ).length
    }), [filteredRequirements])

    const queue = (botStatus.tasks || [])
        .filter((task) => task.workstream === selectedModule)
        .sort((a, b) => {
            const sequence = (name: string): number => {
                const match = name.match(/-(\d+)-/)
                return match ? Number(match[1]) : Number.MIN_SAFE_INTEGER
            }

            const difference = sequence(b.name) - sequence(a.name)

            return difference !== 0
                ? difference
                : b.name.localeCompare(a.name)
        })

    const queueRows = 10

    const queueTotalPages = Math.max(
        Math.ceil(queue.length / queueRows),
        1
    )

    const safeQueuePage = Math.min(
        queuePage,
        queueTotalPages
    )

    const paginatedQueue = queue.slice(
        (safeQueuePage - 1) * queueRows,
        safeQueuePage * queueRows
    )

    const makeTaskMarkdown = (requirement: Requirement) => {
        const sprintInfo = sprints.find(
            (sprint) => sprint.key === requirement.module
        ) ?? selectedSprint

        const number = requirement.id.match(/-(\d{3})$/)?.[1] ?? requirement.id
        const displayId = `${sprintInfo.name} ${number}`

        return `# ${displayId} - ${requirement.title}

## Objetivo

${requirement.description}

## Escopo

Módulo: ${sprintInfo.name}
Tipo: ${requirement.type}
Prioridade: ${requirement.priority}

## Critérios de aceite

- [ ] ${requirement.acceptance}
- [ ] Testes automatizados relevantes passam.
- [ ] Não alterar módulos sem relação direta.

## Condição de parada

Parar quando os critérios de aceite estiverem atendidos. Registrar arquivos alterados, validações executadas e pendências no relatório final.
`
    }

    const resetRequirementForm = () => {
        setForm({
            title: "",
            description: "",
            type: "Requisito",
            priority: "Média",
            acceptance: ""
        })
        setEditingRequirementId(null)
        setShowRequirementForm(false)
    }

    const openNewRequirement = () => {
        setEditingRequirementId(null)
        setForm({
            title: "",
            description: "",
            type: "Requisito",
            priority: "Média",
            acceptance: ""
        })
        setShowRequirementForm(true)
    }

    const openEditRequirement = (requirement: Requirement) => {
        setEditingRequirementId(requirement.id)
        setForm({
            title: requirement.title,
            description: requirement.description,
            type: requirement.type,
            priority: requirement.priority,
            acceptance: requirement.acceptance
        })
        setShowRequirementForm(true)
    }

    const saveRequirement = () => {
        if (!selectedSprint) return

        if (!form.title.trim() || !form.description.trim() || !form.acceptance.trim()) {
            setNotice("Preencha título, descrição e critérios de aceite.")
            return
        }

        if (editingRequirementId) {
            setRequirements((current) => current.map((requirement) =>
                requirement.id === editingRequirementId
                    ? {
                        ...requirement,
                        title: form.title.trim(),
                        description: form.description.trim(),
                        type: form.type,
                        priority: form.priority,
                        acceptance: form.acceptance.trim()
                    }
                    : requirement
            ))

            setNotice(`${editingRequirementId} atualizado.`)
            resetRequirementForm()
            return
        }

        const prefix = getPrefix(selectedSprint)
        const sequenceBase = selectedModule === "armamento" ? 32 : 0
        const sequencePattern = new RegExp(`^${prefix}-(\\d{3})$`)

        const highestExistingSequence = requirements
            .filter((requirement) => requirement.module === selectedModule)
            .map((requirement) => {
                const match = requirement.id.match(sequencePattern)
                return match ? Number(match[1]) : 0
            })
            .reduce(
                (highest, current) => Math.max(highest, current),
                sequenceBase
            )

        const nextSequence = highestExistingSequence + 1

        const newRequirement: Requirement = {
            id: `${prefix}-${String(nextSequence).padStart(3, "0")}`,
            title: form.title.trim(),
            description: form.description.trim(),
            module: selectedModule,
            type: form.type,
            status: "Backlog",
            priority: form.priority,
            acceptance: form.acceptance.trim(),
            createdAt: new Date().toISOString().slice(0, 10)
        }

        setRequirements((current) => [newRequirement, ...current])
        setNotice(`${newRequirement.id} adicionado ao backlog.`)
        resetRequirementForm()
    }

    const taskMatchesRequirement = (
        task: QueuedTask,
        requirement: Requirement
    ) => {
        const number = requirement.id.match(/-(\d{3})$/)?.[1]

        return Boolean(
            number &&
            task.workstream === requirement.module &&
            (
                task.name.includes(`-${number}-`) ||
                task.title.includes(` ${number} -`) ||
                task.title.includes(requirement.id)
            )
        )
    }

    const deleteRequirement = (requirement: Requirement) => {
        if ((botStatus.tasks || []).some(
            (task) => taskMatchesRequirement(task, requirement)
        )) {
            setNotice("Retire o requisito da fila do bot antes de excluí-lo.")
            return
        }

        if (!window.confirm(
            `Excluir ${requirement.id} - ${requirement.title}?`
        )) {
            return
        }

        setRequirements((current) => current.filter(
            (item) => item.id !== requirement.id
        ))

        setNotice(`${requirement.id} excluído do backlog.`)
    }

    const updateStatus = (
        id: string,
        status: RequirementStatus
    ) => {
        setRequirements((current) => current.map((requirement) =>
            requirement.id === id
                ? { ...requirement, status }
                : requirement
        ))
    }

    const resetSprintForm = () => {
        setSprintForm({
            name: "",
            detail: "",
            accent: "#e85d04",
            steps: 1
        })
        setEditingSprintKey(null)
        setShowSprintForm(false)
    }

    const openNewSprint = () => {
        setEditingSprintKey(null)
        setSprintForm({
            name: "",
            detail: "",
            accent: "#e85d04",
            steps: 1
        })
        setShowSprintForm(true)
    }

    const openEditSprint = (sprint: Sprint) => {
        setEditingSprintKey(sprint.key)
        setSprintForm({
            name: sprint.name,
            detail: sprint.detail,
            accent: sprint.accent,
            steps: sprint.steps
        })
        setShowSprintForm(true)
    }

    const saveSprint = () => {
        const name = sprintForm.name.trim()
        const detail = sprintForm.detail.trim()

        if (!name || !detail || sprintForm.steps < 1) {
            setNotice("Preencha nome, descrição e etapas da sprint.")
            return
        }

        if (editingSprintKey) {
            setSprints((current) => current.map((sprint) =>
                sprint.key === editingSprintKey
                    ? {
                        ...sprint,
                        name,
                        detail,
                        accent: sprintForm.accent,
                        steps: sprintForm.steps
                    }
                    : sprint
            ))

            setNotice(`${name} atualizada.`)
            resetSprintForm()
            return
        }

        const key = slugify(name)

        if (!key || !/^[a-z0-9][a-z0-9-]{1,39}$/.test(key)) {
            setNotice("Nome da sprint gera um identificador inválido.")
            return
        }

        if (sprints.some((sprint) => sprint.key === key)) {
            setNotice("Já existe uma sprint com esse nome.")
            return
        }

        const newSprint: Sprint = {
            key,
            name,
            detail,
            accent: sprintForm.accent,
            steps: sprintForm.steps
        }

        setSprints((current) => [...current, newSprint])
        setSelectedModule(key)
        setNotice(`${name} criada.`)
        resetSprintForm()
    }

    const deleteSprint = (sprint: Sprint) => {
        const hasRequirements = requirements.some(
            (requirement) => requirement.module === sprint.key
        )

        const hasTasks = (botStatus.tasks || []).some(
            (task) => task.workstream === sprint.key
        )

        if (hasRequirements || hasTasks) {
            setNotice(
                "A sprint só pode ser excluída quando não tiver requisitos nem tarefas na fila."
            )
            return
        }

        if (sprints.length === 1) {
            setNotice("É necessário manter pelo menos uma sprint.")
            return
        }

        if (!window.confirm(`Excluir a sprint ${sprint.name}?`)) {
            return
        }

        const remaining = sprints.filter(
            (item) => item.key !== sprint.key
        )

        setSprints(remaining)

        if (selectedModule === sprint.key) {
            setSelectedModule(remaining[0].key)
        }

        setNotice(`${sprint.name} excluída.`)
    }

    const sendRequest = async (
        path: string,
        data: object,
        method = "POST"
    ) => {
        setBusy(true)

        try {
            const response = await fetch(path, {
                method,
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(data)
            })

            const responseText = await response.text()

            let result: {
                error?: string
                message?: string
            } = {}

            if (responseText.trim()) {
                try {
                    result = JSON.parse(responseText) as {
                        error?: string
                        message?: string
                    }
                } catch {
                    result = {
                        message: responseText
                    }
                }
            }

            if (!response.ok) {
                throw new Error(
                    result.error ||
                    result.message ||
                    `Não foi possível concluir o comando. HTTP ${response.status}.`
                )
            }

            setNotice(
                result.message ||
                "Comando concluído com sucesso."
            )

            await refreshStatus()

            return true
        } catch (error) {
            setNotice(
                error instanceof Error
                    ? error.message
                    : "Não foi possível comunicar com o bot."
            )

            return false
        } finally {
            setBusy(false)
        }
    }

    const downloadTask = (requirement: Requirement) =>
        sendRequest("/api/bot/tasks", {
            id: requirement.id,
            module: requirement.module,
            markdown: makeTaskMarkdown(requirement)
        })

    const removeQueuedTask = async (task: QueuedTask) => {
        const confirmed = window.confirm(
            `Deseja realmente retirar "${task.title}" da fila do bot e devolvê-lo ao Backlog?`
        )

        if (!confirmed) {
            return
        }

        const removed = await sendRequest(
            "/api/bot/tasks",
            { name: task.name },
            "DELETE"
        )

        if (!removed) return

        setRequirements((current) => current.map((requirement) =>
            taskMatchesRequirement(task, requirement)
                ? { ...requirement, status: "Backlog" }
                : requirement
        ))
    }

    const sendBotCommand = (
        command: "start" | "pause" | "resume" | "stop"
    ) => sendRequest(
        "/api/bot/control",
        {
            command,
            workstream: selectedModule
        }
    )

    if (!selectedSprint) {
        return null
    }

    return (
        <Layout title="Command Center">
            <div className="command-center">
                <style jsx global>{`
                    /* COMMAND_CENTER_BOT_STATUS_SUMMARY */

                    .command-center .bot-status-summary {
                        display: grid;
                        grid-template-columns: minmax(170px, 0.7fr) minmax(260px, 1.4fr) minmax(220px, 1fr);
                        gap: 1rem;
                        align-items: center;
                        margin-bottom: 1rem;
                        padding: 1rem 1.1rem;
                        background: #181818;
                        border: 1px solid #343434;
                        border-radius: 0.75rem;
                    }

                    .command-center .bot-status-summary-main,
                    .command-center .bot-status-summary-task,
                    .command-center .bot-status-summary-message {
                        min-width: 0;
                    }

                    .command-center .bot-status-summary-main {
                        display: flex;
                        align-items: center;
                        gap: 0.75rem;
                    }

                    .command-center .bot-status-summary-main > div,
                    .command-center .bot-status-summary-task,
                    .command-center .bot-status-summary-message {
                        display: flex;
                        flex-direction: column;
                        gap: 0.2rem;
                    }

                    .command-center .bot-status-summary .eyebrow {
                        color: #a3a3a3 !important;
                    }

                    .command-center .bot-status-summary strong {
                        color: #ffffff !important;
                        font-weight: 700;
                        line-height: 1.25;
                    }

                    .command-center .bot-status-summary-message > span:last-child {
                        color: #d4d4d4 !important;
                        line-height: 1.35;
                    }

                    .command-center .bot-status-summary-task strong,
                    .command-center .bot-status-summary-message > span:last-child {
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                    }

                    @media (max-width: 900px) {
                        .command-center .bot-status-summary {
                            grid-template-columns: 1fr;
                        }

                        .command-center .bot-status-summary-task strong,
                        .command-center .bot-status-summary-message > span:last-child {
                            white-space: normal;
                        }
                    }
                `}</style>
                <style jsx global>{`
                    /* COMMAND_CENTER_CARD_DIVIDERS */

                    /* Linha divisoria logo abaixo do titulo dos cards. */
                    .command-center .module-tile > strong,
                    .command-center .module-tile > h3,
                    .command-center .module-tile > h4,
                    .command-center .requirement-card > h3,
                    .command-center .requirement-card > h4 {
                        display: block !important;
                        width: 100% !important;
                        padding-bottom: 0.75rem !important;
                        margin-bottom: 0.75rem !important;
                        border-bottom: 1px solid #343434 !important;
                    }

                    /* Linha acima da area de acoes dos Sprints. */
                    .command-center .module-tile .actions,
                    .command-center .module-tile .card-actions,
                    .command-center .module-tile .module-actions,
                    .command-center .module-tile .sprint-actions,
                    .command-center .module-tile > div:has(
                        button[aria-label*="Editar" i],
                        button[title*="Editar" i],
                        button[aria-label*="Excluir" i],
                        button[title*="Excluir" i]
                    ) {
                        width: 100% !important;
                        padding-top: 0.75rem !important;
                        margin-top: 0.75rem !important;
                        border-top: 1px solid #343434 !important;
                    }

                    /* Linha acima da area de acoes dos Requisitos. */
                    .command-center .requirement-card .actions,
                    .command-center .requirement-card .card-actions,
                    .command-center .requirement-card .requirement-actions,
                    .command-center .requirement-card > div:has(
                        button[aria-label*="Editar" i],
                        button[title*="Editar" i],
                        .requirement-send-icon,
                        button[aria-label*="Excluir" i],
                        button[title*="Excluir" i]
                    ) {
                        width: 100% !important;
                        padding-top: 0.75rem !important;
                        margin-top: 0.75rem !important;
                        border-top: 1px solid #343434 !important;
                    }
                `}</style>
                <style jsx global>{`
                    /* COMMAND_CENTER_CARD_DIVIDERS */

                    /* Linha divisoria logo abaixo do titulo dos cards. */
                    .command-center .module-tile > strong,
                    .command-center .module-tile > h3,
                    .command-center .module-tile > h4,
                    .command-center .requirement-card > h3,
                    .command-center .requirement-card > h4 {
                        display: block !important;
                        width: 100% !important;
                        padding-bottom: 0.75rem !important;
                        margin-bottom: 0.75rem !important;
                        border-bottom: 1px solid #343434 !important;
                    }

                    /* Linha acima da area de acoes dos Sprints. */
                    .command-center .module-tile .actions,
                    .command-center .module-tile .card-actions,
                    .command-center .module-tile .module-actions,
                    .command-center .module-tile .sprint-actions,
                    .command-center .module-tile > div:has(
                        button[aria-label*="Editar" i],
                        button[title*="Editar" i],
                        button[aria-label*="Excluir" i],
                        button[title*="Excluir" i]
                    ) {
                        width: 100% !important;
                        padding-top: 0.75rem !important;
                        margin-top: 0.75rem !important;
                        border-top: 1px solid #343434 !important;
                    }

                    /* Linha acima da area de acoes dos Requisitos. */
                    .command-center .requirement-card .actions,
                    .command-center .requirement-card .card-actions,
                    .command-center .requirement-card .requirement-actions,
                    .command-center .requirement-card > div:has(
                        button[aria-label*="Editar" i],
                        button[title*="Editar" i],
                        .requirement-send-icon,
                        button[aria-label*="Excluir" i],
                        button[title*="Excluir" i]
                    ) {
                        width: 100% !important;
                        padding-top: 0.75rem !important;
                        margin-top: 0.75rem !important;
                        border-top: 1px solid #343434 !important;
                    }
                `}</style>
                <style jsx global>{`
                    /* COMMAND_CENTER_REQUIREMENT_SEND_ICON */

                    .command-center .card-action-prime-icon {
                        font-weight: 400 !important;
                        font-style: normal !important;
                    }

                    .command-center .requirement-card .requirement-send-icon {
                        display: inline-flex !important;
                        align-items: center !important;
                        justify-content: center !important;
                        flex: 0 0 auto !important;
                        width: auto !important;
                        min-width: 0 !important;
                        height: auto !important;
                        margin: 0 !important;
                        padding: 0 !important;
                        background: transparent !important;
                        background-color: transparent !important;
                        border: none !important;
                        border-radius: 0 !important;
                        box-shadow: none !important;
                        color: #22c55e !important;
                        opacity: 1 !important;
                    }

                    .command-center .requirement-card .requirement-send-icon svg {
                        display: block !important;
                        width: 1.125rem !important;
                        height: 1.125rem !important;
                        color: #22c55e !important;
                        stroke: #22c55e !important;
                    }

                    .command-center .requirement-card .requirement-send-icon:hover,
                    .command-center .requirement-card .requirement-send-icon:focus,
                    .command-center .requirement-card .requirement-send-icon:focus-visible {
                        background: transparent !important;
                        background-color: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                        outline: none !important;
                    }

                    .command-center .requirement-card .requirement-send-icon:hover {
                        opacity: 0.75 !important;
                    }

                    /* Area de acoes do card: Editar + Enviar ao bot + Excluir,
                       alinhados a direita e centralizados verticalmente. */
                    .command-center .requirement-card .actions,
                    .command-center .requirement-card .card-actions,
                    .command-center .requirement-card .requirement-actions,
                    .command-center .requirement-card > div:has(
                        button[aria-label*="Editar" i],
                        .requirement-send-icon
                    ) {
                        display: flex !important;
                        align-items: center !important;
                        justify-content: flex-end !important;
                        width: 100% !important;
                        gap: 0.65rem !important;
                        margin-left: auto !important;
                    }

                    /* Ordem: Editar, Enviar ao bot, Excluir. */
                    .command-center .requirement-card button[aria-label*="Editar" i],
                    .command-center .requirement-card button[title*="Editar" i],
                    .command-center .requirement-card [data-card-action="edit"] {
                        order: 1 !important;
                    }

                    .command-center .requirement-card .requirement-send-icon {
                        order: 2 !important;
                    }

                    .command-center .requirement-card button[aria-label*="Excluir" i],
                    .command-center .requirement-card button[title*="Excluir" i],
                    .command-center .requirement-card [data-card-action="delete"] {
                        order: 3 !important;
                    }
                `}</style>
                <style jsx global>{`
                    /* COMMAND_CENTER_SPRINT_ACTIONS_RIGHT */

                    /* Container dos botoes Editar/Excluir dentro do card de Sprint. */
                    .command-center .module-tile .actions,
                    .command-center .module-tile .card-actions,
                    .command-center .module-tile .module-actions,
                    .command-center .module-tile .sprint-actions,
                    .command-center .module-tile > div:has(
                        button[aria-label*="Editar" i],
                        button[title*="Editar" i],
                        button[aria-label*="Excluir" i],
                        button[title*="Excluir" i]
                    ) {
                        display: flex !important;
                        align-items: center !important;
                        justify-content: flex-end !important;
                        width: 100% !important;
                        margin-left: auto !important;
                        gap: 0.65rem !important;
                    }

                    /* Os proprios botoes permanecem juntos no lado direito. */
                    .command-center .module-tile button[aria-label*="Editar" i],
                    .command-center .module-tile button[title*="Editar" i],
                    .command-center .module-tile button[aria-label*="Excluir" i],
                    .command-center .module-tile button[title*="Excluir" i] {
                        flex: 0 0 auto !important;
                        margin-left: 0 !important;
                    }
                `}</style>
                <style jsx global>{`
                    /* COMMAND_CENTER_CARD_ACTIONS_VISIBLE */

                    .command-center .module-tile button,
                    .command-center .requirement-card button,
                    .command-center .module-tile [role="button"],
                    .command-center .requirement-card [role="button"] {
                        visibility: visible !important;
                    }

                    .command-center .module-tile button svg,
                    .command-center .requirement-card button svg,
                    .command-center .module-tile button i,
                    .command-center .requirement-card button i {
                        visibility: visible !important;
                        opacity: 1 !important;
                        display: inline-block !important;
                    }

                    .command-center .module-tile button[aria-label*="Editar" i],
                    .command-center .module-tile button[title*="Editar" i],
                    .command-center .requirement-card button[aria-label*="Editar" i],
                    .command-center .requirement-card button[title*="Editar" i],
                    .command-center .requirement-card [data-card-action="edit"] {
                        color: #ffffff !important;
                        background: transparent !important;
                        background-color: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                        opacity: 1 !important;
                    }

                    .command-center .module-tile button[aria-label*="Excluir" i],
                    .command-center .module-tile button[title*="Excluir" i],
                    .command-center .requirement-card button[aria-label*="Excluir" i],
                    .command-center .requirement-card button[title*="Excluir" i],
                    .command-center .requirement-card [data-card-action="delete"] {
                        color: #ef4444 !important;
                        background: transparent !important;
                        background-color: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                        opacity: 1 !important;
                    }

                    .command-center .requirement-card button[aria-label*="Enviar ao bot" i],
                    .command-center .requirement-card button[title*="Enviar ao bot" i] {
                        color: #22c55e !important;
                        background: transparent !important;
                        background-color: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                        opacity: 1 !important;
                    }

                    .command-center .module-tile button[aria-label*="Editar" i] svg,
                    .command-center .module-tile button[title*="Editar" i] svg,
                    .command-center .requirement-card button[aria-label*="Editar" i] svg,
                    .command-center .requirement-card button[title*="Editar" i] svg {
                        color: #ffffff !important;
                        stroke: #ffffff !important;
                    }

                    .command-center .module-tile button[aria-label*="Excluir" i] svg,
                    .command-center .module-tile button[title*="Excluir" i] svg,
                    .command-center .requirement-card button[aria-label*="Excluir" i] svg,
                    .command-center .requirement-card button[title*="Excluir" i] svg {
                        color: #ef4444 !important;
                        stroke: #ef4444 !important;
                    }

                    .command-center .requirement-card button[aria-label*="Enviar ao bot" i] svg,
                    .command-center .requirement-card button[title*="Enviar ao bot" i] svg {
                        color: #22c55e !important;
                        stroke: #22c55e !important;
                    }

                    .command-center .module-tile button:hover,
                    .command-center .requirement-card button:hover {
                        opacity: 0.8 !important;
                    }
                `}</style>
                <style jsx global>{`
                    /* COMMAND_CENTER_TRANSPARENT_CARD_ACTIONS */

                    /* SPRINTS + BACKLOG:
                       remove fundo amarelo e bordas somente das acoes. */
                    .command-center .module-tile button[aria-label*="Editar" i],
                    .command-center .module-tile button[title*="Editar" i],
                    .command-center .module-tile button[aria-label*="Excluir" i],
                    .command-center .module-tile button[title*="Excluir" i],
                    .command-center .requirement-card button[aria-label*="Editar" i],
                    .command-center .requirement-card button[title*="Editar" i],
                    .command-center .requirement-card button[aria-label*="Excluir" i],
                    .command-center .requirement-card button[title*="Excluir" i],
                    .command-center .requirement-card button[aria-label*="Enviar ao bot" i],
                    .command-center .requirement-card button[title*="Enviar ao bot" i],
                    .command-center .requirement-card [data-card-action="edit"],
                    .command-center .requirement-card [data-card-action="delete"] {
                        background: transparent !important;
                        background-color: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                    }

                    .command-center .module-tile button[aria-label*="Editar" i],
                    .command-center .module-tile button[title*="Editar" i],
                    .command-center .requirement-card button[aria-label*="Editar" i],
                    .command-center .requirement-card button[title*="Editar" i],
                    .command-center .requirement-card [data-card-action="edit"] {
                        color: #ffffff !important;
                    }

                    .command-center .module-tile button[aria-label*="Excluir" i],
                    .command-center .module-tile button[title*="Excluir" i],
                    .command-center .requirement-card button[aria-label*="Excluir" i],
                    .command-center .requirement-card button[title*="Excluir" i],
                    .command-center .requirement-card [data-card-action="delete"] {
                        color: #ef4444 !important;
                    }

                    .command-center .requirement-card button[aria-label*="Enviar ao bot" i],
                    .command-center .requirement-card button[title*="Enviar ao bot" i] {
                        color: #22c55e !important;
                    }

                    .command-center .module-tile button[aria-label*="Editar" i]:hover,
                    .command-center .module-tile button[title*="Editar" i]:hover,
                    .command-center .module-tile button[aria-label*="Excluir" i]:hover,
                    .command-center .module-tile button[title*="Excluir" i]:hover,
                    .command-center .requirement-card button[aria-label*="Editar" i]:hover,
                    .command-center .requirement-card button[title*="Editar" i]:hover,
                    .command-center .requirement-card button[aria-label*="Excluir" i]:hover,
                    .command-center .requirement-card button[title*="Excluir" i]:hover,
                    .command-center .requirement-card button[aria-label*="Enviar ao bot" i]:hover,
                    .command-center .requirement-card button[title*="Enviar ao bot" i]:hover,
                    .command-center .requirement-card [data-card-action="edit"]:hover,
                    .command-center .requirement-card [data-card-action="delete"]:hover {
                        background: transparent !important;
                        background-color: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                        opacity: 0.75 !important;
                    }

                    .command-center .module-tile button[aria-label*="Editar" i]:focus,
                    .command-center .module-tile button[aria-label*="Excluir" i]:focus,
                    .command-center .requirement-card button[aria-label*="Editar" i]:focus,
                    .command-center .requirement-card button[aria-label*="Excluir" i]:focus,
                    .command-center .requirement-card button[aria-label*="Enviar ao bot" i]:focus,
                    .command-center .requirement-card [data-card-action="edit"]:focus,
                    .command-center .requirement-card [data-card-action="delete"]:focus {
                        background: transparent !important;
                        background-color: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                        outline: none !important;
                    }
                `}</style>
<style jsx global>{`
                    /* COMMAND_CENTER_TABLE_ACTIONS_PRIMEICONS */

                    .command-center table th:last-child,
                    .command-center table td:last-child {
                        text-align: right !important;
                        vertical-align: middle !important;
                    }

                    .command-center table td:last-child > div,
                    .command-center .table-actions,
                    .command-center .queue-actions,
                    .command-center .actions {
                        display: flex !important;
                        align-items: center !important;
                        justify-content: flex-end !important;
                        gap: 0.75rem !important;
                        min-height: 100% !important;
                    }

                    .command-center table td:last-child button {
                        display: inline-flex !important;
                        align-items: center !important;
                        justify-content: center !important;
                        width: auto !important;
                        min-width: 0 !important;
                        height: auto !important;
                        padding: 0 !important;
                        margin: 0 !important;
                        background: transparent !important;
                        background-color: transparent !important;
                        border: none !important;
                        border-radius: 0 !important;
                        box-shadow: none !important;
                        line-height: 1 !important;
                    }

                    .command-center table td:last-child button:hover:not(:disabled),
                    .command-center table td:last-child button:focus,
                    .command-center table td:last-child button:focus-visible {
                        background: transparent !important;
                        background-color: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                        outline: none !important;
                    }

                    .command-center table td:last-child button:hover:not(:disabled) {
                        opacity: 0.72 !important;
                    }

                    .command-center table td:last-child button svg {
                        display: block !important;
                        width: 1.125rem !important;
                        height: 1.125rem !important;
                        stroke: currentColor !important;
                        color: currentColor !important;
                    }

                    /* Editar: neutro / cor padrao do texto */
                    .command-center table td:last-child button[aria-label="Editar"] {
                        color: inherit !important;
                    }

                    /* Excluir: vermelho */
                    .command-center table td:last-child button[aria-label="Excluir"] {
                        color: #ef4444 !important;
                    }

                    /* Enviar ao bot: verde */
                    .command-center table td:last-child button[aria-label="Enviar ao bot"] {
                        color: #22c55e !important;
                    }

                    /* Voltar para backlog: verde */
                    .command-center table td:last-child button[aria-label="Voltar para backlog"] {
                        color: #22c55e !important;
                    }

                    .command-center table td:last-child button:disabled {
                        opacity: 0.35 !important;
                        cursor: not-allowed !important;
                    }
                `}</style>
                <style jsx global>{`
                    /* COMMAND_CENTER_QUEUE_ACTIONS_FIXED */

                    .command-center [aria-label="Fila real do bot"] table th:last-child,
                    .command-center [aria-label="Fila real do bot"] table td:last-child {
                        text-align: right !important;
                    }

                    .command-center [aria-label="Fila real do bot"] table td:last-child > div {
                        display: flex !important;
                        justify-content: flex-end !important;
                        align-items: center !important;
                        gap: 0.5rem !important;
                    }

                    .command-center [aria-label="Fila real do bot"] table td:last-child button {
                        display: inline-flex !important;
                        align-items: center !important;
                        justify-content: center !important;
                        padding: 0 !important;
                        margin: 0 !important;
                        min-width: 0 !important;
                        width: auto !important;
                        height: auto !important;
                        background: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                        color: #22c55e !important;
                    }

                    .command-center [aria-label="Fila real do bot"] table td:last-child button:hover:not(:disabled),
                    .command-center [aria-label="Fila real do bot"] table td:last-child button:focus,
                    .command-center [aria-label="Fila real do bot"] table td:last-child button:focus-visible {
                        background: transparent !important;
                        border: none !important;
                        box-shadow: none !important;
                        outline: none !important;
                    }

                    .command-center [aria-label="Fila real do bot"] table td:last-child button:hover:not(:disabled) {
                        opacity: 0.75 !important;
                    }

                    .command-center [aria-label="Fila real do bot"] table td:last-child button svg {
                        color: #22c55e !important;
                        stroke: #22c55e !important;
                        width: 1.125rem !important;
                        height: 1.125rem !important;
                    }
                `}</style>
<style jsx global>{`
                    /* COMMAND_CENTER_YELLOW_THEME_V2 */

                    .command-center label,
                    .command-center .eyebrow,
                    .command-center .form-heading label,
                    .command-center .requirement-form label,
                    .command-center .card-meta,
                    .command-center .type-label,
                    .command-center .column-heading,
                    .command-center .column-heading h3 {
                        color: #ffffff !important;
                    }

                    .command-center .command-button,
                    .command-center .command-button-primary,
                    .command-center .command-button-danger,
                    .command-center .task-export,
                    .command-center .icon-button {
                        background: #ff9900 !important;
                        background-color: #ff9900 !important;
                        border-color: #ff9900 !important;
                        color: #171717 !important;
                    }

                    .command-center .command-button:hover:not(:disabled),
                    .command-center .command-button-primary:hover:not(:disabled),
                    .command-center .command-button-danger:hover:not(:disabled),
                    .command-center .task-export:hover:not(:disabled),
                    .command-center .icon-button:hover:not(:disabled) {
                        background: #e68a00 !important;
                        background-color: #e68a00 !important;
                        border-color: #e68a00 !important;
                        color: #171717 !important;
                    }

                    .command-center .command-button:disabled,
                    .command-center .command-button-primary:disabled,
                    .command-center .command-button-danger:disabled,
                    .command-center .task-export:disabled,
                    .command-center .icon-button:disabled {
                        opacity: 0.5 !important;
                        cursor: not-allowed !important;
                    }

                    .command-center .command-button svg,
                    .command-center .command-button-primary svg,
                    .command-center .command-button-danger svg,
                    .command-center .task-export svg,
                    .command-center .icon-button svg {
                        color: #171717 !important;
                        stroke: #171717 !important;
                    }
                `}</style>
            <style jsx global>{`
                .command-center {
                    --cc-card-bg: #181818;
                    --cc-card-bg-strong: #121212;
                    --cc-card-border: #343434;
                    --cc-text: #f5f5f5;
                    --cc-text-muted: #a3a3a3;
                }

                .command-center .module-tile,
                .command-center .command-metrics > div,
                .command-center .requirement-form,
                .command-center .requirement-card,
                .command-center .scrum-column,
                .command-center .bot-run-panel,
                .command-center .command-notice,
                .command-center .column-empty,
                .command-center [aria-label="Fila real do bot"] {
                    background: var(--cc-card-bg) !important;
                    border-color: var(--cc-card-border) !important;
                    color: var(--cc-text) !important;
                }

                .command-center .module-tile.is-selected,
                .command-center .bot-run-icon,
                .command-center [aria-label="Fila real do bot"] thead {
                    background: var(--cc-card-bg-strong) !important;
                }

                .command-center .module-tile strong,
                .command-center .module-tile span,
                .command-center .command-metrics strong,
                .command-center .requirement-form h3,
                .command-center .requirement-card h4,
                .command-center .scrum-column h3,
                .command-center .bot-run-panel h3,
                .command-center [aria-label="Fila real do bot"] h3,
                .command-center [aria-label="Fila real do bot"] th,
                .command-center [aria-label="Fila real do bot"] td {
                    color: var(--cc-text);
                }

                .command-center .module-tile small,
                .command-center .command-metrics span,
                .command-center .requirement-card p,
                .command-center .bot-run-panel p,
                .command-center .column-empty,
                .command-center [aria-label="Fila real do bot"] p,
                .command-center [aria-label="Fila real do bot"] small {
                    color: var(--cc-text-muted) !important;
                }

                .command-center .requirement-form input,
                .command-center .requirement-form textarea,
                .command-center .requirement-form select,
                .command-center .requirement-card select,
                .command-center .command-search,
                .command-center .command-search input {
                    background: #202020 !important;
                    border-color: var(--cc-card-border) !important;
                    color: var(--cc-text) !important;
                }

                .command-center [aria-label="Fila real do bot"] table,
                .command-center [aria-label="Fila real do bot"] tbody,
                .command-center [aria-label="Fila real do bot"] tr,
                .command-center [aria-label="Fila real do bot"] td {
                    background: var(--cc-card-bg) !important;
                    border-color: var(--cc-card-border) !important;
                }

                .command-center [aria-label="Fila real do bot"] thead,
                .command-center [aria-label="Fila real do bot"] th {
                    background: var(--cc-card-bg-strong) !important;
                    border-color: var(--cc-card-border) !important;
                }

                .command-center .comandos-datatable-paginator {
                    background: transparent !important;
                    color: var(--cc-text) !important;
                }
            `}</style>
                                                        <style jsx global>{`
                    /* COMMAND_CENTER_WORKER_SPRINT_SPACING */

                    .command-center .bot-run-panel .bot-controls {
                        display: flex !important;
                        align-items: center !important;
                        justify-content: flex-end !important;
                        flex-wrap: nowrap !important;
                        gap: 0.75rem !important;
                    }

                    .command-center .bot-run-panel .bot-controls .command-button {
                        display: inline-flex !important;
                        align-items: center !important;
                        justify-content: center !important;
                        min-height: 2.5rem !important;
                        margin: 0 !important;
                        white-space: nowrap !important;
                    }

                    .command-center .bot-run-panel + .command-toolbar {
                        margin-top: 2rem !important;
                    }

                    @media (max-width: 900px) {
                        .command-center .bot-run-panel .bot-controls {
                            flex-wrap: wrap !important;
                        }
                    }
                `}</style>

                                <style jsx global>{`
                    /* COMMAND_CENTER_BOT_OVERVIEW_CARD */

                    .command-center .bot-overview-worker .bot-run-panel {
                        grid-template-columns: minmax(0, 1fr) auto !important;
                    }

                    .command-center .codex-rate-limit-reset {
                        margin-top: 0.35rem !important;
                        color: #f5f5f5 !important;
                        font-weight: 600;
                    }

                    .command-center .bot-overview-card {
                        width: 100%;
                        margin-bottom: 1rem;
                        padding: 1rem 1.1rem;
                        background: #181818;
                        border: 1px solid #343434;
                        border-radius: 0.75rem;
                        color: #f5f5f5;
                    }

                    .command-center .bot-overview-card .bot-status-summary,
                    .command-center .bot-overview-card .bot-run-panel {
                        margin: 0 !important;
                        padding: 0 !important;
                        background: transparent !important;
                        border: none !important;
                        border-radius: 0 !important;
                        box-shadow: none !important;
                    }

                    .command-center .bot-overview-divider {
                        width: 100%;
                        height: 1px;
                        margin: 1rem 0;
                        background: #343434;
                    }

                    .command-center .bot-overview-worker .bot-run-panel {
                        width: 100%;
                    }

                    .command-center .bot-overview-worker .bot-controls {
                        display: flex !important;
                        align-items: center !important;
                        justify-content: flex-end !important;
                        gap: 0.75rem !important;
                        flex-wrap: nowrap !important;
                    }

                    .command-center .bot-overview-card + .command-toolbar {
                        margin-top: 2rem !important;
                    }

                    @media (max-width: 900px) {
                        .command-center .bot-overview-worker .bot-controls {
                            flex-wrap: wrap !important;
                        }
                    }
                `}</style>

                <section className="bot-overview-card" aria-label="Status e controle do bot">
                    <div className="bot-overview-status">
                        <section className="bot-status-summary" aria-label="Status atual do bot">
                    <div className="bot-status-summary-main">
                    <span className={`comandos-bot-status-light comandos-bot-status-${botStatus.state}`} aria-hidden="true" />
                    <div>
                    <span className="eyebrow">STATUS DO BOT</span>
                    <strong>{stateLabels[botStatus.state] || botStatus.state}</strong>
                    </div>
                    </div>

                    <div className="bot-status-summary-task">
                    <span className="eyebrow">
                    {["working", "validating"].includes(botStatus.state)
                    ? "REQUISITO EM EXECUÇÃO"
                    : "REQUISITO ATUAL"}
                    </span>
                    <strong>
                    {["working", "validating"].includes(botStatus.state)
                    ? (
                    (botStatus.tasks || []).find((task) => task.state === "working")?.title ||
                    (botStatus.tasks || []).find((task) => task.name === botStatus.taskName)?.title ||
                    botStatus.taskName ||
                    "Requisito em execução não identificado"
                    )
                    : "Nenhum requisito em execução"}
                    </strong>
                    </div>

                    <div className="bot-status-summary-message">
                    <span className="eyebrow">DETALHE</span>
                    <span>{botStatus.message || "Sem atualização do worker."}</span>
                    </div>
                    </section>
                    </div>

                    <div className="bot-overview-divider" aria-hidden="true" />

                    <div className="bot-overview-worker">
                        <section className="bot-run-panel">
                    <div>
                        <span className="eyebrow">
                            CONTROLE DO WORKER
                        </span>
                        <h3>{botStatus.message}</h3>
                        <p>
                            {botStatus.taskName || selectedModule}
                            {" | "}
                            Atualizado:{" "}
                            {botStatus.updatedAt
                                ? new Date(botStatus.updatedAt).toLocaleTimeString()
                                : "nunca"}
                        </p>
                        {(() => {
                            const resetTime = formatRateLimitReset(botStatus)

                            if (!resetTime) {
                                return null
                            }

                            return (
                                <p className="codex-rate-limit-reset">
                                    Codex liberado para trabalhar às {resetTime}.
                                </p>
                            )
                        })()}
                    </div>

                    <div className="bot-controls">
                        <button
                            className="command-button command-button-primary"
                            onClick={() => void sendBotCommand("start")}
                            disabled={
                                busy ||
                                botStatus.alive ||
                                !queue.some((task) =>
                                    ["pending", "working"].includes(task.state)
                                )
                            }
                        >
                            Iniciar próxima etapa
                        </button>

                        <button
                            className="command-button"
                            onClick={() => void sendBotCommand("pause")}
                            disabled={
                                busy ||
                                !botStatus.alive ||
                                !["working", "waiting", "validating"].includes(
                                    botStatus.state
                                )
                            }
                        >
                            Pausar após etapa
                        </button>

                        <button
                            className="command-button"
                            onClick={() => void sendBotCommand("resume")}
                            disabled={
                                busy ||
                                !["paused", "pause-requested"].includes(
                                    botStatus.state
                                )
                            }
                        >
                            Continuar
                        </button>

                        <button
                            className="command-button command-button-danger"
                            onClick={() => void sendBotCommand("stop")}
                            disabled={busy || !botStatus.alive}
                        >
                            <X size={15} />
                            Parar
                        </button>
                    </div>
                </section>
                    </div>
                </section>

                

<div className="command-toolbar">

<div>
                        <span className="eyebrow">SPRINTS</span>
                        <h2>Planejamento do ERP</h2>
                    </div>

                    <div className="command-actions">
                        <button
                            className="command-button command-button-primary"
                            onClick={openNewSprint}
                        >
                            <Plus size={16} />
                            Nova sprint
                        </button>
                    </div>
                </div>

                <section
                    className="module-strip"
                    aria-label="Sprints do ERP"
                >
                    {sprints.map((sprint, index) => (
                        <div
                            key={sprint.key}
                            className={`module-tile ${
                                selectedModule === sprint.key
                                    ? "is-selected"
                                    : ""
                            }`}
                            style={{
                                "--module-accent": sprint.accent
                            } as React.CSSProperties}
                        >
                            <button
                                type="button"
                                onClick={() => setSelectedModule(sprint.key)}
                                style={{
                                    all: "unset",
                                    cursor: "pointer",
                                    display: "block"
                                }}
                            >
                                <span className="module-index">
                                    {String(index + 1).padStart(2, "0")}
                                </span>
                                <strong>{sprint.name}</strong>
                                <small>{sprint.detail}</small>
                                <span className="module-steps">
                                    {sprint.steps} etapas previstas
                                </span>
                            </button>

                            <div
                                className="command-actions"
                                style={{ marginTop: 10 }}
                            >
                                <button
                                    className="icon-button"
                                    aria-label={`Editar ${sprint.name}`}
                                    onClick={() => openEditSprint(sprint)}
                                >
                                    <Pencil className="card-action-prime-icon" />
                                </button>

                                <button
                                    className="icon-button"
                                    aria-label={`Excluir ${sprint.name}`}
                                    onClick={() => deleteSprint(sprint)}
                                >
                                    <Trash className="card-action-prime-icon" />
                                </button>
                            </div>
                        </div>
                    ))}
                </section>

                {showSprintForm && (
                    <section className="requirement-form">
                        <div className="form-heading">
                            <div>
                                <span className="eyebrow">
                                    {editingSprintKey
                                        ? "EDITAR SPRINT"
                                        : "NOVA SPRINT"}
                                </span>
                                <h3>
                                    {editingSprintKey
                                        ? "Editar sprint atual"
                                        : "Criar nova sprint"}
                                </h3>
                            </div>

                            <button
                                className="icon-button"
                                aria-label="Fechar formulário de sprint"
                                onClick={resetSprintForm}
                            >
                                <X size={17} />
                            </button>
                        </div>

                        <div className="form-grid">
                            <label>
                                Nome
                                <input
                                    value={sprintForm.name}
                                    onChange={(event) =>
                                        setSprintForm({
                                            ...sprintForm,
                                            name: event.target.value
                                        })
                                    }
                                />
                            </label>

                            <label>
                                Etapas previstas
                                <input
                                    type="number"
                                    min={1}
                                    value={sprintForm.steps}
                                    onChange={(event) =>
                                        setSprintForm({
                                            ...sprintForm,
                                            steps: Number(event.target.value) || 1
                                        })
                                    }
                                />
                            </label>

                            <label className="form-wide">
                                Descrição
                                <textarea
                                    value={sprintForm.detail}
                                    onChange={(event) =>
                                        setSprintForm({
                                            ...sprintForm,
                                            detail: event.target.value
                                        })
                                    }
                                />
                            </label>

                            <label>
                                Cor
                                <input
                                    type="color"
                                    value={sprintForm.accent}
                                    onChange={(event) =>
                                        setSprintForm({
                                            ...sprintForm,
                                            accent: event.target.value
                                        })
                                    }
                                />
                            </label>
                        </div>

                        <div className="form-footer">
                            <button
                                className="command-button"
                                onClick={resetSprintForm}
                            >
                                Cancelar
                            </button>

                            <button
                                className="command-button command-button-primary"
                                onClick={saveSprint}
                            >
                                Salvar sprint
                            </button>
                        </div>
                    </section>
                )}

                
                <div className="command-toolbar">
                    <div>
                        <span className="eyebrow">SPRINT ATUAL</span>
                        <h2>{selectedSprint.name}</h2>
                    </div>

                    <div className="command-actions">
                        <label className="command-search">
                            <Search size={16} />
                            <input
                                value={search}
                                onChange={(event) =>
                                    setSearch(event.target.value)
                                }
                                placeholder="Buscar requisito"
                            />
                        </label>

                        <button
                            className="command-button command-button-primary"
                            onClick={openNewRequirement}
                        >
                            <Plus size={16} />
                            Novo requisito
                        </button>
                    </div>
                </div>

                <div className="command-metrics">
                    <div>
                        <strong>{metrics.total}</strong>
                        <span>itens no módulo</span>
                    </div>
                    <div>
                        <strong>{metrics.doing}</strong>
                        <span>em execução</span>
                    </div>
                    <div>
                        <strong>{metrics.done}</strong>
                        <span>concluídos</span>
                    </div>
                    <div className="metric-note">
                        <span className="status-dot" />
                        Planejamento local; execução real na fila abaixo
                    </div>
                </div>

                {notice && (
                    <div className="command-notice">
                        {notice}
                        <button
                            aria-label="Fechar aviso"
                            onClick={() => setNotice("")}
                        >
                            <X size={15} />
                        </button>
                    </div>
                )}

                {showRequirementForm && (
                    <section className="requirement-form">
                        <div className="form-heading">
                            <div>
                                <span className="eyebrow">
                                    {editingRequirementId
                                        ? "EDITAR REQUISITO"
                                        : "NOVO REQUISITO"}
                                </span>
                                <h3>{selectedSprint.name}</h3>
                            </div>

                            <button
                                className="icon-button"
                                aria-label="Fechar formulário de requisito"
                                onClick={resetRequirementForm}
                            >
                                <X size={17} />
                            </button>
                        </div>

                        <div className="form-grid">
                            <label>
                                Título
                                <input
                                    value={form.title}
                                    onChange={(event) =>
                                        setForm({
                                            ...form,
                                            title: event.target.value
                                        })
                                    }
                                />
                            </label>

                            <label>
                                Tipo
                                <select
                                    value={form.type}
                                    onChange={(event) =>
                                        setForm({
                                            ...form,
                                            type: event.target.value as RequirementType
                                        })
                                    }
                                >
                                    <option>Requisito</option>
                                    <option>Correção</option>
                                </select>
                            </label>

                            <label className="form-wide">
                                Descrição
                                <textarea
                                    value={form.description}
                                    onChange={(event) =>
                                        setForm({
                                            ...form,
                                            description: event.target.value
                                        })
                                    }
                                />
                            </label>

                            <label>
                                Prioridade
                                <select
                                    value={form.priority}
                                    onChange={(event) =>
                                        setForm({
                                            ...form,
                                            priority: event.target.value as Requirement["priority"]
                                        })
                                    }
                                >
                                    <option>Alta</option>
                                    <option>Média</option>
                                    <option>Baixa</option>
                                </select>
                            </label>

                            <label>
                                Critérios de aceite
                                <textarea
                                    value={form.acceptance}
                                    onChange={(event) =>
                                        setForm({
                                            ...form,
                                            acceptance: event.target.value
                                        })
                                    }
                                />
                            </label>
                        </div>

                        <div className="form-footer">
                            <button
                                className="command-button"
                                onClick={resetRequirementForm}
                            >
                                Cancelar
                            </button>

                            <button
                                className="command-button command-button-primary"
                                onClick={saveRequirement}
                            >
                                {editingRequirementId
                                    ? "Salvar alterações"
                                    : "Adicionar item"}
                            </button>
                        </div>
                    </section>
                )}

                <section
                    className="scrum-board"
                    aria-label="Quadro Scrum"
                >
                    {statusColumns.map((status) => {
                        const items = filteredRequirements.filter(
                            (requirement) => requirement.status === status
                        )

                        return (
                            <div
                                className="scrum-column"
                                key={status}
                            >
                                <div className="column-heading">
                                    <div>
                                        <span
                                            className={`column-marker marker-${status.toLowerCase()}`}
                                        />
                                        <h3>{status}</h3>
                                    </div>
                                    <span className="column-count">
                                        {items.length}
                                    </span>
                                </div>

                                <div className="column-items">
                                    {items.map((requirement) => (
                                        <article
                                            className="requirement-card"
                                            key={requirement.id}
                                        >
                                            <div className="card-meta">
                                                <span>{requirement.id}</span>
                                                <span
                                                    className={`priority priority-${requirement.priority.toLowerCase()}`}
                                                >
                                                    {requirement.priority}
                                                </span>
                                            </div>

                                            <h4>{requirement.title}</h4>
                                            <p>{requirement.description}</p>

                                            <div className="card-footer">
                                                <span className="type-label">
                                                    {requirement.type}
                                                </span>

                                                <select
                                                    value={requirement.status}
                                                    onChange={(event) =>
                                                        updateStatus(
                                                            requirement.id,
                                                            event.target.value as RequirementStatus
                                                        )
                                                    }
                                                >
                                                    {statusColumns.map((option) => (
                                                        <option key={option}>
                                                            {option}
                                                        </option>
                                                    ))}
                                                </select>
                                            </div>

                                            <div
                                                className="command-actions"
                                                style={{ marginTop: 10 }}
                                            >
                                                <button
                                                    className="task-export requirement-send-icon"
                                                    disabled={busy}
                                                    onClick={() => downloadTask(requirement)}
                                                    aria-label="Enviar ao bot"
                                                    title="Enviar ao bot"
                                                >
    <Send className="card-action-prime-icon" />
</button>

                                                <button
                                                    className="icon-button"
                                                    aria-label={`Editar ${requirement.id}`}
                                                    onClick={() =>
                                                        openEditRequirement(requirement)
                                                    }
                                                >
                                                    <Pencil className="card-action-prime-icon" />
                                                </button>

                                                <button
                                                    className="icon-button"
                                                    aria-label={`Excluir ${requirement.id}`}
                                                    onClick={() =>
                                                        deleteRequirement(requirement)
                                                    }
                                                >
                                                    <Trash className="card-action-prime-icon" />
                                                </button>
                                            </div>
                                        </article>
                                    ))}

                                    {items.length === 0 && (
                                        <div className="column-empty">
                                            Nenhum item nesta etapa.
                                        </div>
                                    )}
                                </div>
                            </div>
                        )
                    })}
                </section>

                <section
                    className="requirement-form"
                    aria-label="Fila real do bot"
                >
                    <h3>Fila do bot | {selectedSprint.name}</h3>

                    <p>
                        Somente tarefas pendentes podem ser retiradas e devolvidas ao backlog.
                    </p>

                    <div
                        style={{
                            width: "100%",
                            overflowX: "auto",
                            border: "1px solid var(--surface-border, #d8dee9)",
                            borderRadius: "0.5rem"
                        }}
                    >
                        <table
                            style={{
                                width: "100%",
                                minWidth: "900px",
                                borderCollapse: "collapse"
                            }}
                        >
                            <thead style={{ background: "#121212", color: "#f5f5f5" }}>
                                <tr>
                                    <th
                                        style={{
                                            padding: "0.85rem 1rem",
                                            textAlign: "left",
                                            whiteSpace: "nowrap"
                                        }}
                                    >
                                        Situação
                                    </th>

                                    <th
                                        style={{
                                            padding: "0.85rem 1rem",
                                            textAlign: "left"
                                        }}
                                    >
                                        Tarefa
                                    </th>

                                    <th
                                        style={{
                                            padding: "0.85rem 1rem",
                                            textAlign: "left"
                                        }}
                                    >
                                        Arquivo
                                    </th>

                                    <th
                                        style={{
                                            width: "90px",
                                            padding: "0.85rem 1rem",
                                            textAlign: "center"
                                        }}
                                    >
                                        Ações
                                    </th>
                                </tr>
                            </thead>

                            <tbody>
                                {paginatedQueue.map((task) => (
                                    <tr key={`${task.state}-${task.name}`} style={{ background: "#181818", color: "#f5f5f5" }}>
                                        <td
                                            style={{
                                                padding: "0.85rem 1rem",
                                                borderTop:
                                                    "1px solid #343434",
                                                whiteSpace: "nowrap"
                                            }}
                                        >
                                            <strong>
                                                {queueLabels[task.state] || task.state}
                                            </strong>
                                        </td>

                                        <td
                                            style={{
                                                padding: "0.85rem 1rem",
                                                borderTop:
                                                    "1px solid #343434"
                                            }}
                                        >
                                            {task.title}
                                        </td>

                                        <td
                                            style={{
                                                padding: "0.85rem 1rem",
                                                borderTop:
                                                    "1px solid #343434"
                                            }}
                                        >
                                            <small style={{ color: "#a3a3a3" }}>{task.name}</small>
                                        </td>

                                        <td
                                            style={{
                                                padding: "0.55rem 1rem",
                                                borderTop:
                                                    "1px solid #343434",
                                                textAlign: "center"
                                            }}
                                        >
                                            {task.state === "pending" ? (
                                                <button
                                                    type="button"
                                                    className="icon-button"
                                                    aria-label="Voltar para backlog"
                                                    title="Voltar para backlog"
                                                    disabled={busy}
                                                    onClick={() => removeQueuedTask(task)}
                                                >
                                                    <ArrowUTurnUpLeft />
                                                </button>
                                            ) : (
                                                <span aria-hidden="true">—</span>
                                            )}
                                        </td>
                                    </tr>
                                ))}

                                {paginatedQueue.length === 0 && (
                                    <tr>
                                        <td
                                            colSpan={4}
                                            style={{
                                                padding: "1.25rem",
                                                textAlign: "center",
                                                borderTop:
                                                    "1px solid #343434"
                                            }}
                                        >
                                            Nenhuma tarefa nesta fila.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>

                    {queue.length > 0 && (
                        <div
                            style={{
                                width: "100%",
                                display: "flex",
                                justifyContent: "center",
                                alignItems: "center",
                                marginTop: "1rem"
                            }}
                        >
                            <Paginator.Root
                                className="comandos-datatable-paginator"
                                page={safeQueuePage}
                                total={queue.length}
                                itemsPerPage={queueRows}
                                onPageChange={(
                                    event: PaginatorRootChangeEvent
                                ) => {
                                    setQueuePage(event.value)
                                }}
                            >
                                <Paginator.Content>
                                    <Paginator.First>
                                        <AngleDoubleLeft />
                                    </Paginator.First>

                                    <Paginator.Prev>
                                        <AngleLeft />
                                    </Paginator.Prev>

                                    <Paginator.Pages>
                                        {({
                                            paginator
                                        }: PaginatorPagesInstance) =>
                                            paginator?.pages.map(
                                                (page, pageIndex) =>
                                                    page.type === "page" ? (
                                                        <Paginator.Page
                                                            key={pageIndex}
                                                            value={page.value}
                                                        />
                                                    ) : (
                                                        <Paginator.Ellipsis
                                                            key={pageIndex}
                                                        >
                                                            <EllipsisH />
                                                        </Paginator.Ellipsis>
                                                    )
                                            )
                                        }
                                    </Paginator.Pages>

                                    <Paginator.Next>
                                        <AngleRight />
                                    </Paginator.Next>

                                    <Paginator.Last>
                                        <AngleDoubleRight />
                                    </Paginator.Last>
                                </Paginator.Content>
                            </Paginator.Root>
                        </div>
                    )}
                </section>
            </div>
        </Layout>
    )
}






