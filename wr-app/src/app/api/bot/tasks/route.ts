import { mkdir, readFile, writeFile } from "node:fs/promises"
import { join } from "node:path"
import { localRequest, taskDirectory, workstreams } from "../runtime"

export const runtime = "nodejs"

export async function POST(request: Request) {
    if (!localRequest(request)) return Response.json({ error: "A fila do bot está disponível somente no computador local." }, { status: 403 })
    let body
    try { body = await request.json() } catch { return Response.json({ error: "JSON inválido." }, { status: 400 }) }
    if (!body || !workstreams.has(body.module) || typeof body.id !== "string" || !/^[a-z0-9-]{1,80}$/i.test(body.id)
        || typeof body.markdown !== "string" || body.markdown.length > 30000
        || !["## Objetivo", "## Escopo", "## Critérios de aceite", "## Condição de parada"].every(section => body.markdown.includes(section))) {
        return Response.json({ error: "Tarefa inválida. Informe objetivo, escopo, critérios de aceite e condição de parada." }, { status: 400 })
    }
    const name = `${body.module}-painel-${body.id.toLowerCase()}.md`
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
