interface MessageProps {
    type: string;
    field?: string;
    text: string
}

export interface Alert {
    type: string;
    field?: string;
    text: string
}

export const Message: React.FC<MessageProps> = ({
    type,
    field,
    text
}) => {
    
    return (

        <article className = { `comandos-message comandos-message-${type}` }>
            <div className = "comandos-message-body">
                { field && `${ field }: `}{ text }
            </div>
        </article>

    )
}