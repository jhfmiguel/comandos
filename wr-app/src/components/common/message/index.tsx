"use client"

import * as React from "react"

import type { ToastType } from "@primereact/ui/toaster"

import {
    COMANDOS_TOAST_GROUP,
    notify,
    type ComandosToastSeverity
} from "../toast"

interface MessageProps {
    type: string
    field?: string
    text: string
    onClose?: () => void
}

export interface Alert {
    type: string
    field?: string
    text: string
}

const recentMessages = new Map<string, number>()
const DEDUPLICATION_WINDOW_MS = 1000

function resolveSeverity(type: string): ComandosToastSeverity {
    const normalized = type.trim().toLowerCase()

    if (normalized === "danger" || normalized === "error") return "error"
    if (normalized === "warning" || normalized === "warn") return "warn"
    if (normalized === "success") return "success"
    if (normalized === "secondary") return "secondary"
    if (normalized === "contrast") return "contrast"

    return "info"
}

function shouldEmit(key: string): boolean {
    const now = Date.now()
    const previous = recentMessages.get(key)

    if (previous !== undefined && now - previous < DEDUPLICATION_WINDOW_MS) {
        return false
    }

    recentMessages.set(key, now)

    for (const [messageKey, timestamp] of recentMessages) {
        if (now - timestamp >= DEDUPLICATION_WINDOW_MS) {
            recentMessages.delete(messageKey)
        }
    }

    return true
}

export const Message: React.FC<MessageProps> = ({
    type,
    field,
    text,
    onClose
}) => {
    const onCloseRef = React.useRef(onClose)

    React.useEffect(() => {
        onCloseRef.current = onClose
    }, [onClose])

    React.useEffect(() => {
        const severity = resolveSeverity(type)
        const deduplicationKey = `${severity}|${field ?? ""}|${text}`

        // React StrictMode can mount, clean up and mount again in development.
        // A module-level guard prevents two identical Message instances/mounts
        // from producing two simultaneous toasts.
        if (!shouldEmit(deduplicationKey)) return

        const options = {
            title: field || undefined,
            description: text,
            onDismiss: (_toastItem: ToastType) => {
                onCloseRef.current?.()
            }
        }

        switch (severity) {
            case "success":
                notify.success(options)
                break
            case "warn":
                notify.warn(options)
                break
            case "error":
                notify.error(options)
                break
            case "secondary":
                notify.secondary(options)
                break
            case "contrast":
                notify.contrast(options)
                break
            default:
                notify.info(options)
                break
        }
    }, [field, text, type])

    return null
}

export { COMANDOS_TOAST_GROUP }