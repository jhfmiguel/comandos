import { mkdir, readFile, readdir, rename, writeFile } from "node:fs/promises"
import { join, resolve } from "node:path"
import { randomUUID } from "node:crypto"

export const automationRoot = process.env.COMANDOS_AUTOMATION_ROOT
    ? resolve(process.env.COMANDOS_AUTOMATION_ROOT)
    : resolve(process.cwd(), "..", "automation")
export const runtimeDirectory = join(automationRoot, "runtime")
export const taskDirectory = join(automationRoot, "tasks")
export const workstreams = new Set(["armamento", "municao", "transporte", "inventario", "custodia", "auditoria"])

export function localRequest(request: Request) {
    const url = new URL(request.url)
    if (!["localhost", "127.0.0.1", "[::1]"].includes(url.hostname)) return false
    const origin = request.headers.get("origin")
    return !origin || origin === url.origin
}

export async function writeJson(name: string, data: unknown) {
    await mkdir(runtimeDirectory, { recursive: true })
    const temporary = join(runtimeDirectory, `${name}.${randomUUID()}.tmp`)
    await writeFile(temporary, JSON.stringify(data), "utf8")
    await rename(temporary, join(runtimeDirectory, name))
}

export async function readStatus() {
    const offline = { state: "offline", message: "Worker não iniciado. Inicie a próxima etapa pelo painel.", taskName: "", processedTasks: 0, updatedAt: null, alive: false }
    try {
        const status = JSON.parse((await readFile(join(runtimeDirectory, "status.json"), "utf8")).replace(/^\uFEFF/, ""))
        let alive = false
        if (Number.isInteger(status.pid) && status.pid > 0) {
            try { process.kill(status.pid, 0); alive = true } catch (error) {
                alive = (error as NodeJS.ErrnoException).code === "EPERM"
            }
        }
        const age = Date.now() - Date.parse(status.updatedAt)
        const terminal = ["completed", "stopped", "failed"].includes(status.state)
        if (!alive && !terminal) return { ...status, state: "offline", alive, message: "O processo do worker terminou. A fila foi preservada." }
        if (alive && !terminal && (!Number.isFinite(age) || age > 90000)) {
            return { ...status, state: "unresponsive", alive, message: "Worker sem atualização há mais de 90 segundos. Pare a instância antes de reiniciar." }
        }
        return { ...status, alive }
    } catch { return offline }
}

export async function readTasks() {
    const tasks = []
    for (const [folder, state] of [["", "pending"], ["working", "working"], ["completed", "completed"], ["failed", "failed"]]) {
        let files: string[]
        try { files = await readdir(join(taskDirectory, folder)) } catch { continue }
        for (const name of files.filter(name => /^[a-z0-9][a-z0-9-]*\.md$/i.test(name)).sort()) {
            const content = await readFile(join(taskDirectory, folder, name), "utf8").catch(() => "")
            tasks.push({ name, state, workstream: name.split("-")[0], title: content.replace(/^\uFEFF/, "").match(/^#\s+(.+)/m)?.[1] ?? name })
        }
    }
    return tasks
}
