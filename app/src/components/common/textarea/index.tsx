import { TextareaHTMLAttributes } from "react"

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
    label: string
    columnClasses?: string
    id: string
    error?: string
}

export const Textarea: React.FC<TextareaProps> = ({
    label,
    columnClasses,
    id,
    error,
    ...textareaProps
}) => (
    <div className={`field ${columnClasses ?? ""}`}>
        <label className="block font-semibold mb-2" htmlFor={id}>
            {label}
        </label>

        <div className="w-full">
            <textarea
                id={id}
                className="comandos-input w-full"
                {...textareaProps}
            />

            {error && <p className="text-red-500 text-sm mt-1">{error}</p>}
        </div>
    </div>
)
