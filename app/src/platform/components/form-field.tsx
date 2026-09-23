import * as React from "react"

interface FormFieldProps {
    label: React.ReactNode
    htmlFor?: string
    error?: React.ReactNode
    hint?: React.ReactNode
    required?: boolean
    className?: string
    children: React.ReactNode
}

export function FormField({
    label,
    htmlFor,
    error,
    hint,
    required = false,
    className = "",
    children
}: FormFieldProps) {
    const errorId = htmlFor ? `${htmlFor}-error` : undefined
    const hintId = htmlFor ? `${htmlFor}-hint` : undefined

    return (
        <div className={`field ${className}`.trim()}>
            <label
                htmlFor={htmlFor}
                className="block font-semibold mb-2"
            >
                {label}
                {required && <span aria-hidden="true"> *</span>}
            </label>

            {children}

            {hint && (
                <p id={hintId} className="text-sm mt-1 opacity-70">
                    {hint}
                </p>
            )}

            {error && (
                <p
                    id={errorId}
                    className="text-red-500 text-sm mt-1"
                    role="alert"
                >
                    {error}
                </p>
            )}
        </div>
    )
}
