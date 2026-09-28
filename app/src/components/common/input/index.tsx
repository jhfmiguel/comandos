import { InputHTMLAttributes } from "react"
import { FormatUtils } from "@4us-dev/utils"
import { formatDate } from "utils/date"
import { formatReal } from "utils/money"
import { formatOnlyNumbers } from "utils/numeric"
import { useComandosPreferences } from "components/settings/preferences-provider"

const formatUtils = new FormatUtils()

interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "size"> {
    size?: "small" | "large"
    label: string
    columnClasses?: string
    id: string
    error?: string
    formatter?: (value: string) => string
    currency?: boolean
    onlyNumbers?: boolean
}

export const Input: React.FC<InputProps> = ({
    label,
    columnClasses,
    id,
    error,
    formatter,
    onChange,
    currency = false,
    onlyNumbers = false,
    size,
    required,
    ...inputProps
}) => {
    const { locale } = useComandosPreferences()
    const errorId = error ? `${id}-error` : undefined

    const onInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        let value = event.target.value
        const name = event.target.name

        if (onlyNumbers) value = value.replace(/\D/g, "")
        if (currency) value = formatReal(value, locale)
        const formattedValue = (formatter && formatter(value)) || value

        if (onChange) {
            onChange({
                ...event,
                target: {
                    ...event.target,
                    name,
                    value: formattedValue
                }
            })
        }
    }

    const onInputKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (!onlyNumbers && !currency) return

        const allowedKeys = [
            "Backspace", "Delete", "Tab", "Escape", "Enter",
            "ArrowLeft", "ArrowRight", "Home", "End"
        ]

        if (!/[0-9]/.test(event.key) && !allowedKeys.includes(event.key)) {
            event.preventDefault()
        }
    }

    const control = (
        <input
            {...inputProps}
            className={`comandos-input ${size ? `comandos-input-${size} ` : ""}${currency ? "comandos-currency-value " : ""}w-full`}
            id={id}
            required={required}
            aria-required={required || undefined}
            aria-invalid={error ? true : undefined}
            aria-describedby={errorId ?? inputProps["aria-describedby"]}
            value={inputProps.value ?? ""}
            inputMode={currency ? "numeric" : inputProps.inputMode}
            onChange={onInputChange}
            onKeyDown={onInputKeyDown}
        />
    )

    return (
        <div className={`field ${columnClasses ?? ""}`} data-invalid={error ? "true" : "false"}>
            <label className="block font-semibold mb-2" htmlFor={id}>
                {label}{required ? " *" : ""}
            </label>

            {currency ? (
                <div className="comandos-currency-input w-full">
                    <span className="comandos-input-addon" aria-hidden="true">R$</span>
                    {control}
                </div>
            ) : (
                <div className="w-full">{control}</div>
            )}

            {error && <p id={errorId} className="text-red-500 text-sm mt-1" role="alert">{error}</p>}
        </div>
    )
}

export const InputMoney: React.FC<Omit<InputProps, "currency">> = (props) => {
    const { value, onChange, name, id, label, columnClasses, error, size, required, ...restProps } = props
    const safeValue = typeof value === "object" ? "" : (value ?? "")
    const { locale } = useComandosPreferences()
    const errorId = error ? `${id}-error` : undefined

    const onMoneyChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
        const input = event.currentTarget
        const digits = event.target.value.replace(/\D/g, "")
        const formattedValue = digits ? formatReal(digits, locale) : ""

        if (onChange) {
            event.target.value = formattedValue
            onChange(event)
        }

        requestAnimationFrame(() => {
            const cursorPosition = input.value.length
            input.setSelectionRange(cursorPosition, cursorPosition)
        })
    }

    return (
        <div className={`field ${columnClasses ?? ""}`} data-invalid={error ? "true" : "false"}>
            <label className="block font-semibold mb-2" htmlFor={id}>
                {label}{required ? " *" : ""}
            </label>

            <div className="comandos-currency-input w-full">
                <span className="comandos-input-addon" aria-hidden="true">R$</span>
                <input
                    {...restProps}
                    className={`comandos-input ${size ? `comandos-input-${size} ` : ""}comandos-currency-value w-full`}
                    id={id}
                    name={name}
                    type="text"
                    required={required}
                    aria-required={required || undefined}
                    aria-invalid={error ? true : undefined}
                    aria-describedby={errorId ?? restProps["aria-describedby"]}
                    value={safeValue}
                    inputMode="numeric"
                    onChange={onMoneyChange}
                />
            </div>

            {error && <p id={errorId} className="text-red-500 text-sm mt-1" role="alert">{error}</p>}
        </div>
    )
}

export const InputCPF: React.FC<InputProps> = (props) => (
    <Input {...props} formatter={formatUtils.formatCPF} onlyNumbers />
)

export const InputOnlyNumbers: React.FC<InputProps> = (props) => (
    <Input {...props} formatter={formatOnlyNumbers} onlyNumbers inputMode="numeric" pattern="[0-9]*" />
)

export const InputPhone: React.FC<InputProps> = (props) => (
    <Input {...props} formatter={formatUtils.formatPhone} onlyNumbers inputMode="numeric" />
)

export const InputCEP: React.FC<InputProps> = (props) => (
    <Input {...props} formatter={formatUtils.formatCEP} onlyNumbers inputMode="numeric" />
)

export const InputDate: React.FC<InputProps> = (props) => {
    const { locale } = useComandosPreferences()

    return (
        <Input
            {...props}
            formatter={(value: string) => formatDate(value, locale)}
            onlyNumbers
            inputMode="numeric"
            maxLength={10}
        />
    )
}
