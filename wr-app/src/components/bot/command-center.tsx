"use client"

import { useEffect, useMemo, useState } from "react"
import { Play, Plus, Search, Send, X } from "lucide-react"

import { Layout } from "components"

type ModuleKey = "armamento" | "municao" | "transporte" | "inventario" | "custodia" | "auditoria"
type RequirementType = "Requisito" | "Correção"
type RequirementStatus = "Backlog" | "Fazendo" | "Concluído"

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

type QueuedTask = { name: string; title: string; workstream: string; state: string }
type BotStatus = { state: string; message: string; taskName: string; workstream?: string; processedTasks: number; updatedAt: string | null; alive?: boolean; tasks?: QueuedTask[] }
const stateLabels: Record<string, string> = { offline: "offline", waiting: "aguardando", working: "trabalhando", validating: "validando", paused: "pausado", "pause-requested": "pausa solicitada", completed: "concluído", stopped: "parado", failed: "falhou", unresponsive: "sem resposta", starting: "iniciando" }
const queueLabels: Record<string, string> = { pending: "Pendente", working: "Em execução", completed: "Concluída", failed: "Falhou" }

const modules: Array<{ key: ModuleKey; name: string; detail: string; accent: string; steps: number }> = [
    { key: "armamento", name: "Armamento", detail: "Cadastro, ciclo de vida e movimentações", accent: "#e85d04", steps: 4 },
    { key: "municao", name: "Munição", detail: "Consumo, lotes e rastreabilidade", accent: "#f48c06", steps: 3 },
    { key: "transporte", name: "Transporte", detail: "Transferências, custódia e logística", accent: "#2a9d8f", steps: 4 },
    { key: "inventario", name: "Inventário", detail: "Estoque, contagem e reconciliação", accent: "#457b9d", steps: 5 },
    { key: "custodia", name: "Custódia", detail: "Responsáveis, unidades e termos", accent: "#6d597a", steps: 3 },
    { key: "auditoria", name: "Auditoria", detail: "Histórico, conformidade e evidências", accent: "#bc6c25", steps: 3 }
]

const statusColumns: RequirementStatus[] = ["Backlog", "Fazendo", "Concluído"]

const moduleByKey = (key: ModuleKey) => modules.find((module) => module.key === key) ?? modules[0]

const makeTaskMarkdown = (requirement: Requirement) => `# ${requirement.id} - ${requirement.title}

## Objetivo

${requirement.description}

## Escopo

Módulo: ${moduleByKey(requirement.module).name}
Tipo: ${requirement.type}
Prioridade: ${requirement.priority}

## Critérios de aceite

- [ ] ${requirement.acceptance}
- [ ] Testes automatizados relevantes passam.
- [ ] Não alterar módulos sem relação direta.

## Condição de parada

Parar quando os critérios de aceite estiverem atendidos. Registrar arquivos alterados, validações executadas e pendências no relatório final.
`

