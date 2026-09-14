"use client"

import {
    Check,
    ExclamationTriangle,
    InfoCircle,
    Times
} from "@primeicons/react"
import { Toast } from "@primereact/ui/toast"
import {
    toast,
    Toaster,
    type ToasterRegionInstance,
    type ToastType
} from "@primereact/ui/toaster"

export const COMANDOS_TOAST_GROUP = "severity"

export type ComandosToastSeverity =
    | "success"
    | "info"
    | "warn"
    | "error"
    | "secondary"
    | "contrast"

export interface ComandosToastOptions {
    title?: React.ReactNode
    description: React.ReactNode
    duration?: number
    onDismiss?: (toastItem: ToastType) => void
}

function emitToast(
    severity: ComandosToastSeverity,
    options: ComandosToastOptions
) {
    return toast({
        severity,
        title: options.title,
        description: options.description,
        duration: options.duration,
        group: COMANDOS_TOAST_GROUP,
        onDismiss: options.onDismiss
    })
}

export const notify = {
    success: (options: ComandosToastOptions) =>
        emitToast("success", options),

    info: (options: ComandosToastOptions) =>
        emitToast("info", options),

    warn: (options: ComandosToastOptions) =>
        emitToast("warn", options),

    error: (options: ComandosToastOptions) =>
        emitToast("error", options),

    secondary: (options: ComandosToastOptions) =>
        emitToast("secondary", options),

    contrast: (options: ComandosToastOptions) =>
        emitToast("contrast", options)
}

export function ComandosToaster() {
    return (
        <Toaster.Root
            group={COMANDOS_TOAST_GROUP}
            position="top-right"
            limit={5}
        >
            <Toaster.Portal>
                <Toaster.Region>
                    {({ toaster }: ToasterRegionInstance) =>
                        toaster?.toasts.map(
                            (toastItem: ToastType) => (
                                <Toast.Root
                                    key={toastItem.id}
                                    toast={toastItem}
                                >
                                    <Toast.Content>
                                        <Toast.Icon match="success">
                                            <Check />
                                        </Toast.Icon>

                                        <Toast.Icon match="error">
                                            <Times />
                                        </Toast.Icon>

                                        <Toast.Icon match="warn">
                                            <ExclamationTriangle />
                                        </Toast.Icon>

                                        <Toast.Icon match="info">
                                            <InfoCircle />
                                        </Toast.Icon>

                                        <Toast.Icon match="secondary">
                                            <InfoCircle />
                                        </Toast.Icon>

                                        <Toast.Icon match="contrast">
                                            <InfoCircle />
                                        </Toast.Icon>

                                        <Toast.Message>
                                            <Toast.Title />
                                            <Toast.Description />
                                        </Toast.Message>

                                        <Toast.Close>
                                            <Times />
                                        </Toast.Close>
                                    </Toast.Content>
                                </Toast.Root>
                            )
                        )
                    }
                </Toaster.Region>
            </Toaster.Portal>
        </Toaster.Root>
    )
}
