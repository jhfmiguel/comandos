import { InputHTMLAttributes } from 'react'

import { formatReal } from 'utils/money'

interface InputProps extends InputHTMLAttributes<HTMLInputElement>{
    onChange?: ( value: any ) => void
    label: string
    columnClasses?: string
    id: string
    currency?: boolean
    error?: string
}

export const Input: React.FC<InputProps> = ( {
    onChange,
    label,
    columnClasses,
    id,
    currency,
    error,
    ... inputProps
}: InputProps ) => {

    const onInputChange = ( event: any ) => {


        // --- FORMATTING THE PRICE FIELD FOR THE BRAZILIAN REAL CURRENCY ---

        let value = event.target.value

        if( value && currency ) {
            value = formatReal( value )
        }

        if( onChange ) {
            onChange( value )
        }

        // --- 
        
    }

    return (

        <div className = {`field column ${columnClasses}`}>

            <label 
                className = "label"
                htmlFor = { id }
            >
            { label }
            </label>
            
            <div className="control">
                <input
                    className="input"
                    id = "inputId"
                    { ...inputProps }
                    value = { inputProps.value ?? '' }
                    onChange = { onInputChange }   
                />

                { error &&
                
                    <p className = "help is-danger" >{ error }</p>
                
                }

            </div>

        </div>

    )

}

