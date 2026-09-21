import {
    mkdir,
    unlink,
    writeFile
} from "node:fs/promises"
import {
    basename,
    join,
    relative,
    resolve,
    sep
} from "node:path"

import {
    localRequest,
    taskDirectory
} from "../runtime"

export const runtime = "nodejs"

const MAX_FILE_SIZE = 500 * 1024 * 1024
const MAX_FILES = 10

function safeSegment(value: string) {
    return value
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase()
        .replace(/[^a-z0-9._-]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .slice(0, 120)
}

function attachmentsRoot() {
    return resolve(taskDirectory, "..", "attachments")
}

function attachmentDirectory(requirementId: string) {
    const safeId = safeSegment(requirementId)

    if (!safeId) {
        throw new Error("Identificador do requisito inválido.")
    }

    return join(attachmentsRoot(), safeId)
}

function projectRelativePath(absolutePath: string) {
    const automationRoot = resolve(taskDirectory, "..")
    const projectRoot = resolve(automationRoot, "..")

    return relative(projectRoot, absolutePath)
        .split(sep)
        .join("/")
}

export async function POST(request: Request) {
    if (!localRequest(request)) {
        return Response.json(
            {
                error:
                    "Os anexos estão disponíveis somente no computador local."
            },
            { status: 403 }
        )
    }

    try {
        const formData = await request.formData()
        const requirementId = formData.get("requirementId")

        if (
            typeof requirementId !== "string" ||
            !requirementId.trim()
        ) {
            return Response.json(
                {
                    error:
                        "Identificador do requisito inválido."
                },
                { status: 400 }
            )
        }

        const files = formData
            .getAll("files")
            .filter(
                (item): item is File =>
                    item instanceof File
            )

        if (files.length === 0) {
            return Response.json({
                ok: true,
                attachments: []
            })
        }

        if (files.length > MAX_FILES) {
            return Response.json(
                {
                    error:
                        `Máximo de ${MAX_FILES} anexos por envio.`
                },
                { status: 400 }
            )
        }

        const directory =
            attachmentDirectory(requirementId)

        await mkdir(directory, {
            recursive: true
        })

        const attachments = []

        for (const file of files) {
            if (file.size > MAX_FILE_SIZE) {
                return Response.json(
                    {
                        error:
                            `O arquivo "${file.name}" ultrapassa 500 MB.`
                    },
                    { status: 400 }
                )
            }

            const originalName =
                basename(file.name)

            const safeName =
                safeSegment(originalName) ||
                "anexo"

            const extensionIndex =
                safeName.lastIndexOf(".")

            const extension =
                extensionIndex > 0
                    ? safeName.slice(extensionIndex)
                    : ""

            const baseName =
                extensionIndex > 0
                    ? safeName.slice(0, extensionIndex)
                    : safeName

            const uniqueName =
                `${Date.now()}-${crypto.randomUUID()}-${baseName}${extension}`

            const absolutePath =
                join(directory, uniqueName)

            const buffer =
                Buffer.from(
                    await file.arrayBuffer()
                )

            await writeFile(
                absolutePath,
                buffer,
                { flag: "wx" }
            )

            attachments.push({
                name: originalName,
                path:
                    projectRelativePath(
                        absolutePath
                    ),
                size: file.size,
                type:
                    file.type ||
                    "application/octet-stream"
            })
        }

        return Response.json(
            {
                ok: true,
                attachments
            },
            { status: 201 }
        )
    } catch (error) {
        console.error(error)

        return Response.json(
            {
                error:
                    "Não foi possível salvar os anexos."
            },
            { status: 500 }
        )
    }
}

export async function DELETE(request: Request) {
    if (!localRequest(request)) {
        return Response.json(
            {
                error:
                    "Os anexos estão disponíveis somente no computador local."
            },
            { status: 403 }
        )
    }

    try {
        const body = await request.json()

        if (
            !body ||
            typeof body.requirementId !== "string" ||
            !Array.isArray(body.paths)
        ) {
            return Response.json(
                {
                    error:
                        "Solicitação inválida."
                },
                { status: 400 }
            )
        }

        const directory =
            attachmentDirectory(
                body.requirementId
            )

        const allowedRoot =
            resolve(directory) + sep

        for (const value of body.paths) {
            if (typeof value !== "string") {
                continue
            }

            const filename =
                basename(value)

            const absolutePath =
                resolve(directory, filename)

            if (
                !absolutePath.startsWith(
                    allowedRoot
                )
            ) {
                continue
            }

            await unlink(
                absolutePath
            ).catch(
                (
                    error:
                        NodeJS.ErrnoException
                ) => {
                    if (
                        error.code !==
                        "ENOENT"
                    ) {
                        throw error
                    }
                }
            )
        }

        return Response.json({
            ok: true
        })
    } catch (error) {
        console.error(error)

        return Response.json(
            {
                error:
                    "Não foi possível remover os anexos."
            },
            { status: 500 }
        )
    }
}