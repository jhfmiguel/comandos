import { InputHTMLAttributes } from "react"
import { FormatUtils } from '@4us-dev/utils'
import { NumericFormat } from "react-number-format"
import { formatDate } from "utils/date"

// IMPORTED: Using the centralized numeric utility function
import { formatOnlyNumbers } from "utils/numeric"

const formatUtils = new FormatUtils()

interface InputProps extends InputHTMLAttributes<HTMLInputElement>{
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

    const onInputChange = ( event: any ) => {
        let value = event.target.value
        const name = event.target.name

        if (onlyNumbers) {
            value = value.replace(/\D/g, '');
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
                'Backspace', 'Delete', 'Tab', 'Escape', 'Enter', 
                'ArrowLeft', 'ArrowRight', 'Home', 'End'
            ];
            
            if (!/[0-9]/.test(event.key) && !allowedKeys.includes(event.key)) {
                event.preventDefault();
            }
        }
    };

    return (
        <div className = {`field column ${columnClasses}`}>
            <label 
                className = 'label'
                htmlFor = { id }
            >
            { label }
            </label>
            
            <div className={ currency ? "field has-addons" : "control" } >
                { currency && (
                    <div className="control">
                        <span className="button is-static">R$</span>
                    </div>
                ) }
                
                <div className={currency ? "control is-expanded" : ""}>
                    <input
                        className='input'
                        id = { id }
                        value = { inputProps.value ?? '' } 
                        onChange = { onInputChange }
                        onKeyDown = { onInputKeyDown } 
                        { ... inputProps }
                    />
                </div>
            </div>

            { error &&
                <p className = 'help is-danger' >{ error }</p>
            }
        </div>
    )
}

export const InputMoney: React.FC<InputProps> = (props: InputProps) => {
    const { value, onChange, name, id, label, columnClasses, error, currency, ...restProps } = props;

    const safeValue = typeof value === 'object' ? '' : (value ?? '');

    return (
        <div className={`field column ${columnClasses}`}>
            <label className='label' htmlFor={id}>
                {label}
            </label>
            
            <div className="field has-addons">
                <div className="control">
                    <span className="button is-static">R$</span>
                </div>
                
                <div className="control is-expanded">
                    <NumericFormat
                        {...(restProps as any)}
                        className="input"
                        id={id}
                        name={name}
                        type="text"
                        value={safeValue}
                        thousandSeparator="."
                        decimalSeparator=","
                        decimalScale={2}
                        fixedDecimalScale={true}
                        allowNegative={false}
                        
                        onValueChange={(values) => {
                            if (onChange) {
                                onChange({
                                    target: {
                                        name: name || id,
                                        value: values.formattedValue
                                    }
                                } as any);
                            }
                        }}
                    />
                </div>
            </div>

            {error && <p className='help is-danger'>{error}</p>}
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
            pattern = "[0-9]*" // Mantido apenas aqui porque este campo não usa máscara de texto (são apenas números puros)
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

export const InputDate: React.FC< InputProps > = ( props: InputProps ) => {
    return (
        <Input { ...props } 
            formatter = { (value: string) => formatDate(value) ?? '' } 
            onlyNumbers={ true }
            inputMode = "numeric" 
            maxLength={10} 
        />
    )
}


