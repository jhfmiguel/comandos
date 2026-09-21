"use client"

import * as React from "react"
import {
    CheckSquare,
    ChevronDown,
    ChevronLeft,
    ChevronRight,
    Download,
    FileJson,
    Filter,
    GripVertical,
    MoreHorizontal,
    Pencil,
    Printer,
    Redo2,
    Search,
    Send,
    Trash2,
    Undo2,
    X
} from "lucide-react"

type BoardStatus = "Backlog" | "Fazendo" | "Concluído"
type BoardPriority = "Alta" | "Média" | "Baixa"
type BoardType = "Requisito" | "Correção"

export type TaskBoardRequirement = {
    id: string
    title: string
    description: string
    module: string
    type: BoardType
    status: BoardStatus
    priority: BoardPriority
    acceptance: string
    createdAt: string
    attachments?: {
        name: string
        path: string
        size: number
        type: string
    }[]

}

type Props = {
    moduleName: string
    requirements: TaskBoardRequirement[]
    busy: boolean
    onStatusChange: (id: string, status: BoardStatus) => void
    onEdit: (requirement: TaskBoardRequirement) => void
    onDelete: (requirement: TaskBoardRequirement) => void
    onSend: (requirement: TaskBoardRequirement) => void | Promise<unknown>
}

type HistoryEntry = {
    id: string
    from: BoardStatus
    to: BoardStatus
}

type GroupMode = "none" | "priority" | "type"
type SortMode = "id" | "priority" | "recent"
type DensityMode = "comfortable" | "compact"

const initialColumns: BoardStatus[] = [
    "Backlog",
    "Fazendo",
    "Concluído"
]

const priorityWeight: Record<BoardPriority, number> = {
    Alta: 3,
    Média: 2,
    Baixa: 1
}

const transitionRules: Record<BoardStatus, BoardStatus[]> = {
    Backlog: ["Backlog", "Fazendo"],
    Fazendo: ["Backlog", "Fazendo", "Concluído"],
    Concluído: ["Fazendo", "Concluído"]
}

const defaultWipLimits: Record<BoardStatus, number> = {
    Backlog: 999,
    Fazendo: 5,
    Concluído: 999
}

const exportDownload = (
    filename: string,
    content: string,
    mime: string
) => {
    const blob = new Blob([content], { type: mime })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement("a")

    anchor.href = url
    anchor.download = filename
    anchor.click()

    URL.revokeObjectURL(url)
}