export function CommandCenter() {
    const [requirements, setRequirements] = useState<Requirement[]>([])
    const [loaded, setLoaded] = useState(false)
    const [busy, setBusy] = useState(false)
    const [selectedModule, setSelectedModule] = useState<ModuleKey>("armamento")
    const [search, setSearch] = useState("")
    const [showForm, setShowForm] = useState(false)
    const [notice, setNotice] = useState("")
    const [botStatus, setBotStatus] = useState<BotStatus>({ state: "offline", message: "Worker ainda não iniciou.", taskName: "", processedTasks: 0, updatedAt: null })
    const [form, setForm] = useState({ title: "", description: "", type: "Requisito" as RequirementType, priority: "Média" as Requirement["priority"], acceptance: "" })

    useEffect(() => {
        Promise.resolve().then(() => {
            try {
                const stored = JSON.parse(window.localStorage.getItem("comandos-requirements") || "[]")
                if (!Array.isArray(stored) || !stored.every(item => item && typeof item.id === "string" && typeof item.title === "string" && typeof item.description === "string" && typeof item.acceptance === "string" && modules.some(module => module.key === item.module) && statusColumns.includes(item.status))) throw new Error()
                setRequirements(stored)
            } catch { setNotice("Não foi possível carregar o backlog salvo no navegador. A fila do bot permanece disponível abaixo.") }
            setLoaded(true)
        })
    }, [])

    useEffect(() => {
        if (loaded) Promise.resolve().then(() => {
            try { window.localStorage.setItem("comandos-requirements", JSON.stringify(requirements)) }
            catch { setNotice("O navegador não permitiu salvar o backlog local.") }
        })
    }, [requirements, loaded])

    useEffect(() => {
        const refreshStatus = async () => {
            try {
                const response = await fetch("/api/bot/status", { cache: "no-store" })
                if (!response.ok) throw new Error()
                setBotStatus(await response.json() as BotStatus)
            } catch { setBotStatus((current) => ({ ...current, state: "offline", message: "Não foi possível consultar o worker." })) }
        }
        refreshStatus()
        const timer = window.setInterval(refreshStatus, 2000)
        return () => window.clearInterval(timer)
    }, [])

    const filteredRequirements = useMemo(() => requirements.filter((requirement) => {
        const matchesModule = requirement.module === selectedModule
        const query = search.toLowerCase()
        return matchesModule && (!query || `${requirement.id} ${requirement.title} ${requirement.description}`.toLowerCase().includes(query))
    }), [requirements, search, selectedModule])

    const metrics = useMemo(() => ({
        total: filteredRequirements.length,
        doing: filteredRequirements.filter((requirement) => requirement.status === "Fazendo").length,
        done: filteredRequirements.filter((requirement) => requirement.status === "Concluído").length
    }), [filteredRequirements])

    const updateStatus = (id: string, status: RequirementStatus) => {
        setRequirements((current) => current.map((requirement) => requirement.id === id ? { ...requirement, status } : requirement))
    }

    const addRequirement = () => {
        if (!form.title.trim() || !form.description.trim() || !form.acceptance.trim()) {
            setNotice("Preencha título, descrição e critérios de aceite.")
            return
        }
        const prefix = selectedModule.slice(0, 3).toUpperCase()
        const newRequirement: Requirement = {
            id: `${prefix}-${crypto.randomUUID()}`,
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
        setForm({ title: "", description: "", type: "Requisito", priority: "Média", acceptance: "" })
        setShowForm(false)
        setNotice(`${newRequirement.id} adicionado ao backlog.`)
    }

    const sendRequest = async (path: string, data: object) => {
        setBusy(true)
        try {
            const response = await fetch(path, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(data) })
            const result = await response.json()
            if (!response.ok) throw new Error(result.error || "Não foi possível concluir o comando.")
            setNotice(result.message)
            const status = await fetch("/api/bot/status", { cache: "no-store" })
            if (status.ok) setBotStatus(await status.json())
        } catch (error) { setNotice(error instanceof Error ? error.message : "Não foi possível comunicar com o bot.") }
        finally { setBusy(false) }
    }

    const downloadTask = (requirement: Requirement) => sendRequest("/api/bot/tasks", { id: requirement.id, module: requirement.module, markdown: makeTaskMarkdown(requirement) })
    const sendBotCommand = (command: "start" | "pause" | "resume" | "stop") => sendRequest("/api/bot/control", { command, workstream: selectedModule })
    const queue = (botStatus.tasks || []).filter(task => task.workstream === selectedModule)

    return (
        <Layout title="Command Center">
            <div className="command-center">
                <section className="command-hero">
                    <div>
                        <span className="command-kicker">COMANDOS / ORQUESTRAÇÃO</span>
                        <h1>Desenvolvimento em movimento.</h1>
                        <p>Escolha o módulo, organize o backlog e entregue etapas claras ao Codex.</p>
                    </div>
                    <div className="command-hero-status"><span className={`status-dot status-${botStatus.state}`} /> Bot {stateLabels[botStatus.state] || botStatus.state}<small>{botStatus.taskName || botStatus.message}<br />workstream: {botStatus.workstream || selectedModule}</small></div>
                </section>

                <section className="module-strip" aria-label="Módulos do ERP">
                    {modules.map((module) => (
                        <button key={module.key} className={`module-tile ${selectedModule === module.key ? "is-selected" : ""}`} onClick={() => setSelectedModule(module.key)} style={{ "--module-accent": module.accent } as React.CSSProperties}>
                            <span className="module-index">0{modules.indexOf(module) + 1}</span>
                            <strong>{module.name}</strong>
                            <small>{module.detail}</small>
                            <span className="module-steps">{module.steps} etapas previstas</span>
                        </button>
                    ))}
                </section>

                <div className="command-toolbar">
                    <div><span className="eyebrow">SPRINT ATUAL</span><h2>{moduleByKey(selectedModule).name}</h2></div>
                    <div className="command-actions">
                        <label className="command-search"><Search size={16} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar requisito" /></label>
                        <button className="command-button command-button-primary" onClick={() => setShowForm(true)}><Plus size={16} /> Novo item</button>
                    </div>
                </div>

                <div className="command-metrics"><div><strong>{metrics.total}</strong><span>itens no módulo</span></div><div><strong>{metrics.doing}</strong><span>em execução</span></div><div><strong>{metrics.done}</strong><span>concluídos</span></div><div className="metric-note"><span className="status-dot" /> Planejamento local; execução real na fila abaixo</div></div>

                {notice && <div className="command-notice">{notice}<button aria-label="Fechar aviso" onClick={() => setNotice("")}><X size={15} /></button></div>}

                {showForm && <section className="requirement-form"><div className="form-heading"><div><span className="eyebrow">NOVO ITEM</span><h3>Adicionar ao backlog de {moduleByKey(selectedModule).name}</h3></div><button className="icon-button" aria-label="Fechar formulário" onClick={() => setShowForm(false)}><X size={17} /></button></div><div className="form-grid"><label>Título<input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="Ex.: validar baixa por lote" /></label><label>Tipo<select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as RequirementType })}><option>Requisito</option><option>Correção</option></select></label><label className="form-wide">Descrição<textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="O que precisa ser construído ou corrigido?" /></label><label>Prioridade<select value={form.priority} onChange={(event) => setForm({ ...form, priority: event.target.value as Requirement["priority"] })}><option>Alta</option><option>Média</option><option>Baixa</option></select></label><label>Critérios de aceite<textarea value={form.acceptance} onChange={(event) => setForm({ ...form, acceptance: event.target.value })} placeholder="Como saberemos que terminou?" /></label></div><div className="form-footer"><button className="command-button" onClick={() => setShowForm(false)}>Cancelar</button><button className="command-button command-button-primary" onClick={addRequirement}>Adicionar item</button></div></section>}

                <section className="scrum-board" aria-label="Quadro Scrum">
                    {statusColumns.map((status) => <div className="scrum-column" key={status}><div className="column-heading"><div><span className={`column-marker marker-${status.toLowerCase()}`} /><h3>{status}</h3></div><span className="column-count">{filteredRequirements.filter((requirement) => requirement.status === status).length}</span></div><div className="column-items">{filteredRequirements.filter((requirement) => requirement.status === status).map((requirement) => <article className="requirement-card" key={requirement.id}><div className="card-meta"><span>{requirement.id}</span><span className={`priority priority-${requirement.priority.toLowerCase()}`}>{requirement.priority}</span></div><h4>{requirement.title}</h4><p>{requirement.description}</p><div className="card-footer"><span className="type-label">{requirement.type}</span><select aria-label={`Mover ${requirement.id}`} value={requirement.status} onChange={(event) => updateStatus(requirement.id, event.target.value as RequirementStatus)}>{statusColumns.map((option) => <option key={option}>{option}</option>)}</select></div><button className="task-export" disabled={busy} onClick={() => downloadTask(requirement)}><Send size={14} /> Enviar ao bot</button></article>)}{filteredRequirements.filter((requirement) => requirement.status === status).length === 0 && <div className="column-empty">Nenhum item nesta etapa.</div>}</div></div>)}
                </section>

                <section className="bot-run-panel">
                    <div className="bot-run-icon"><Play size={18} /></div>
                    <div><span className="eyebrow">CONTROLE DO WORKER</span><h3>{botStatus.message}</h3><p>{botStatus.taskName || selectedModule} | Atualizado: {botStatus.updatedAt ? new Date(botStatus.updatedAt).toLocaleTimeString() : "nunca"}</p></div>
                    <div className="bot-controls">
                        <button className="command-button command-button-primary" onClick={() => sendBotCommand("start")} disabled={busy || botStatus.alive || !queue.some(task => ["pending", "working"].includes(task.state))}>Iniciar próxima etapa</button>
                        <button className="command-button" onClick={() => sendBotCommand("pause")} disabled={busy || !botStatus.alive || !["working", "waiting", "validating"].includes(botStatus.state)}>Pausar após etapa</button>
                        <button className="command-button" onClick={() => sendBotCommand("resume")} disabled={busy || !["paused", "pause-requested"].includes(botStatus.state)}>Continuar</button>
                        <button className="command-button command-button-danger" onClick={() => sendBotCommand("stop")} disabled={busy || !botStatus.alive}><X size={15} /> Parar</button>
                    </div>
                </section>
                <section className="requirement-form" aria-label="Fila real do bot">
                    <h3>Fila do bot | {moduleByKey(selectedModule).name}</h3>
                    <p>O início executa uma etapa pendente deste módulo. Os estados abaixo vêm do worker.</p>
                    {queue.length === 0 ? <p>Nenhuma tarefa enviada neste módulo.</p> : <ul>{queue.map(task => <li key={task.name}><strong>{queueLabels[task.state] || task.state}</strong> | {task.title}<br /><small>{task.name}</small></li>)}</ul>}
                </section>
            </div>
        </Layout>
    )
}
