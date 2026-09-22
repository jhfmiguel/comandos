"use client"

import * as React from "react"
import { Check, CircleAlert, Info, X } from "lucide-react"

export const COMANDOS_TOAST_GROUP = "severity"

export type ComandosToastSeverity =
    | "success"
    | "info"
    | "warn"
    | "error"
    | "secondary"
    | "contrast"

export interface ComandosToastItem {
    id: string
    severity: ComandosToastSeverity
    title?: React.ReactNode
    description: React.ReactNode
    duration: number
    onDismiss?: (toastItem: ComandosToastItem) => void
}

export interface ComandosToastOptions {
    title?: React.ReactNode
    description: React.ReactNode
    duration?: number
    onDismiss?: (toastItem: ComandosToastItem) => void
}

type Listener = (toastItem: ComandosToastItem) => void
const listeners = new Set<Listener>()

function emitToast(severity: ComandosToastSeverity, options: ComandosToastOptions) {
    const toastItem: ComandosToastItem = {
        id: crypto.randomUUID(),
        severity,
        title: options.title,
        description: options.description,
        duration: options.duration ?? 5000,
        onDismiss: options.onDismiss
    }

    listeners.forEach((listener) => listener(toastItem))
    return toastItem
}

export const notify = {
    success: (options: ComandosToastOptions) => emitToast("success", options),
    info: (options: ComandosToastOptions) => emitToast("info", options),
    warn: (options: ComandosToastOptions) => emitToast("warn", options),
    error: (options: ComandosToastOptions) => emitToast("error", options),
    secondary: (options: ComandosToastOptions) => emitToast("secondary", options),
    contrast: (options: ComandosToastOptions) => emitToast("contrast", options)
}

function ToastIcon({ severity }: { severity: ComandosToastSeverity }) {
    if (severity === "success") return <Check size={18} aria-hidden="true" />
    if (severity === "error" || severity === "warn") return <CircleAlert size={18} aria-hidden="true" />
    return <Info size={18} aria-hidden="true" />
}

export function ComandosToaster() {
    const [items, setItems] = React.useState<ComandosToastItem[]>([])

    const dismiss = React.useCallback((id: string) => {
        setItems((current) => {
            const item = current.find((candidate) => candidate.id === id)
            if (item) queueMicrotask(() => item.onDismiss?.(item))
            return current.filter((candidate) => candidate.id !== id)
        })
    }, [])

    React.useEffect(() => {
        const listener: Listener = (toastItem) => {
            setItems((current) => [...current.slice(-4), toastItem])

            window.setTimeout(() => {
                dismiss(toastItem.id)
            }, toastItem.duration)
        }

        listeners.add(listener)
        return () => {
            listeners.delete(listener)
        }
    }, [dismiss])

    return (
        <div
            className="comandos-toast-region"
            role="region"
            aria-label="Notifications"
            aria-live="polite"
        >
            {items.map((toastItem) => (
                <div
                    key={toastItem.id}
                    className={`comandos-toast comandos-toast-${toastItem.severity}`}
                    role={toastItem.severity === "error" ? "alert" : "status"}
                >
                    <span className="comandos-toast-icon">
                        <ToastIcon severity={toastItem.severity} />
                    </span>

                    <div className="comandos-toast-message">
                        {toastItem.title && (
                            <strong className="comandos-toast-title">
                                {toastItem.title}
                            </strong>
                        )}
                        <div className="comandos-toast-description">
                            {toastItem.description}
                        </div>
                    </div>

                    <button
                        type="button"
                        className="comandos-toast-close"
                        aria-label="Close notification"
                        onClick={() => dismiss(toastItem.id)}
                    >
                        <X size={17} aria-hidden="true" />
                    </button>
                </div>
            ))}
        </div>
    )
}
