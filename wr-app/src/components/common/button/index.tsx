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
                className={`button ${columnClasses} ${disabled ? 'is-loading' : ''}`} 
                disabled={disabled} 
                {...props}
            >
                {label}
                
            </button>
        )
}

