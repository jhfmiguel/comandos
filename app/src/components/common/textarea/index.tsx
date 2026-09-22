import { TextareaHTMLAttributes } from 'react'
import { Label } from 'components/ui/label'
import { Textarea as PrimeTextarea } from 'components/ui/textarea'

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

        <div className = {`field ${columnClasses ?? ""}`}>

            <Label 
                className = "block font-semibold mb-2"
                htmlFor = { id }
            >
            { label }
            </Label>
            
            <div className = "w-full">

                <PrimeTextarea
                    id = { id }
                    className = "comandos-input w-full" 
                    { ... textareaProps }                                   
                />

                { error &&
                
                    <p className = "text-red-500 text-sm mt-1" >{ error }</p>
                
                }

            </div>

        </div>

    )

}