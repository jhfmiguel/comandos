import React from 'react'

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
    label: string
    columnClasses?: string
}


export const Button: React.FC<ButtonProps> = ({ 
    
    label, 
    columnClasses, 
    type = 'button', 
    disabled, 
    ...props 

}) => {
    
        return (

            <button 
                type={type} 
                className={`comandos-action-button ${columnClasses ?? ""} ${disabled ? "opacity-60 cursor-wait" : ""}`} 
                disabled={disabled} 
                {...props}
            >
                {label}
                
            </button>
        )
}

