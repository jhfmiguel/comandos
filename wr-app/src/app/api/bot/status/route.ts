import { localRequest, readStatus, readTasks } from "../runtime"

export const dynamic = "force-dynamic"
export const runtime = "nodejs"

export async function GET(request: Request) {
    if (!localRequest(request)) return Response.json({ error: "O controle do bot está disponível somente no computador local." }, { status: 403 })
    return Response.json({ ...await readStatus(), tasks: await readTasks() })
}