export function ComandosTaskBoard({
    moduleName,
    requirements,
    busy,
    onStatusChange,
    onEdit,
    onDelete,
    onSend
}: Props) {
    const storageKey = `comandos-taskboard-${moduleName
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")}`

    const [query, setQuery] = React.useState("")
    const [priorityFilter, setPriorityFilter] =
        React.useState<BoardPriority | "Todas">("Todas")
    const [typeFilter, setTypeFilter] =
        React.useState<BoardType | "Todos">("Todos")
    const [groupMode, setGroupMode] =
        React.useState<GroupMode>("none")
    const [sortMode, setSortMode] =
        React.useState<SortMode>("id")
    const [density, setDensity] =
        React.useState<DensityMode>("comfortable")
    const [columns, setColumns] =
        React.useState<BoardStatus[]>(initialColumns)
    const [collapsed, setCollapsed] =
        React.useState<BoardStatus[]>([])
    const [selection, setSelection] =
        React.useState<string[]>([])
    const [history, setHistory] =
        React.useState<HistoryEntry[]>([])
    const [redoStack, setRedoStack] =
        React.useState<HistoryEntry[]>([])
    const [draggedId, setDraggedId] =
        React.useState<string | null>(null)
    const [contextMenu, setContextMenu] = React.useState<{
        x: number
        y: number
        requirement: TaskBoardRequirement
    } | null>(null)
    const [showOptions, setShowOptions] = React.useState(false)

    React.useEffect(() => {
        const timer = window.setTimeout(() => {
            try {
                const raw = window.localStorage.getItem(storageKey)

                if (!raw) return

                const state = JSON.parse(raw) as {
                    columns?: BoardStatus[]
                    collapsed?: BoardStatus[]
                    groupMode?: GroupMode
                    sortMode?: SortMode
                    density?: DensityMode
                }

                if (
                    Array.isArray(state.columns) &&
                    state.columns.length === 3
                ) {
                    setColumns(state.columns)
                }

                if (Array.isArray(state.collapsed)) {
                    setCollapsed(state.collapsed)
                }

                if (state.groupMode) {
                    setGroupMode(state.groupMode)
                }

                if (state.sortMode) {
                    setSortMode(state.sortMode)
                }

                if (state.density) {
                    setDensity(state.density)
                }
            } catch {
                // Estado visual inválido não deve bloquear o board.
            }
        }, 0)

        return () => {
            window.clearTimeout(timer)
        }
    }, [storageKey])

    React.useEffect(() => {
        try {
            window.localStorage.setItem(
                storageKey,
                JSON.stringify({
                    columns,
                    collapsed,
                    groupMode,
                    sortMode,
                    density
                })
            )
        } catch {
            // O board continua funcional sem persistência.
        }
    }, [
        storageKey,
        columns,
        collapsed,
        groupMode,
        sortMode,
        density
    ])

    React.useEffect(() => {
        const close = () => setContextMenu(null)

        window.addEventListener("click", close)
        window.addEventListener("resize", close)

        return () => {
            window.removeEventListener("click", close)
            window.removeEventListener("resize", close)
        }
    }, [])

    const visibleRequirements = React.useMemo(() => {
        const normalizedQuery = query.trim().toLowerCase()

        return requirements
            .filter((requirement) =>
                (
                    !normalizedQuery ||
                    `${requirement.id} ${requirement.title} ${requirement.description} ${requirement.acceptance}`
                        .toLowerCase()
                        .includes(normalizedQuery)
                ) &&
                (
                    priorityFilter === "Todas" ||
                    requirement.priority === priorityFilter
                ) &&
                (
                    typeFilter === "Todos" ||
                    requirement.type === typeFilter
                )
            )
            .sort((a, b) => {
                if (sortMode === "priority") {
                    return (
                        priorityWeight[b.priority] -
                        priorityWeight[a.priority]
                    )
                }

                if (sortMode === "recent") {
                    return b.createdAt.localeCompare(a.createdAt)
                }

                return a.id.localeCompare(b.id)
            })
    }, [
        requirements,
        query,
        priorityFilter,
        typeFilter,
        sortMode
    ])

    const selectedRequirements = React.useMemo(
        () => requirements.filter((item) =>
            selection.includes(item.id)
        ),
        [requirements, selection]
    )

    const canMove = React.useCallback((
        requirement: TaskBoardRequirement,
        target: BoardStatus
    ) => {
        if (!transitionRules[requirement.status].includes(target)) {
            return false
        }

        if (requirement.status === target) {
            return true
        }

        const targetCount = requirements.filter(
            (item) => item.status === target
        ).length

        return targetCount < defaultWipLimits[target]
    }, [requirements])

    const moveRequirement = React.useCallback((
        requirement: TaskBoardRequirement,
        target: BoardStatus,
        recordHistory = true
    ) => {
        if (!canMove(requirement, target)) {
            return
        }

        if (requirement.status === target) {
            return
        }

        if (recordHistory) {
            setHistory((current) => [
                ...current,
                {
                    id: requirement.id,
                    from: requirement.status,
                    to: target
                }
            ])
            setRedoStack([])
        }

        onStatusChange(requirement.id, target)
    }, [canMove, onStatusChange])

    const moveSelection = (target: BoardStatus) => {
        selectedRequirements.forEach((requirement) => {
            if (canMove(requirement, target)) {
                moveRequirement(requirement, target)
            }
        })
    }

    const undo = () => {
        const entry = history.at(-1)

        if (!entry) return

        const requirement = requirements.find(
            (item) => item.id === entry.id
        )

        if (!requirement) return

        onStatusChange(entry.id, entry.from)
        setHistory((current) => current.slice(0, -1))
        setRedoStack((current) => [...current, entry])
    }

    const redo = () => {
        const entry = redoStack.at(-1)

        if (!entry) return

        const requirement = requirements.find(
            (item) => item.id === entry.id
        )

        if (!requirement) return

        onStatusChange(entry.id, entry.to)
        setRedoStack((current) => current.slice(0, -1))
        setHistory((current) => [...current, entry])
    }

    const toggleSelection = (
        id: string,
        additive = false
    ) => {
        setSelection((current) => {
            if (!additive) {
                return current.length === 1 && current[0] === id
                    ? []
                    : [id]
            }

            return current.includes(id)
                ? current.filter((item) => item !== id)
                : [...current, id]
        })
    }

    const reorderColumn = (
        status: BoardStatus,
        direction: -1 | 1
    ) => {
        setColumns((current) => {
            const index = current.indexOf(status)
            const target = index + direction

            if (target < 0 || target >= current.length) {
                return current
            }

            const next = [...current]
            const [item] = next.splice(index, 1)

            next.splice(target, 0, item)

            return next
        })
    }

    const toggleCollapsed = (status: BoardStatus) => {
        setCollapsed((current) =>
            current.includes(status)
                ? current.filter((item) => item !== status)
                : [...current, status]
        )
    }

    const exportJson = () => {
        exportDownload(
            `taskboard-${moduleName}.json`,
            JSON.stringify(visibleRequirements, null, 2),
            "application/json;charset=utf-8"
        )
    }

    const exportCsv = () => {
        const escapeCsv = (value: string) =>
            `"${value.replace(/"/g, '""')}"`

        const rows = [
            [
                "ID",
                "Título",
                "Status",
                "Prioridade",
                "Tipo",
                "Descrição",
                "Criado em"
            ],
            ...visibleRequirements.map((item) => [
                item.id,
                item.title,
                item.status,
                item.priority,
                item.type,
                item.description,
                item.createdAt
            ])
        ]

        exportDownload(
            `taskboard-${moduleName}.csv`,
            rows
                .map((row) => row.map(escapeCsv).join(";"))
                .join("\r\n"),
            "text/csv;charset=utf-8"
        )
    }

    const groupedItems = (
        items: TaskBoardRequirement[]
    ): Array<[string, TaskBoardRequirement[]]> => {
        if (groupMode === "priority") {
            return ["Alta", "Média", "Baixa"].map((group) => [
                group,
                items.filter((item) => item.priority === group)
            ])
        }

        if (groupMode === "type") {
            return ["Requisito", "Correção"].map((group) => [
                group,
                items.filter((item) => item.type === group)
            ])
        }

        return [["", items]]
    }

    const handleCardKeyDown = (
        event: React.KeyboardEvent<HTMLElement>,
        requirement: TaskBoardRequirement
    ) => {
        if (event.key === "Enter") {
            event.preventDefault()
            onEdit(requirement)
            return
        }

        if (event.key === " ") {
            event.preventDefault()
            toggleSelection(
                requirement.id,
                event.ctrlKey || event.metaKey
            )
            return
        }

        if (!event.altKey) return

        const index = columns.indexOf(requirement.status)

        if (event.key === "ArrowLeft" && index > 0) {
            event.preventDefault()
            moveRequirement(
                requirement,
                columns[index - 1]
            )
        }

        if (
            event.key === "ArrowRight" &&
            index < columns.length - 1
        ) {
            event.preventDefault()
            moveRequirement(
                requirement,
                columns[index + 1]
            )
        }
    }

    return (
        <section
            className={`comandos-taskboard density-${density}`}
            aria-label={`Task Board ${moduleName}`}
        >
            <div className="taskboard-toolbar">
                <div className="taskboard-search">
                    <Search size={16} />
                    <input
                        value={query}
                        onChange={(event) => setQuery(event.target.value)}
                        placeholder="Buscar no Task Board"
                        aria-label="Buscar no Task Board"
                    />
                    {query && (
                        <button
                            type="button"
                            onClick={() => setQuery("")}
                            aria-label="Limpar busca"
                            title="Limpar busca"
                        >
                            <X size={15} />
                        </button>
                    )}

</div>

                <select
                    value={priorityFilter}
                    onChange={(event) =>
                        setPriorityFilter(
                            event.target.value as BoardPriority | "Todas"
                        )
                    }
                    aria-label="Filtrar por prioridade"
                >
                    <option>Todas</option>
                    <option>Alta</option>
                    <option>Média</option>
                    <option>Baixa</option>
                </select>

                <select
                    value={typeFilter}
                    onChange={(event) =>
                        setTypeFilter(
                            event.target.value as BoardType | "Todos"
                        )
                    }
                    aria-label="Filtrar por tipo"
                >
                    <option>Todos</option>
                    <option>Requisito</option>
                    <option>Correção</option>
                </select>

                <div className="taskboard-toolbar-spacer" />

                <button
                    type="button"
                    className="taskboard-icon-button"
                    onClick={undo}
                    disabled={history.length === 0}
                    title="Desfazer"
                    aria-label="Desfazer"
                >
                    <Undo2 size={17} />
                </button>

                <button
                    type="button"
                    className="taskboard-icon-button"
                    onClick={redo}
                    disabled={redoStack.length === 0}
                    title="Refazer"
                    aria-label="Refazer"
                >
                    <Redo2 size={17} />
                </button>

                <div className="taskboard-options">
                    <button
                        type="button"
                        className="taskboard-icon-button"
                        onClick={(event) => {
                            event.stopPropagation()
                            setShowOptions((current) => !current)
                        }}
                        title="Opções do Task Board"
                        aria-label="Opções do Task Board"
                    >
                        <Filter size={17} />
                    </button>

                    {showOptions && (
                        <div
                            className="taskboard-options-menu"
                            onClick={(event) => event.stopPropagation()}
                        >
                            <label>
                                Agrupar
                                <select
                                    value={groupMode}
                                    onChange={(event) =>
                                        setGroupMode(
                                            event.target.value as GroupMode
                                        )
                                    }
                                >
                                    <option value="none">Sem agrupamento</option>
                                    <option value="priority">Prioridade</option>
                                    <option value="type">Tipo</option>
                                </select>
                            </label>

                            <label>
                                Ordenar
                                <select
                                    value={sortMode}
                                    onChange={(event) =>
                                        setSortMode(
                                            event.target.value as SortMode
                                        )
                                    }
                                >
                                    <option value="id">ID</option>
                                    <option value="priority">Prioridade</option>
                                    <option value="recent">Mais recentes</option>
                                </select>
                            </label>

                            <label>
                                Densidade
                                <select
                                    value={density}
                                    onChange={(event) =>
                                        setDensity(
                                            event.target.value as DensityMode
                                        )
                                    }
                                >
                                    <option value="comfortable">
                                        Confortável
                                    </option>
                                    <option value="compact">
                                        Compacta
                                    </option>
                                </select>
                            </label>

                            <button type="button" onClick={exportJson}>
                                <FileJson size={15} />
                                Exportar JSON
                            </button>

                            <button type="button" onClick={exportCsv}>
                                <Download size={15} />
                                Exportar CSV
                            </button>

                            <button
                                type="button"
                                onClick={() => window.print()}
                            >
                                <Printer size={15} />
                                Imprimir
                            </button>
                        </div>
                    )}
                </div>

                
            </div>

            {selection.length > 0 && (
                <div className="taskboard-bulkbar">
                    <CheckSquare size={16} />
                    <strong>{selection.length}</strong>
                    <span>selecionado(s)</span>

                    <div className="taskboard-toolbar-spacer" />

                    {initialColumns.map((status) => (
                        <button
                            type="button"
                            key={status}
                            onClick={() => moveSelection(status)}
                        >
                            Mover para {status}
                        </button>
                    ))}

                    <button
                        type="button"
                        onClick={() => setSelection([])}
                    >
                        Limpar seleção
                    </button>
                </div>
            )}

            <div className="taskboard-columns">
                {columns.map((status, columnIndex) => {
                    const items = visibleRequirements.filter(
                        (requirement) =>
                            requirement.status === status
                    )
                    const isCollapsed = collapsed.includes(status)
                    const wipLimit = defaultWipLimits[status]
                    const isAtLimit = items.length >= wipLimit

                    return (
                        <section
                            key={status}
                            className={[
                                "taskboard-column",
                                isCollapsed ? "is-collapsed" : "",
                                isAtLimit && wipLimit < 999
                                    ? "is-at-limit"
                                    : ""
                            ].filter(Boolean).join(" ")}
                            onDragOver={(event) => {
                                event.preventDefault()
                            }}
                            onDrop={() => {
                                if (!draggedId) return

                                const requirement = requirements.find(
                                    (item) => item.id === draggedId
                                )

                                if (requirement) {
                                    moveRequirement(requirement, status)
                                }

                                setDraggedId(null)
                            }}
                        >
                            <header className="taskboard-column-header">
                                <button
                                    type="button"
                                    className="taskboard-column-collapse"
                                    onClick={() => toggleCollapsed(status)}
                                    aria-label={
                                        isCollapsed
                                            ? `Expandir ${status}`
                                            : `Recolher ${status}`
                                    }
                                >
                                    <ChevronDown
                                        size={16}
                                        className={
                                            isCollapsed
                                                ? "is-rotated"
                                                : ""
                                        }
                                    />
                                </button>

                                <div>
                                    <strong>{status}</strong>
                                    <small>
                                        {items.length}
                                        {wipLimit < 999
                                            ? ` / ${wipLimit} WIP`
                                            : " itens"}
                                    </small>
                                </div>

                                <div className="taskboard-column-actions">
                                    <button
                                        type="button"
                                        disabled={columnIndex === 0}
                                        onClick={() =>
                                            reorderColumn(status, -1)
                                        }
                                        aria-label={`Mover coluna ${status} para esquerda`}
                                        title="Mover coluna para esquerda"
                                    >
                                        <ChevronLeft size={15} />
                                    </button>

                                    <button
                                        type="button"
                                        disabled={
                                            columnIndex ===
                                            columns.length - 1
                                        }
                                        onClick={() =>
                                            reorderColumn(status, 1)
                                        }
                                        aria-label={`Mover coluna ${status} para direita`}
                                        title="Mover coluna para direita"
                                    >
                                        <ChevronRight size={15} />
                                    </button>
                                </div>
                            </header>

                            {!isCollapsed && (
                                <div className="taskboard-column-content">
                                    {groupedItems(items).map(
                                        ([group, groupItems]) => (
                                            <div
                                                className="taskboard-group"
                                                key={`${status}-${group || "all"}`}
                                            >
                                                {group && (
                                                    <div className="taskboard-group-title">
                                                        {group}
                                                        <span>
                                                            {groupItems.length}
                                                        </span>
                                                    </div>
                                                )}

                                                {groupItems.map(
                                                    (requirement) => {
                                                        const isSelected =
                                                            selection.includes(
                                                                requirement.id
                                                            )

                                                        return (
                                                            <article
                                                                key={requirement.id}
                                                                className={[
                                                                    "taskboard-card",
                                                                    isSelected
                                                                        ? "is-selected"
                                                                        : ""
                                                                ].filter(Boolean).join(" ")}
                                                                draggable
                                                                tabIndex={0}
                                                                onDragStart={() =>
                                                                    setDraggedId(
                                                                        requirement.id
                                                                    )
                                                                }
                                                                onDragEnd={() =>
                                                                    setDraggedId(null)
                                                                }
                                                                onClick={(event) =>
                                                                    toggleSelection(
                                                                        requirement.id,
                                                                        event.ctrlKey ||
                                                                        event.metaKey
                                                                    )
                                                                }
                                                                onDoubleClick={() =>
                                                                    onEdit(
                                                                        requirement
                                                                    )
                                                                }
                                                                onKeyDown={(event) =>
                                                                    handleCardKeyDown(
                                                                        event,
                                                                        requirement
                                                                    )
                                                                }
                                                                onContextMenu={(event) => {
                                                                    event.preventDefault()

                                                                    setContextMenu({
                                                                        x: event.clientX,
                                                                        y: event.clientY,
                                                                        requirement
                                                                    })
                                                                }}
                                                            >
                                                                <div className="taskboard-card-top">
                                                                    <GripVertical
                                                                        size={16}
                                                                        className="taskboard-drag-handle"
                                                                    />

                                                                    <span className="taskboard-card-id">
                                                                        {requirement.id}
                                                                    </span>

                                                                    <span
                                                                        className={`taskboard-priority priority-${requirement.priority.toLowerCase()}`}
                                                                    >
                                                                        {requirement.priority}
                                                                    </span>

                                                                    <button
                                                                        type="button"
                                                                        className="taskboard-more"
                                                                        onClick={(event) => {
                                                                            event.stopPropagation()

                                                                            const rect =
                                                                                event.currentTarget.getBoundingClientRect()

                                                                            setContextMenu({
                                                                                x: rect.right - 8,
                                                                                y: rect.bottom + 4,
                                                                                requirement
                                                                            })
                                                                        }}
                                                                        aria-label={`Opções de ${requirement.id}`}
                                                                    >
                                                                        <MoreHorizontal size={17} />
                                                                    </button>
                                                                </div>

                                                                <h4>
                                                                    {requirement.title}
                                                                </h4>

                                                                <p>
                                                                    {requirement.description}
                                                                </p>

                                                                <div className="taskboard-card-tags">
                                                                    <span>
                                                                        {requirement.type}
                                                                    </span>
                                                                    <span>
                                                                        {requirement.createdAt}
                                                                    </span>
                                                                </div>
                                                            </article>
                                                        )
                                                    }
                                                )}

                                                {groupItems.length === 0 &&
                                                    groupMode !== "none" && (
                                                        <div className="taskboard-group-empty">
                                                            Sem itens
                                                        </div>
                                                    )}
                                            </div>
                                        )
                                    )}

                                    {items.length === 0 && (
                                        <div className="taskboard-empty">
                                            Nenhum item nesta etapa.
                                        </div>
                                    )}
                                </div>
                            )}
                        </section>
                    )
                })}
            </div>

            <div className="taskboard-help">
                <span>
                    Arraste cartões para mover.
                </span>
                <span>
                    Ctrl/Cmd + clique: multisseleção.
                </span>
                <span>
                    Espaço: selecionar.
                </span>
                <span>
                    Enter: editar.
                </span>
                <span>
                    Alt + ←/→: mover entre colunas.
                </span>
            </div>

            {contextMenu && (
                <div
                    className="taskboard-context-menu"
                    style={{
                        left: contextMenu.x,
                        top: contextMenu.y
                    }}
                    onClick={(event) => event.stopPropagation()}
                >
                    <button
                        type="button"
                        onClick={() => {
                            onEdit(contextMenu.requirement)
                            setContextMenu(null)
                        }}
                    >
                        <Pencil size={15} />
                        Editar
                    </button>

                    <button
                        type="button"
                        onClick={() => {
                            void onSend(contextMenu.requirement)
                            setContextMenu(null)
                        }}
                    >
                        <Send size={15} />
                        Enviar ao bot
                    </button>

                    {initialColumns.map((status) => (
                        <button
                            type="button"
                            key={status}
                            disabled={
                                !canMove(
                                    contextMenu.requirement,
                                    status
                                )
                            }
                            onClick={() => {
                                moveRequirement(
                                    contextMenu.requirement,
                                    status
                                )
                                setContextMenu(null)
                            }}
                        >
                            Mover para {status}
                        </button>
                    ))}

                    <button
                        type="button"
                        className="danger"
                        onClick={() => {
                            onDelete(contextMenu.requirement)
                            setContextMenu(null)
                        }}
                    >
                        <Trash2 size={15} />
                        Excluir
                    </button>
                </div>
            )}

            <style jsx global>{`
                .command-center .comandos-taskboard {
                    --tb-bg: #121212;
                    --tb-surface: #181818;
                    --tb-surface-2: #202020;
                    --tb-border: #343434;
                    --tb-text: #f5f5f5;
                    --tb-muted: #a3a3a3;
                    margin-top: 1rem;
                }

                .command-center .taskboard-toolbar,
                .command-center .taskboard-bulkbar {
                    display: flex;
                    align-items: center;
                    gap: 0.5rem;
                    flex-wrap: wrap;
                    padding: 0.75rem;
                    border: 1px solid var(--tb-border);
                    background: var(--tb-surface);
                }

                .command-center .taskboard-bulkbar {
                    margin-top: 0.65rem;
                    color: var(--tb-text);
                }

                .command-center .taskboard-toolbar-spacer {
                    flex: 1 1 auto;
                }

                .command-center .taskboard-search {
                    display: flex;
                    align-items: center;
                    gap: 0.45rem;
                    min-width: 16rem;
                    padding: 0 0.65rem;
                    border: 1px solid var(--tb-border);
                    border-radius: 0.45rem;
                    background: var(--tb-surface-2);
                    color: var(--tb-muted);
                }

                .command-center .taskboard-search input {
                    width: 100%;
                    min-height: 2.35rem;
                    border: 0 !important;
                    outline: 0 !important;
                    background: transparent !important;
                    color: var(--tb-text) !important;
                }

                .command-center .taskboard-search button,
                .command-center .taskboard-icon-button,
                .command-center .taskboard-column-actions button,
                .command-center .taskboard-column-collapse,
                .command-center .taskboard-more,
                .command-center .taskboard-card-actions button {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    border: 0;
                    background: transparent;
                    color: var(--tb-muted);
                    cursor: pointer;
                }

                .command-center .taskboard-toolbar > select,
                .command-center .taskboard-options-menu select {
                    min-height: 2.35rem;
                    padding: 0 0.55rem;
                    border: 1px solid var(--tb-border);
                    border-radius: 0.45rem;
                    background: var(--tb-surface-2);
                    color: var(--tb-text);
                }

                .command-center .taskboard-icon-button {
                    width: 2.35rem;
                    height: 2.35rem;
                    border: 1px solid var(--tb-border);
                    border-radius: 0.45rem;
                }

                .command-center .taskboard-icon-button:hover:not(:disabled),
                .command-center .taskboard-column-actions button:hover:not(:disabled),
                .command-center .taskboard-column-collapse:hover,
                .command-center .taskboard-more:hover,
                .command-center .taskboard-card-actions button:hover {
                    background: var(--tb-surface-2);
                    color: var(--tb-text);
                }

                .command-center .taskboard-icon-button:disabled,
                .command-center .taskboard-column-actions button:disabled {
                    opacity: 0.35;
                    cursor: not-allowed;
                }

                .command-center .taskboard-add-button,
                .command-center .taskboard-bulkbar button {
                    display: inline-flex;
                    align-items: center;
                    gap: 0.4rem;
                    min-height: 2.35rem;
                    padding: 0 0.75rem;
                    border: 1px solid #ff9900;
                    border-radius: 0.45rem;
                    background: #ff9900;
                    color: #171717;
                    font-weight: 700;
                    cursor: pointer;
                }

                .command-center .taskboard-options {
                    position: relative;
                }

                .command-center .taskboard-options-menu {
                    position: absolute;
                    right: 0;
                    top: calc(100% + 0.4rem);
                    z-index: 60;
                    display: grid;
                    gap: 0.55rem;
                    min-width: 14rem;
                    padding: 0.75rem;
                    border: 1px solid var(--tb-border);
                    border-radius: 0.6rem;
                    background: var(--tb-surface);
                    box-shadow: 0 0.75rem 1.75rem rgba(0,0,0,0.3);
                }

                .command-center .taskboard-options-menu label {
                    display: grid;
                    gap: 0.3rem;
                    color: var(--tb-muted) !important;
                    font-size: 0.75rem;
                }

                .command-center .taskboard-options-menu button {
                    display: flex;
                    align-items: center;
                    gap: 0.45rem;
                    padding: 0.55rem;
                    border: 0;
                    border-radius: 0.4rem;
                    background: transparent;
                    color: var(--tb-text);
                    cursor: pointer;
                }

                .command-center .taskboard-options-menu button:hover {
                    background: var(--tb-surface-2);
                }

                .command-center .taskboard-columns {
                    display: grid;
                    grid-template-columns: repeat(3, minmax(17rem, 1fr));
                    gap: 0.75rem;
                    margin-top: 0.75rem;
                    align-items: start;
                    overflow-x: auto;
                }

                .command-center .taskboard-column {
                    min-width: 0;
                    border: 1px solid var(--tb-border);
                    border-radius: 0.65rem;
                    background: var(--tb-bg);
                    overflow: hidden;
                }

                .command-center .taskboard-column.is-collapsed {
                    min-width: 12rem;
                }

                .command-center .taskboard-column.is-at-limit {
                    border-color: #f59e0b;
                }

                .command-center .taskboard-column-header {
                    display: flex;
                    align-items: center;
                    gap: 0.5rem;
                    padding: 0.75rem;
                    border-bottom: 1px solid var(--tb-border);
                    background: var(--tb-surface);
                    color: var(--tb-text);
                }

                .command-center .taskboard-column-header > div:nth-child(2) {
                    display: grid;
                    gap: 0.15rem;
                    flex: 1 1 auto;
                }

                .command-center .taskboard-column-header small {
                    color: var(--tb-muted);
                }

                .command-center .taskboard-column-actions {
                    display: flex;
                    align-items: center;
                    gap: 0.15rem;
                }

                .command-center .taskboard-column-actions button,
                .command-center .taskboard-column-collapse {
                    width: 1.85rem;
                    height: 1.85rem;
                    border-radius: 0.35rem;
                }

                .command-center .taskboard-column-collapse .is-rotated {
                    transform: rotate(-90deg);
                }

                .command-center .taskboard-column-content {
                    min-height: 7rem;
                    max-height: 62vh;
                    overflow-y: auto;
                    padding: 0.65rem;
                }

                .command-center .taskboard-group + .taskboard-group {
                    margin-top: 0.8rem;
                }

                .command-center .taskboard-group-title {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 0.45rem;
                    padding: 0 0.15rem;
                    color: var(--tb-muted);
                    font-size: 0.72rem;
                    font-weight: 800;
                    text-transform: uppercase;
                    letter-spacing: 0.06em;
                }

                .command-center .taskboard-card {
                    position: relative;
                    padding: 0.8rem;
                    border: 1px solid var(--tb-border);
                    border-radius: 0.55rem;
                    background: var(--tb-surface);
                    color: var(--tb-text);
                    box-shadow: 0 0.15rem 0.45rem rgba(0,0,0,0.16);
                    cursor: grab;
                    outline: none;
                }

                .command-center .taskboard-card + .taskboard-card {
                    margin-top: 0.55rem;
                }

                .command-center .taskboard-card:focus-visible {
                    box-shadow: 0 0 0 2px rgba(255,153,0,0.45);
                }

                .command-center .taskboard-card.is-selected {
                    border-color: #ff9900;
                    box-shadow: 0 0 0 1px #ff9900;
                }

                .command-center .taskboard-card-top {
                    display: flex;
                    align-items: center;
                    gap: 0.45rem;
                }

                .command-center .taskboard-drag-handle {
                    color: #737373;
                    flex: 0 0 auto;
                }

                .command-center .taskboard-card-id {
                    color: var(--tb-muted);
                    font-size: 0.7rem;
                    font-weight: 800;
                }

                .command-center .taskboard-priority {
                    margin-left: auto;
                    padding: 0.17rem 0.42rem;
                    border-radius: 999px;
                    font-size: 0.65rem;
                    font-weight: 800;
                }

                .command-center .taskboard-priority.priority-alta {
                    background: rgba(239,68,68,0.16);
                    color: #f87171;
                }

                .command-center .taskboard-priority.priority-média {
                    background: rgba(245,158,11,0.16);
                    color: #fbbf24;
                }

                .command-center .taskboard-priority.priority-baixa {
                    background: rgba(34,197,94,0.16);
                    color: #4ade80;
                }

                .command-center .taskboard-more {
                    width: 1.75rem;
                    height: 1.75rem;
                    border-radius: 0.35rem;
                }

                .command-center .taskboard-card h4 {
                    margin: 0.65rem 0 0.35rem;
                    color: var(--tb-text) !important;
                    font-size: 0.9rem;
                    line-height: 1.3;
                }

                .command-center .taskboard-card p {
                    margin: 0;
                    color: var(--tb-muted) !important;
                    font-size: 0.76rem;
                    line-height: 1.45;
                }

                .command-center .taskboard-card-tags {
                    display: flex;
                    gap: 0.4rem;
                    flex-wrap: wrap;
                    margin-top: 0.7rem;
                }

                .command-center .taskboard-card-tags span {
                    padding: 0.2rem 0.4rem;
                    border: 1px solid var(--tb-border);
                    border-radius: 999px;
                    color: var(--tb-muted) !important;
                    font-size: 0.64rem;
                }

                .command-center .taskboard-card-actions {
                    display: flex;
                    justify-content: flex-end;
                    gap: 0.3rem;
                    margin-top: 0.7rem;
                    padding-top: 0.6rem;
                    border-top: 1px solid var(--tb-border);
                }

                .command-center .taskboard-card-actions button {
                    width: 2rem;
                    height: 2rem;
                    border-radius: 0.35rem;
                    color: var(--tb-muted);
                }

                .command-center .taskboard-card-actions .send {
                    color: #22c55e;
                }

                .command-center .taskboard-card-actions .delete {
                    color: #ef4444;
                }

                .command-center .taskboard-empty,
                .command-center .taskboard-group-empty {
                    padding: 1rem 0.5rem;
                    color: var(--tb-muted);
                    text-align: center;
                    font-size: 0.75rem;
                }

                .command-center .density-compact .taskboard-card {
                    padding: 0.55rem;
                }

                .command-center .density-compact .taskboard-card p {
                    display: -webkit-box;
                    overflow: hidden;
                    -webkit-box-orient: vertical;
                    -webkit-line-clamp: 2;
                }

                .command-center .taskboard-help {
                    display: flex;
                    gap: 0.8rem 1rem;
                    flex-wrap: wrap;
                    margin-top: 0.65rem;
                    color: var(--tb-muted);
                    font-size: 0.68rem;
                }

                .command-center .taskboard-context-menu {
                    position: fixed;
                    z-index: 99999;
                    display: grid;
                    min-width: 12rem;
                    padding: 0.4rem;
                    border: 1px solid var(--tb-border);
                    border-radius: 0.55rem;
                    background: var(--tb-surface);
                    box-shadow: 0 0.8rem 2rem rgba(0,0,0,0.35);
                }

                .command-center .taskboard-context-menu button {
                    display: flex;
                    align-items: center;
                    gap: 0.5rem;
                    padding: 0.55rem 0.65rem;
                    border: 0;
                    border-radius: 0.35rem;
                    background: transparent;
                    color: var(--tb-text);
                    text-align: left;
                    cursor: pointer;
                }

                .command-center .taskboard-context-menu button:hover:not(:disabled) {
                    background: var(--tb-surface-2);
                }

                .command-center .taskboard-context-menu button:disabled {
                    opacity: 0.35;
                    cursor: not-allowed;
                }

                .command-center .taskboard-context-menu .danger {
                    color: #ef4444;
                }

                html[data-theme="light"] .command-center .comandos-taskboard {
                    --tb-bg: #f5f7fa;
                    --tb-surface: #ffffff;
                    --tb-surface-2: #f3f4f6;
                    --tb-border: #d9dee5;
                    --tb-text: #1f2937;
                    --tb-muted: #66717d;
                }

                html[data-theme="light"] .command-center .taskboard-toolbar,
                html[data-theme="light"] .command-center .taskboard-bulkbar,
                html[data-theme="light"] .command-center .taskboard-column,
                html[data-theme="light"] .command-center .taskboard-column-header,
                html[data-theme="light"] .command-center .taskboard-card,
                html[data-theme="light"] .command-center .taskboard-options-menu,
                html[data-theme="light"] .command-center .taskboard-context-menu {
                    color: var(--tb-text) !important;
                }

                @media (max-width: 900px) {
                    .command-center .taskboard-columns {
                        grid-template-columns: repeat(3, minmax(16rem, 1fr));
                    }

                    .command-center .taskboard-search {
                        min-width: 100%;
                    }
                }

                @media print {
                    .command-center .taskboard-toolbar,
                    .command-center .taskboard-bulkbar,
                    .command-center .taskboard-help,
                    .command-center .taskboard-card-actions,
                    .command-center .taskboard-column-actions,
                    .command-center .taskboard-column-collapse,
                    .command-center .taskboard-more {
                        display: none !important;
                    }

                    .command-center .taskboard-column-content {
                        max-height: none;
                        overflow: visible;
                    }
                }
            `}</style>
        </section>
    )
}
