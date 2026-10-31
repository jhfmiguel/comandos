import { TextareaHTMLAttributes } from "react"
import { FloatLabel, Textarea as SharedTextarea } from "@faria-miguel/ui"

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
    label: string
    columnClasses?: string
    id: string
    error?: string
}

export const Textarea: React.FC<TextareaProps> = ({label,columnClasses,id,error,required,...textareaProps}) => (
    <div className={columnClasses ?? ""}>
        <FloatLabel label={label} htmlFor={id} required={required} error={error}>
            <SharedTextarea id={id} {...textareaProps} required={required} placeholder={textareaProps.placeholder ?? " "} invalid={Boolean(error)} />
        </FloatLabel>
    </div>
)
