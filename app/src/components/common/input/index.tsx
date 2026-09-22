import { InputHTMLAttributes } from "react"
import { FormatUtils } from "@4us-dev/utils"
import { InputGroup } from "components/ui/inputgroup"
import { InputText } from "components/ui/inputtext"
import { Label } from "components/ui/label"
import { formatDate } from "utils/date"
import { formatReal } from "utils/money"

// IMPORTED: Using the centralized numeric utility function
import { formatOnlyNumbers } from "utils/numeric"
import { useComandosPreferences } from "components/settings/preferences-provider"

const formatUtils = new FormatUtils()

interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "size">{
    size?: "small" | "large"
    label: string
    columnClasses?: string
    id: string
    error?: string
    formatter?: ( value: string ) => string
    currency?: boolean
    onlyNumbers?: boolean 
}

export const Input: React.FC<InputProps> = ( {
    label,
    columnClasses,
    id,
    error,
    formatter,
    onChange,
    currency = false,
    onlyNumbers = false, 
    ... inputProps
}: InputProps ) => {

    const onInputChange = ( event: React.ChangeEvent<HTMLInputElement> ) => {
        let value = event.target.value
        const name = event.target.name

        if (onlyNumbers) {
            value = value.replace(/\D/g, "")
        }

        const formattedValue = ( formatter && formatter( value as string )) || value
        
        if ( onChange ) {
            onChange( {
                ... event,
                target: {
                    ...event.target, 
                    name,
                    value: formattedValue
                }
            } )
        }
    }

    const onInputKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (onlyNumbers) {
            const allowedKeys = [
                "Backspace", "Delete", "Tab", "Escape", "Enter", 
                "ArrowLeft", "ArrowRight", "Home", "End"
            ]
            
            if (!/[0-9]/.test(event.key) && !allowedKeys.includes(event.key)) {
                event.preventDefault()
            }
        }
    }

    return (
        <div className = {`field ${columnClasses ?? ""}`}>
            <Label 
                className = "block font-semibold mb-2"
                htmlFor = { id }
            >
            { label }
            </Label>
            
            { currency ? (
                <InputGroup.Root className="comandos-currency-input w-full">

                    <InputGroup.Addon className="comandos-input-addon">
                        R$
                    </InputGroup.Addon>

                    <InputText
                        className="comandos-input comandos-currency-value w-full"
                        id = { id }
                        value = { inputProps.value ?? "" }
                        onChange = { onInputChange }
                        onKeyDown = { onInputKeyDown }
                        { ... inputProps }
                    />

                </InputGroup.Root>
            ) : (
                <div className="w-full">

                    <InputText
                        className="comandos-input w-full"
                        id = { id }
                        value = { inputProps.value ?? "" }
                        onChange = { onInputChange }
                        onKeyDown = { onInputKeyDown }
                        { ... inputProps }
                    />

                </div>
            ) }

            { error &&
                <p className = "text-red-500 text-sm mt-1" >{ error }</p>
            }
        </div>
    )
}

export const InputMoney: React.FC<Omit<InputProps, "currency">> = (props) => {
    const { value, onChange, name, id, label, columnClasses, error, ...restProps } = props

    const safeValue = typeof value === "object" ? "" : (value ?? "")
    const { locale } = useComandosPreferences()

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
        <div className={`field ${columnClasses ?? ""}`}>
            <Label className="block font-semibold mb-2" htmlFor={id}>
                {label}
            </Label>
            
            <InputGroup.Root className="comandos-currency-input w-full">

                <InputGroup.Addon className="comandos-input-addon">
                    R$
                </InputGroup.Addon>

                <InputText
                    {...restProps}
                    className="comandos-input comandos-currency-value w-full"
                    id={id}
                    name={name}
                    type="text"
                    value={safeValue}
                    inputMode="numeric"
                    onChange={onMoneyChange}
                />

            </InputGroup.Root>

            {error && <p className="text-red-500 text-sm mt-1">{error}</p>}
        </div>
    )
}

export const InputCPF: React.FC< InputProps > = ( props: InputProps ) => {
    return (
        <Input 
            { ...props } 
            formatter = { formatUtils.formatCPF } 
            onlyNumbers={true} 
        />
    )
}

export const InputOnlyNumbers: React.FC< InputProps > = ( props: InputProps ) => {
    return (
        <Input 
            { ...props } 
            formatter = { formatOnlyNumbers } 
            onlyNumbers = { true } 
            inputMode = "numeric" 
            pattern = "[0-9]*" // Kept here because this field contains only unformatted numbers
        />
    )
}

// FIXED: Removed pattern="[0-9]*" to allow form submission with phone mask symbols
export const InputPhone: React.FC< InputProps > = ( props: InputProps ) => {
    return (
        <Input { ...props } 
            formatter = { formatUtils.formatPhone } 
            onlyNumbers={ true }
            inputMode = "numeric" 
        />
    )
}

// FIXED: Removed pattern="[0-9]*" to prevent future validation errors with CEP mask hyphens
export const InputCEP: React.FC< InputProps > = ( props: InputProps ) => {
    return (
        <Input { ...props } 
            formatter = { formatUtils.formatCEP } 
            onlyNumbers={ true }
            inputMode = "numeric" 
        />
    )
}

export const InputDate: React.FC<InputProps> = (props: InputProps) => {
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
