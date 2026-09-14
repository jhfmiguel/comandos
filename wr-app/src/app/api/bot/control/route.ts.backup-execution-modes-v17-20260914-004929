import { spawn } from "node:child_process"
import { mkdir, open } from "node:fs/promises"
import { join } from "node:path"
import { localRequest, automationRoot, runtimeDirectory, readStatus, readTasks, workstreams, writeJson } from "../runtime"

export const runtime = "nodejs"

export async function POST(request: Request) {
    if (!localRequest(request)) return Response.json({ error: "O controle do bot está disponível somente no computador local." }, { status: 403 })
    let body
    try { body = await request.json() } catch { return Response.json({ error: "JSON inválido." }, { status: 400 }) }
    if (!body || !["start", "pause", "resume", "stop"].includes(body.command)) return Response.json({ error: "Comando inválido." }, { status: 400 })
    const status = await readStatus()
    if (body.command === "start") {
        if (!workstreams.has(body.workstream)) return Response.json({ error: "Módulo inválido." }, { status: 400 })
        if (status.alive) return Response.json({ error: "Já existe uma instância do worker. Pare ou continue essa instância." }, { status: 409 })
        if (!(await readTasks()).some(task => task.workstream === body.workstream && ["pending", "working"].includes(task.state))) {
            return Response.json({ error: "Não há tarefas na fila deste módulo. Envie um item ao bot." }, { status: 409 })
        }
        if (process.platform !== "win32") return Response.json({ error: "Este worker requer Windows e PowerShell." }, { status: 503 })
        await mkdir(runtimeDirectory, { recursive: true })
        const launchLog = await open(join(runtimeDirectory, "worker-launch.log"), "a")
        try {
            const child = spawn("powershell.exe", ["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", join(automationRoot, "codex-erp-bot.ps1"), "-Workstream", body.workstream, "-MaxTasks", "1", "-NoNotification"], {
                // Windows PowerShell can exit without running -File under DETACHED_PROCESS.
                // Hidden window + unref keeps the worker in the background without that flag.
                cwd: join(automationRoot, ".."), windowsHide: true, stdio: ["ignore", launchLog.fd, launchLog.fd],
            })
            await new Promise<void>((resolve, reject) => { child.once("spawn", resolve); child.once("error", reject) })
            child.unref()
            return Response.json({ ok: true, message: "Inicialização solicitada. Acompanhe a confirmação no status do worker." }, { status: 202 })
        } catch { return Response.json({ error: "Não foi possível iniciar o PowerShell do worker. Consulte runtime/worker-launch.log." }, { status: 503 }) }
        finally { await launchLog.close() }
    }
    if (!status.alive) return Response.json({ error: "Worker offline. Inicie uma etapa antes de enviar comandos." }, { status: 409 })
    if (body.command === "resume" && !["paused", "pause-requested"].includes(status.state)) return Response.json({ error: "O worker não está pausado." }, { status: 409 })
    await writeJson("control.json", { command: body.command, requestedAt: new Date().toISOString() })
    return Response.json({ ok: true, message: body.command === "pause" ? "Pausa solicitada para depois da etapa atual." : body.command === "stop" ? "Parada solicitada. Aguarde a confirmação do worker." : "Continuação solicitada ao worker." }, { status: 202 })
}
