"use client";

import { Check, ExclamationTriangle, Receipt, Sparkles, Spinner, Times, Wifi } from '@primeicons/react';
import { Message as PrimeMessage } from '@primereact/ui/message';

interface MessageProps {
    type: string;
    field?: string;
    text: string;
    onClose?: () => void;
}

export interface Alert {
    type: string;
    field?: string;
    text: string
}

export const Message: React.FC<MessageProps> = ({
    type,
    field,
    text,
    onClose
}) => {
    const severity = type === 'danger' ? 'error' : type === 'warning' ? 'warn' : type;
    const variants = {
        success: Check,
        info: Sparkles,
        warn: Receipt,
        error: ExclamationTriangle,
        secondary: Spinner,
        contrast: Wifi
    };
    const resolvedSeverity = Object.prototype.hasOwnProperty.call(variants, severity)
        ? severity as keyof typeof variants : 'info';
    const Icon = variants[resolvedSeverity];

    return (
        <PrimeMessage.Root key={`${type}:${field ?? ''}:${text}`} severity={resolvedSeverity} onClose={onClose} className="w-full mb-3">
            <PrimeMessage.Content>
                <PrimeMessage.Icon>
                    <Icon className={resolvedSeverity === 'secondary' ? 'animate-spin' : undefined} />
                </PrimeMessage.Icon>
                <PrimeMessage.Text>
                    {field && `${field}: `}{text}
                </PrimeMessage.Text>
                <PrimeMessage.Close type="button" aria-label="Close message">
                    <Times />
                </PrimeMessage.Close>
            </PrimeMessage.Content>
        </PrimeMessage.Root>
    )
}
