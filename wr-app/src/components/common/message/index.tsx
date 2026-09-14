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

function resolveSeverity(type: string): ComandosToastSeverity {
    const normalized = type.trim().toLowerCase()

    if (normalized === "danger" || normalized === "error") {
        return "error"
    }

    if (normalized === "warning" || normalized === "warn") {
        return "warn"
    }

    if (normalized === "success") {
        return "success"
    }

    if (normalized === "secondary") {
        return "secondary"
    }

    if (normalized === "contrast") {
        return "contrast"
    }

    return "info"
}

export const Message: React.FC<MessageProps> = ({
    type,
    field,
    text,
    onClose
}) => {
    React.useEffect(() => {
        const severity = resolveSeverity(type)

        const options = {
            title: field || undefined,
            description: text,
            onDismiss: (_toastItem: ToastType) => {
                onClose?.()
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
    }, [field, onClose, text, type])

    // Compatibility bridge only.
    // No inline Message element remains in the DOM.
    return null
}

export { COMANDOS_TOAST_GROUP }
