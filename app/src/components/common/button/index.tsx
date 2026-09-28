import React from "react"
import {
    Check,
    Clipboard,
    Download,
    Eye,
    Lock,
    Pencil,
    Play,
    Plus,
    Printer,
    RefreshCw,
    RotateCcw,
    Search,
    Unlock,
    Upload,
    X
} from "lucide-react"
import { Trash } from "@primeicons/react"

type ComandosButtonSeverity = "primary" | "secondary" | "success" | "info" | "warn" | "help" | "danger" | "contrast"
type ComandosButtonVariant = "text" | "outlined" | "link" | "contained"
type TableAction =
    | "add"
    | "analyze"
    | "block"
    | "cancel"
    | "copy"
    | "delete"
    | "download"
    | "edit"
    | "execute"
    | "print"
    | "refresh"
    | "return"
    | "success"
    | "unblock"
    | "upload"
    | "view"

interface ButtonProps extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "color"> {
    label?: string
    columnClasses?: string
    severity?: ComandosButtonSeverity
    variant?: ComandosButtonVariant
    iconOnly?: boolean
    fluid?: boolean
    loading?: boolean
}

const normalizeActionText = (value: string): string =>
    value
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim()
        .toLowerCase()

const resolveTableAction = (value: React.ReactNode): TableAction | null => {
    if (typeof value !== "string") return null

    const text = normalizeActionText(value)

    if (/^(edit|editar|alterar)(\b|$)/.test(text)) return "edit"
    if (/^(delete|deletar|excluir|remove|remover)(\b|$)/.test(text)) return "delete"
    if (/^(add|adicionar|incluir|selecionar)(\b|$)/.test(text)) return "add"
    if (/^(view|ver|visualizar|details|detalhes|load|carregar)(\b|$)/.test(text)) return "view"
    if (/^(analyze|analisar)(\b|$)/.test(text)) return "analyze"
    if (/^(execute|executar)(\b|$)/.test(text)) return "execute"
    if (/^(return|retornar|devolver)(\b|$)/.test(text)) return "return"
    if (/^(refresh|atualizar|recarregar)(\b|$)/.test(text)) return "refresh"
    if (/^(block|bloquear)(\b|$)/.test(text)) return "block"
    if (/^(unblock|desbloquear)(\b|$)/.test(text)) return "unblock"
    if (/^(download|baixar|exportar)(\b|$)/.test(text)) return "download"
    if (/^(upload|enviar|importar)(\b|$)/.test(text)) return "upload"
    if (/^(print|imprimir)(\b|$)/.test(text)) return "print"
    if (/^(copy|copiar)(\b|$)/.test(text)) return "copy"
    if (/^(cancel|cancelar|no|nao|reject|rejeitar|recusar)(\b|$)/.test(text)) return "cancel"
    if (/^(save|salvar|yes|sim|approve|aprovar|authorize|autorizar|accept|aceitar|conclude|concluir|confirm|confirmar)(\b|$)/.test(text)) return "success"

    return null
}

const tableActionIcon: Record<TableAction, React.ElementType> = {
    add: Plus,
    analyze: Search,
    block: Lock,
    cancel: X,
    copy: Clipboard,
    delete: Trash,
    download: Download,
    edit: Pencil,
    execute: Play,
    print: Printer,
    refresh: RefreshCw,
    return: RotateCcw,
    success: Check,
    unblock: Unlock,
    upload: Upload,
    view: Eye
}

export const Button: React.FC<ButtonProps> = ({
    label,
    children,
    columnClasses,
    className,
    type = "button",
    disabled,
    severity = "primary",
    variant = "outlined",
    iconOnly = false,
    fluid = false,
    loading = false,
    ...props
}) => {
    const variantClass =
        variant === "text" || variant === "link"
            ? "comandos-button-text"
            : variant === "outlined"
                ? "comandos-button-outlined"
                : "comandos-button-contained"

    const content = label ?? children
    const tableAction = resolveTableAction(content)
    const TableActionIcon = tableAction ? tableActionIcon[tableAction] : null
    const submitLayoutClass = type === "submit" && !iconOnly
        ? "w-full min-h-[3.35rem] justify-center"
        : ""

    return (
        <button
            type={type}
            className={[
                "comandos-action-button",
                variantClass,
                `comandos-button-${severity}`,
                iconOnly ? "comandos-button-icon-only" : "",
                fluid ? "w-full" : "",
                submitLayoutClass,
                columnClasses ?? "",
                className ?? ""
            ].filter(Boolean).join(" ")}
            disabled={disabled || loading}
            aria-busy={loading || undefined}
            data-comandos-table-action={tableAction ?? undefined}
            {...props}
        >
            {loading && <span className="comandos-button-spinner" aria-hidden="true" />}
            {TableActionIcon && (
                <span className="comandos-table-action-icon" aria-hidden="true">
                    <TableActionIcon size={18} aria-hidden="true" />
                </span>
            )}
            <span className={TableActionIcon ? "comandos-button-label" : undefined}>
                {content}
            </span>
        </button>
    )
}
