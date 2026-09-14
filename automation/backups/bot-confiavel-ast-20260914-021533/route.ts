import { spawn } from "node:child_process"
import { mkdir, open } from "node:fs/promises"
import { join } from "node:path"
import {
    localRequest,
    automationRoot,
    runtimeDirectory,
    readStatus,
    readTasks,
    workstreams,
    writeJson
} from "../runtime"

export const runtime = "nodejs"

type ExecutionMode = "continuous" | "until"

type ControlBody = {
    command?: "start" | "pause" | "resume" | "stop"
    workstream?: string
    executionMode?: ExecutionMode
    stopAfterTask?: string
}

export async function POST(request: Request) {
    if (!localRequest(request)) {
        return Response.json(
            {
                error:
                    "O controle do bot está disponível somente " +
                    "no computador local."
            },
            { status: 403 }
        )
    }

    let body: ControlBody

    try {
        body = await request.json() as ControlBody
    } catch {
        return Response.json(
            { error: "JSON inválido." },
            { status: 400 }
        )
    }

    if (
        !body.command ||
        !["start", "pause", "resume", "stop"].includes(body.command)
    ) {
        return Response.json(
            { error: "Comando inválido." },
            { status: 400 }
        )
    }

    const status = await readStatus()

    if (body.command === "start") {
        if (!body.workstream || !workstreams.has(body.workstream)) {
            return Response.json(
                { error: "Módulo inválido." },
                { status: 400 }
            )
        }

        if (status.alive) {
            return Response.json(
                {
                    error:
                        "Já existe uma instância do worker. " +
                        "Pare ou continue essa instância."
                },
                { status: 409 }
            )
        }

        const executionMode: ExecutionMode =
            body.executionMode || "continuous"

        if (!["continuous", "until"].includes(executionMode)) {
            return Response.json(
                { error: "Modo de execução inválido." },
                { status: 400 }
            )
        }

        const tasks = await readTasks()

        const runnableTasks = tasks
            .filter((task) =>
                task.workstream === body.workstream &&
                ["pending", "working"].includes(task.state)
            )
            .sort((a, b) => {
                const sequence = (name: string): number => {
                    const match = name.match(/-(\d+)-/)

                    return match
                        ? Number(match[1])
                        : Number.MIN_SAFE_INTEGER
                }

                const difference =
                    sequence(b.name) - sequence(a.name)

                return difference !== 0
                    ? difference
                    : b.name.localeCompare(a.name)
            })

        if (runnableTasks.length === 0) {
            return Response.json(
                {
                    error:
                        "Não há tarefas na fila deste módulo. " +
                        "Envie um item ao bot."
                },
                { status: 409 }
            )
        }

        if (executionMode === "until") {
            if (!body.stopAfterTask) {
                return Response.json(
                    {
                        error:
                            "Selecione o requisito até o qual " +
                            "o bot deverá trabalhar."
                    },
                    { status: 400 }
                )
            }

            if (
                !runnableTasks.some(
                    (task) => task.name === body.stopAfterTask
                )
            ) {
                return Response.json(
                    {
                        error:
                            "O requisito selecionado não está " +
                            "disponível na fila deste módulo."
                    },
                    { status: 409 }
                )
            }
        }

        if (process.platform !== "win32") {
            return Response.json(
                {
                    error:
                        "Este worker requer Windows e PowerShell."
                },
                { status: 503 }
            )
        }

        await mkdir(runtimeDirectory, { recursive: true })

        const launchLog = await open(
            join(runtimeDirectory, "worker-launch.log"),
            "a"
        )

        try {
            const args = [
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                join(automationRoot, "codex-erp-bot.ps1"),
                "-Workstream",
                body.workstream,
                "-ExecutionMode",
                executionMode,
                "-NoNotification"
            ]

            if (
                executionMode === "until" &&
                body.stopAfterTask
            ) {
                args.push(
                    "-StopAfterTask",
                    body.stopAfterTask
                )
            }

            const child = spawn(
                "powershell.exe",
                args,
                {
                    cwd: join(automationRoot, ".."),
                    windowsHide: true,
                    stdio: [
                        "ignore",
                        launchLog.fd,
                        launchLog.fd
                    ]
                }
            )

            await new Promise<void>((resolve, reject) => {
                child.once("spawn", resolve)
                child.once("error", reject)
            })

            child.unref()

            return Response.json(
                {
                    ok: true,
                    message:
                        executionMode === "continuous"
                            ? "BOT trabalhando sem parar."
                            : "BOT trabalhando até o requisito selecionado."
                },
                { status: 202 }
            )
        } catch {
            return Response.json(
                {
                    error:
                        "Não foi possível iniciar o PowerShell do worker. " +
                        "Consulte runtime/worker-launch.log."
                },
                { status: 503 }
            )
        } finally {
            await launchLog.close()
        }
    }

    if (!status.alive) {
        return Response.json(
            {
                error:
                    "Worker offline. Inicie o bot antes " +
                    "de enviar comandos."
            },
            { status: 409 }
        )
    }

    if (
        body.command === "resume" &&
        !["paused", "pause-requested"].includes(status.state)
    ) {
        return Response.json(
            { error: "O worker não está pausado." },
            { status: 409 }
        )
    }

    await writeJson(
        "control.json",
        {
            command: body.command,
            requestedAt: new Date().toISOString()
        }
    )

    return Response.json(
        {
            ok: true,
            message:
                body.command === "pause"
                    ? "Pausa solicitada para depois da etapa atual."
                    : body.command === "stop"
                        ? "Parada solicitada. Aguarde a confirmação do worker."
                        : "Continuação solicitada ao worker."
        },
        { status: 202 }
    )
}
