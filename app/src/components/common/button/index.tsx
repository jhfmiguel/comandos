import React from "react"

type ComandosButtonSeverity = "primary" | "secondary" | "success" | "info" | "warn" | "danger" | "contrast"
type ComandosButtonVariant = "text" | "outlined" | "link" | "contained"

interface ButtonProps extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "color"> {
    label?: string
    columnClasses?: string
    severity?: ComandosButtonSeverity
    variant?: ComandosButtonVariant
    iconOnly?: boolean
    fluid?: boolean
    loading?: boolean
}

export const Button: React.FC<ButtonProps> = ({
    label,
    children,
    columnClasses,
    className,
    type = "button",
    disabled,
    severity = "primary",
    variant = "contained",
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

    return (
        <button
            type={type}
            className={[
                "comandos-action-button",
                variantClass,
                `comandos-button-${severity}`,
                iconOnly ? "comandos-button-icon-only" : "",
                fluid ? "w-full" : "",
                columnClasses ?? "",
                className ?? ""
            ].filter(Boolean).join(" ")}
            disabled={disabled || loading}
            aria-busy={loading || undefined}
            {...props}
        >
            {loading && <span className="comandos-button-spinner" aria-hidden="true" />}
            {label ?? children}
        </button>
    )
}
