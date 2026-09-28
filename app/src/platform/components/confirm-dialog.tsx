"use client"

import * as React from "react"

interface ConfirmDialogProps {
    open: boolean
    title: string
    description: React.ReactNode
    confirmLabel?: string
    cancelLabel?: string
    busyLabel?: string
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
    busyLabel = "Processing…",
    busy = false,
    danger = false,
    onConfirm,
    onCancel
}: ConfirmDialogProps) {
    const titleId = React.useId()
    const descriptionId = React.useId()
    const dialogRef = React.useRef<HTMLDivElement | null>(null)
    const cancelRef = React.useRef<HTMLButtonElement | null>(null)
    const previousFocusRef = React.useRef<HTMLElement | null>(null)

    React.useEffect(() => {
        if (!open) return

        previousFocusRef.current = document.activeElement instanceof HTMLElement
            ? document.activeElement
            : null

        requestAnimationFrame(() => cancelRef.current?.focus())

        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape" && !busy) {
                event.preventDefault()
                onCancel()
                return
            }

            if (event.key !== "Tab" || !dialogRef.current) return

            const focusable = Array.from(
                dialogRef.current.querySelectorAll<HTMLElement>(
                    'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
                )
            )

            if (!focusable.length) return

            const first = focusable[0]
            const last = focusable[focusable.length - 1]

            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault()
                last.focus()
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault()
                first.focus()
            }
        }

        document.addEventListener("keydown", onKeyDown)

        return () => {
            document.removeEventListener("keydown", onKeyDown)
            previousFocusRef.current?.focus()
        }
    }, [open, busy, onCancel])

    if (!open) return null

    return (
        <div className="comandos-dialog-layer" role="presentation">
            <button
                type="button"
                className="comandos-dialog-backdrop"
                aria-label={cancelLabel}
                tabIndex={-1}
                disabled={busy}
                onClick={() => {
                    if (!busy) onCancel()
                }}
            />

            <div
                ref={dialogRef}
                role="dialog"
                aria-modal="true"
                aria-labelledby={titleId}
                aria-describedby={descriptionId}
                aria-busy={busy || undefined}
                className="comandos-native-dialog"
                style={{ width: "min(28rem, calc(100vw - 2rem))" }}
            >
                <div className="comandos-native-dialog-header">
                    <h2 id={titleId}>{title}</h2>
                </div>

                <div className="comandos-native-dialog-content">
                    <div id={descriptionId}>{description}</div>

                    <div className="comandos-dialog-actions">
                        <button
                            ref={cancelRef}
                            type="button"
                            className="registration-yellow-button"
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
                            {busy ? busyLabel : confirmLabel}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}
