import { TextareaHTMLAttributes } from 'react'

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
    
    onChange?: ( value: any ) => void
    label: string
    columnClasses?: string
    value: string
    id: string
    error?: string

}

export const Textarea: React.FC<TextareaProps> = ( {

    onChange,
    label,
    columnClasses,
    value,
    id,
    error,
    ... textareaProps

}: TextareaProps ) => {

    return (

        <div className = {`field column ${columnClasses}`}>

            <label 
                className = "label"
                htmlFor = { id }
            >
            { label }
            </label>
            
            <div className = "control">
                <textarea
                    { ... textareaProps }
                    className = "textarea" 
                    id = { id }
                    value={ value }
                    onChange = { 
                        event => {
                            if( onChange ) {
                                onChange( event.target.value )
                            } 
                        }   
                    }
                />

                { error &&
                
                    <p className = "help is-danger" >{ error }</p>
                
                }

            </div>

        </div>

    )

}