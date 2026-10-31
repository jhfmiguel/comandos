"use client"

import * as React from "react"
import {
    NotificationsProvider,
    NotificationViewport,
    notify as platformNotify,
    type PlatformNotification
} from "@faria-miguel/platform/notifications"

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

const severityToTone = (severity: ComandosToastSeverity): "success" | "info" | "warning" | "error" => {
    if (severity === "warn") return "warning"
    if (severity === "secondary" || severity === "contrast") return "info"
    return severity
}

const emit = (severity: ComandosToastSeverity, options: ComandosToastOptions): ComandosToastItem => {
    const notification: PlatformNotification = platformNotify[severityToTone(severity)](
        options.description,
        options.title,
        options.duration ?? 5000,
        options.onDismiss ? (notification) => options.onDismiss?.({id:notification.id,severity,title:options.title,description:options.description,duration:options.duration ?? 5000,onDismiss:options.onDismiss}) : undefined
    )
    const item: ComandosToastItem = {
        id: notification.id,
        severity,
        title: options.title,
        description: options.description,
        duration: options.duration ?? 5000,
        onDismiss: options.onDismiss
    }
    return item
}

export const notify = {
    success: (options: ComandosToastOptions) => emit("success", options),
    info: (options: ComandosToastOptions) => emit("info", options),
    warn: (options: ComandosToastOptions) => emit("warn", options),
    error: (options: ComandosToastOptions) => emit("error", options),
    secondary: (options: ComandosToastOptions) => emit("secondary", options),
    contrast: (options: ComandosToastOptions) => emit("contrast", options)
}

export function ComandosToaster() {
    return (
        <NotificationsProvider max={5}>
            <NotificationViewport />
        </NotificationsProvider>
    )
}
