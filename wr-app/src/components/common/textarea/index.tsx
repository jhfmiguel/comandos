import { TextareaHTMLAttributes } from 'react'

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
    
    label: string
    columnClasses?: string
    id: string
    error?: string

}

export const Textarea: React.FC<TextareaProps> = ( {

    label,
    columnClasses,
    id,
    error,
    ...textareaProps

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
                     id = { id }
                    className = "textarea" 
                    { ... textareaProps }                                    
                />

                { error &&
                
                    <p className = "help is-danger" >{ error }</p>
                
                }

            </div>

        </div>

    )

}