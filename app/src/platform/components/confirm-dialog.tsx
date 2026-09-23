"use client"

import * as React from "react"

interface ConfirmDialogProps {
    open: boolean
    title: string
    description: React.ReactNode
    confirmLabel?: string
    cancelLabel?: string
    busy?: boolean
    danger?: boolean
    onConfirm: () => void | Promise<void>
    onCancel: () => void
}

export function ConfirmDialog({
    open,
    title,
    description,
    confirmLabel = "Confirm",
    cancelLabel = "Cancel",
    busy = false,
    danger = false,
    onConfirm,
    onCancel
}: ConfirmDialogProps) {
    React.useEffect(() => {
        if (!open) return

        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape" && !busy) onCancel()
        }

        document.addEventListener("keydown", onKeyDown)
        return () => document.removeEventListener("keydown", onKeyDown)
    }, [open, busy, onCancel])

    if (!open) return null

    return (
        <div className="comandos-dialog-layer">
            <button
                type="button"
                className="comandos-dialog-backdrop"
                aria-label={cancelLabel}
                disabled={busy}
                onClick={() => {
                    if (!busy) onCancel()
                }}
            />

            <div
                role="dialog"
                aria-modal="true"
                aria-labelledby="platform-confirm-title"
                className="comandos-native-dialog"
                style={{ width: "min(28rem, calc(100vw - 2rem))" }}
            >
                <div className="comandos-native-dialog-header">
                    <h2 id="platform-confirm-title">{title}</h2>
                </div>

                <div className="comandos-native-dialog-content">
                    <div>{description}</div>

                    <div className="comandos-dialog-actions">
                        <button
                            type="button"
                            className="comandos-secondary-button"
                            disabled={busy}
                            onClick={onCancel}
                        >
                            {cancelLabel}
                        </button>

                        <button
                            type="button"
                            className={
                                danger
                                    ? "comandos-red-button comandos-dialog-action-button"
                                    : "registration-yellow-button"
                            }
                            disabled={busy}
                            onClick={() => void onConfirm()}
                        >
                            {busy ? "..." : confirmLabel}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}
