import { ButtonHTMLAttributes } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>{
    label: string
    onClick?: (e: any) => void
    columnClasses?: string
}

export const Button: React.FC<ButtonProps> = ( {
    label,
    onClick,
    columnClasses,
    ... buttonProps
}: ButtonProps ) => {

    return (

        <div className = "control">
            <button 
                className = {` button ${columnClasses}`}
                onClick={ onClick }
            >{ label }
            </button>
        </div>

    )

}