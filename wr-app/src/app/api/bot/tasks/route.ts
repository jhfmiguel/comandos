import { mkdir, readFile, writeFile, unlink } from "node:fs/promises"
import { basename, join } from "node:path"
import { localRequest, taskDirectory, workstreams } from "../runtime"

export const runtime = "nodejs"

export async function POST(request: Request) {
    if (!localRequest(request)) return Response.json({ error: "A fila do bot está disponível somente no computador local." }, { status: 403 })
    let body
    try { body = await request.json() } catch { return Response.json({ error: "JSON inválido." }, { status: 400 }) }
    if (!body || !workstreams.has(body.module) || typeof body.id !== "string" || body.id.trim().length < 1 || body.id.trim().length > 120
        || typeof body.markdown !== "string" || body.markdown.length > 30000
        || !["## Objetivo", "## Escopo", "## Critérios de aceite", "## Condição de parada"].every(section => body.markdown.includes(section))) {
        return Response.json({ error: "Tarefa inválida. Informe objetivo, escopo, critérios de aceite e condição de parada." }, { status: 400 })
    }
    const safeId = body.id
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .slice(0, 100)

    if (!safeId) {
        return Response.json({ error: "Identificador da tarefa invalido." }, { status: 400 })
    }
    const number = body.id.match(/-(\d{3})$/)?.[1]

    if (!number) {
        return Response.json(
            { error: "Identificador da tarefa deve seguir o padrão ARM-001." },
            { status: 400 }
        )
    }

    const titleMatch = body.markdown.match(/^#\s+.+?\s+-\s+(.+)$/m)
    const title = titleMatch?.[1] ?? "tarefa"

    const titleSlug = title
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .slice(0, 80)

    const name = `${body.module}-${number}-${titleSlug || "tarefa"}.md`
    await mkdir(taskDirectory, { recursive: true })
    for (const folder of ["", "working", "completed", "failed"]) {
        try {
            const existing = await readFile(join(taskDirectory, folder, name), "utf8")
            if (existing === body.markdown) return Response.json({ ok: true, taskName: name, message: "Esta tarefa já está registrada na fila." })
            return Response.json({ error: "Já existe uma tarefa com este identificador e conteúdo diferente." }, { status: 409 })
        } catch (error) { if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error }
    }
    try { await writeFile(join(taskDirectory, name), body.markdown, { encoding: "utf8", flag: "wx" }) }
    catch (error) {
        if ((error as NodeJS.ErrnoException).code === "EEXIST") return Response.json({ error: "Tarefa já enviada. Atualize a fila." }, { status: 409 })
        throw error
    }
    return Response.json({ ok: true, taskName: name, message: "Tarefa salva na fila do bot. Inicie a próxima etapa para executar." }, { status: 201 })
}



export async function DELETE(request: Request) {
    if (!localRequest(request)) {
        return Response.json(
            {
                error:
                    "A fila do bot está disponível somente no computador local."
            },
            {
                status: 403
            }
        )
    }

    let body: {
        name?: unknown
    }

    try {
        body = await request.json() as {
            name?: unknown
        }
    } catch {
        return Response.json(
            {
                error: "JSON inválido."
            },
            {
                status: 400
            }
        )
    }

    if (
        typeof body.name !== "string" ||
        !body.name.trim()
    ) {
        return Response.json(
            {
                error: "Nome da tarefa inválido."
            },
            {
                status: 400
            }
        )
    }

    const safeName = basename(body.name)

    if (
        safeName !== body.name ||
        !safeName.endsWith(".md") ||
        !/^[a-z0-9._-]+$/i.test(safeName)
    ) {
        return Response.json(
            {
                error: "Nome da tarefa inválido."
            },
            {
                status: 400
            }
        )
    }

    try {
        await unlink(
            join(
                taskDirectory,
                safeName
            )
        )

        return Response.json({
            ok: true,
            taskName: safeName,
            message:
                "Tarefa retirada da fila e devolvida ao backlog."
        })
    } catch (error) {
        const code =
            (error as NodeJS.ErrnoException).code

        if (code === "ENOENT") {
            return Response.json(
                {
                    error:
                        "A tarefa não está mais na fila pendente. Somente tarefas pendentes podem voltar ao backlog."
                },
                {
                    status: 409
                }
            )
        }

        console.error(
            "Failed to remove queued bot task:",
            error
        )

        return Response.json(
            {
                error:
                    "Não foi possível retirar a tarefa da fila."
            },
            {
                status: 500
            }
        )
    }
}

