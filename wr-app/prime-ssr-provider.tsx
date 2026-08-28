'use client';
import { PrimeReactProvider, PrimeReactStyleSheet } from '@primereact/core';
import { useServerInsertedHTML } from 'next/navigation';
import * as React from 'react';
import Aura from '@primeuix/themes/aura';

const styledStyleSheet = new PrimeReactStyleSheet();

export default function PrimeSSRProvider({
    children,
}: Readonly<{
    children?: React.ReactNode;
}>) {
    useServerInsertedHTML(() => {
        const styleElements = styledStyleSheet.getAllElements();

        styledStyleSheet.clear();

        return <>{styleElements}</>;
    });

    // Consolidated PrimeReact configuration including your theme custom options and license properties
    const primereactConfig = {
        theme: {
            preset: Aura,
            options: {
                prefix: 'p',
                darkModeSelector: 'system',
                cssLayer: false,
                cssVariables: true,
                scoped: false
            }
        },
        license: 'eyJpZCI6Ijc1NmI4Yjg0LTVlOTItNDhhNi1iYWNmLWZlYmVlZWNjYjc1MCIsInByb2R1Y3QiOiJwcmltZXVpIiwidGllciI6ImNvbW11bml0eSIsInR5cGUiOiJkZXYiLCJpYXQiOjE3ODc4NjcwMTYsImV4cCI6MTgxOTQwMzAxNn0.Gn8E5B5Y-ABketx3yDAvjF8OBqHD2madjg4AI-PdLVaLus1K6jNBz1J7LKRwXYsDkG5f1JX1_8uNV9vEwOFtCA'
    };

    return (
        <PrimeReactProvider {...primereactConfig} stylesheet={styledStyleSheet}>
            {children}
        </PrimeReactProvider>
    );
}
